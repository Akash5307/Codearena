package com.codearena.judge.sandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DockerSandbox {

    private static final Logger log = LoggerFactory.getLogger(DockerSandbox.class);

    /**
     * Hard cap on captured stdout/stderr per stream. A solution that floods stdout
     * would otherwise grow the in-memory buffer unbounded and OOM the judge itself.
     * 1 MB is far beyond any legitimate answer; excess output is dropped (a flooding
     * solution fails the comparison anyway).
     */
    private static final int MAX_OUTPUT_BYTES = 1_048_576;

    /**
     * Max concurrent processes/threads inside a sandbox container (fork-bomb guard).
     * Generous enough for JVM/Go runtimes (GC + JIT threads scale with cores), far
     * too small for a fork bomb to harm the host.
     */
    private static final long PIDS_LIMIT = 128L;

    private final DockerClient dockerClient;

    public DockerSandbox(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public record ExecutionResult(int exitCode, String stdout, String stderr,
                                  boolean timedOut, boolean oomKilled) {}

    public record LanguageSpec(String image, String[] compileCmd, String[] runCmd, String sourceFile) {}

    public static LanguageSpec getLanguageSpec(String language) {
        return switch (language.toUpperCase()) {
            case "CPP" -> new LanguageSpec(
                    "gcc:13", new String[]{"g++", "-O2", "-o", "/sandbox/solution", "/sandbox/solution.cpp"},
                    new String[]{"/sandbox/solution"}, "solution.cpp");
            case "C" -> new LanguageSpec(
                    "gcc:13", new String[]{"gcc", "-O2", "-o", "/sandbox/solution", "/sandbox/solution.c"},
                    new String[]{"/sandbox/solution"}, "solution.c");
            case "JAVA" -> new LanguageSpec(
                    // openjdk:* images are deprecated/unpublished for 21; temurin JDK has javac + java
                    "eclipse-temurin:21-jdk-alpine", new String[]{"javac", "/sandbox/Solution.java"},
                    new String[]{"java", "-cp", "/sandbox", "Solution"}, "Solution.java");
            case "PYTHON" -> new LanguageSpec(
                    "python:3.12-slim", null,
                    new String[]{"python3", "/sandbox/solution.py"}, "solution.py");
            case "JAVASCRIPT" -> new LanguageSpec(
                    "node:20-slim", null,
                    new String[]{"node", "/sandbox/solution.js"}, "solution.js");
            case "GO" -> new LanguageSpec(
                    "golang:1.22-alpine", new String[]{"go", "build", "-o", "/sandbox/solution", "/sandbox/solution.go"},
                    new String[]{"/sandbox/solution"}, "solution.go");
            case "RUST" -> new LanguageSpec(
                    "rust:1.77-slim", new String[]{"rustc", "-O", "-o", "/sandbox/solution", "/sandbox/solution.rs"},
                    new String[]{"/sandbox/solution"}, "solution.rs");
            case "KOTLIN" -> new LanguageSpec(
                    // Custom image (Temurin 21 + kotlinc); build with
                    // sandbox-images/kotlin/Dockerfile and tag codearena-kotlin:21.
                    "codearena-kotlin:21", new String[]{"kotlinc", "/sandbox/Solution.kt", "-include-runtime", "-d", "/sandbox/solution.jar"},
                    new String[]{"java", "-jar", "/sandbox/solution.jar"}, "Solution.kt");
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    public void pullImageIfNeeded(String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (Exception e) {
            log.info("Pulling image: {}", image);
            try {
                dockerClient.pullImageCmd(image)
                        .start()
                        .awaitCompletion(120, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while pulling image: " + image, ie);
            }
        }
    }

    public ExecutionResult execute(String image, String[] cmd, String stdinData,
                                    long timeLimitMs, long memoryLimitBytes,
                                    Bind... binds) {
        String containerId = null;
        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(memoryLimitBytes)
                    .withMemorySwap(memoryLimitBytes)
                    .withCpuCount(1L)
                    .withNetworkMode("none")
                    // Hardening: PID cap (fork bombs), drop every capability,
                    // and block setuid/setgid privilege escalation.
                    .withPidsLimit(PIDS_LIMIT)
                    .withCapDrop(Capability.ALL)
                    .withSecurityOpts(List.of("no-new-privileges"))
                    .withBinds(binds);

            CreateContainerResponse container = dockerClient.createContainerCmd(image)
                    .withCmd(cmd)
                    .withHostConfig(hostConfig)
                    .withStdinOpen(stdinData != null)
                    .withWorkingDir("/sandbox")
                    .exec();

            containerId = container.getId();

            if (stdinData != null) {
                dockerClient.attachContainerCmd(containerId)
                        .withStdIn(new java.io.ByteArrayInputStream(
                                stdinData.getBytes(StandardCharsets.UTF_8)))
                        .withFollowStream(false)
                        .withStdIn(new java.io.ByteArrayInputStream(
                                stdinData.getBytes(StandardCharsets.UTF_8)))
                        .exec(new ResultCallback.Adapter<>());
            }

            dockerClient.startContainerCmd(containerId).exec();

            boolean finished;
            try {
                finished = dockerClient.waitContainerCmd(containerId)
                        .exec(new WaitContainerResultCallback())
                        .awaitCompletion(timeLimitMs + 2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ExecutionResult(-1, "", "Interrupted", true, false);
            }

            if (!finished) {
                try { dockerClient.stopContainerCmd(containerId).withTimeout(1).exec(); } catch (Exception ignored) {}
                return new ExecutionResult(-1, "", "Time Limit Exceeded", true, false);
            }

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            try {
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true)
                        .withStdErr(true)
                        .withFollowStream(true)
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                byte[] payload = frame.getPayload();
                                ByteArrayOutputStream target =
                                        frame.getStreamType() == StreamType.STDOUT ? stdout
                                                : frame.getStreamType() == StreamType.STDERR ? stderr
                                                : null;
                                if (target == null) return;
                                int remaining = MAX_OUTPUT_BYTES - target.size();
                                if (remaining > 0) {
                                    target.write(payload, 0, Math.min(payload.length, remaining));
                                }
                            }
                        })
                        .awaitCompletion(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var state = dockerClient.inspectContainerCmd(containerId).exec().getState();
            int exitCode = state.getExitCodeLong().intValue();
            // OOMKilled is set by the kernel cgroup when the process exceeds the
            // memory limit. Exit 137 alone is ambiguous (also a plain SIGKILL), so
            // trust the daemon's flag to distinguish MLE from a generic RE.
            boolean oomKilled = Boolean.TRUE.equals(state.getOOMKilled());

            return new ExecutionResult(
                    exitCode,
                    stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8),
                    false,
                    oomKilled
            );

        } finally {
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {}
            }
        }
    }
}
