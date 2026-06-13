package com.codearena.judge.service;

import com.codearena.judge.config.MinioConfig;
import com.codearena.judge.dto.JudgeResult;
import com.codearena.judge.dto.JudgeTask;
import com.codearena.judge.sandbox.DockerSandbox;
import com.codearena.judge.sandbox.DockerSandbox.ExecutionResult;
import com.codearena.judge.sandbox.DockerSandbox.LanguageSpec;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeService.class);

    // Compilation budget — independent of (and larger than) the problem's run limits.
    private static final long COMPILE_TIMEOUT_MS = 60_000;
    private static final long COMPILE_MEMORY_BYTES = 1024L * 1024 * 1024; // 1 GB

    private final DockerSandbox sandbox;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * Directory where per-submission sandbox dirs are created. When the judge itself
     * runs in a container talking to the HOST Docker daemon (docker.sock mount), bind
     * mount sources are resolved on the HOST filesystem — so this directory must be
     * bind-mounted into the judge container at the SAME absolute path as on the host
     * (see docker-compose.full.yml: /tmp/codearena-judge). Empty = system temp dir
     * (fine when the judge runs directly on the host).
     */
    private final String workDir;

    public JudgeService(DockerSandbox sandbox, MinioClient minioClient, MinioConfig minioConfig,
                        @Value("${judge.work-dir:}") String workDir) {
        this.sandbox = sandbox;
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.workDir = workDir;
    }

    private Path createSandboxDir(Long submissionId) throws IOException {
        if (workDir == null || workDir.isBlank()) {
            return Files.createTempDirectory("judge-" + submissionId);
        }
        Path base = Path.of(workDir);
        Files.createDirectories(base);
        return Files.createTempDirectory(base, "judge-" + submissionId);
    }

    public JudgeResult judge(JudgeTask task) {
        LanguageSpec spec = DockerSandbox.getLanguageSpec(task.language());

        // Pull image
        sandbox.pullImageIfNeeded(spec.image());

        Path sandboxDir = null;
        try {
            sandboxDir = createSandboxDir(task.submissionId());
            Path sourceFile = sandboxDir.resolve(spec.sourceFile());
            Files.writeString(sourceFile, task.sourceCode());

            Bind bind = new Bind(sandboxDir.toAbsolutePath().toString(), new Volume("/sandbox"));
            long memoryBytes = (long) task.memoryLimitMb() * 1024 * 1024;

            // Compile step (if needed). Compilers (kotlinc, rustc, g++) need far
            // more memory and time than the solution's own run limits, so give the
            // compile step its own generous budget rather than the problem's limit.
            if (spec.compileCmd() != null) {
                ExecutionResult compileResult = sandbox.execute(
                        spec.image(), spec.compileCmd(), null,
                        COMPILE_TIMEOUT_MS, COMPILE_MEMORY_BYTES, bind);

                if (compileResult.exitCode() != 0) {
                    log.info("Compilation error for submission {}: {}", task.submissionId(), compileResult.stderr());
                    return new JudgeResult(task.submissionId(), "CE", null, null, 0, 0);
                }
            }

            // Download test cases
            List<TestCasePair> testCases = downloadTestCases(task.problemId());
            if (testCases.isEmpty()) {
                log.warn("No test cases found for problem {}", task.problemId());
                return new JudgeResult(task.submissionId(), "AC", 0, 0, 0, 0);
            }

            int passed = 0;
            int maxTimeMs = 0;
            int maxMemoryKb = 0;

            for (int i = 0; i < testCases.size(); i++) {
                TestCasePair tc = testCases.get(i);

                // Feed input via a file + shell redirect instead of attaching stdin:
                // docker-java stdin attach is unreliable (no EOF), which hangs
                // solutions that read until end-of-input.
                Files.writeString(sandboxDir.resolve("input.txt"), tc.input());
                String[] runCmd = {"sh", "-c", String.join(" ", spec.runCmd()) + " < /sandbox/input.txt"};

                // Wall-clock per test (includes ~100-300ms container start overhead,
                // so this is an upper bound on the solution's own runtime).
                long runStart = System.nanoTime();
                ExecutionResult runResult = sandbox.execute(
                        spec.image(), runCmd, null,
                        task.timeLimitMs(), memoryBytes, bind);
                int elapsedMs = (int) ((System.nanoTime() - runStart) / 1_000_000);
                maxTimeMs = Math.max(maxTimeMs, elapsedMs);

                if (runResult.timedOut()) {
                    return new JudgeResult(task.submissionId(), "TLE",
                            task.timeLimitMs(), null, passed, testCases.size());
                }

                // Check OOM before the generic exit-code path: an OOM kill exits
                // nonzero but is a memory-limit breach (MLE), not a runtime error.
                if (runResult.oomKilled()) {
                    return new JudgeResult(task.submissionId(), "MLE",
                            maxTimeMs, task.memoryLimitMb() * 1024, passed, testCases.size());
                }

                if (runResult.exitCode() != 0) {
                    return new JudgeResult(task.submissionId(), "RE",
                            maxTimeMs, null, passed, testCases.size());
                }

                if (!compareOutput(runResult.stdout(), tc.expectedOutput())) {
                    return new JudgeResult(task.submissionId(), "WA",
                            maxTimeMs, null, passed, testCases.size());
                }

                passed++;
            }

            return new JudgeResult(task.submissionId(), "AC", maxTimeMs, maxMemoryKb, passed, testCases.size());

        } catch (Exception e) {
            log.error("Judge error for submission {}", task.submissionId(), e);
            return new JudgeResult(task.submissionId(), "RE", null, null, 0, 0);
        } finally {
            if (sandboxDir != null) {
                cleanupDir(sandboxDir);
            }
        }
    }

    private boolean compareOutput(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        return actual.strip().equals(expected.strip());
    }

    private record TestCasePair(String input, String expectedOutput) {}

    private List<TestCasePair> downloadTestCases(Long problemId) {
        String prefix = "testcases/" + problemId + "/";
        List<String> inputKeys = new ArrayList<>();
        List<String> outputKeys = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                String key = result.get().objectName();
                // Heuristic: input files and output files are uploaded in pairs
                // They're stored with UUIDs, so we rely on the order from the test_cases table
                // For now, collect all files and pair them
                inputKeys.add(key);
            }
        } catch (Exception e) {
            log.error("Error listing test cases for problem {}", problemId, e);
            return List.of();
        }

        // Test cases are uploaded as pairs (input file, then output file) via the API
        // Files are named: testcases/{problemId}/{uuid}_{originalName}
        // We sort by name and pair them: even indices = input, odd indices = output
        inputKeys.sort(Comparator.naturalOrder());

        List<TestCasePair> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < inputKeys.size(); i += 2) {
            String inputContent = downloadFileContent(inputKeys.get(i));
            String outputContent = downloadFileContent(inputKeys.get(i + 1));
            if (inputContent != null && outputContent != null) {
                pairs.add(new TestCasePair(inputContent, outputContent));
            }
        }

        return pairs;
    }

    private String downloadFileContent(String objectName) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build())) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error downloading {}", objectName, e);
            return null;
        }
    }

    private void cleanupDir(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
