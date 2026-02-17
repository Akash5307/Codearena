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
import java.util.concurrent.TimeUnit;

@Component
public class DockerSandbox {

    private static final Logger log = LoggerFactory.getLogger(DockerSandbox.class);

    private final DockerClient dockerClient;

    public DockerSandbox(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public record ExecutionResult(int exitCode, String stdout, String stderr, boolean timedOut) {}

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
                    "openjdk:21-slim", new String[]{"javac", "/sandbox/Solution.java"},
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
                    "openjdk:21-slim", new String[]{"kotlinc", "/sandbox/Solution.kt", "-include-runtime", "-d", "/sandbox/solution.jar"},
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
                return new ExecutionResult(-1, "", "Interrupted", true);
            }

            if (!finished) {
                try { dockerClient.stopContainerCmd(containerId).withTimeout(1).exec(); } catch (Exception ignored) {}
                return new ExecutionResult(-1, "", "Time Limit Exceeded", true);
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
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    stdout.write(frame.getPayload(), 0, frame.getPayload().length);
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    stderr.write(frame.getPayload(), 0, frame.getPayload().length);
                                }
                            }
                        })
                        .awaitCompletion(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int exitCode = dockerClient.inspectContainerCmd(containerId)
                    .exec().getState().getExitCodeLong().intValue();

            return new ExecutionResult(
                    exitCode,
                    stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8),
                    false
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
