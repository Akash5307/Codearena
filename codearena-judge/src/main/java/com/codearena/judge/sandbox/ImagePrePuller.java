package com.codearena.judge.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pre-pulls every sandbox image in the background once the worker is up, so the
 * first submission in a language doesn't stall on a multi-hundred-MB image pull
 * (gcc:13 alone is ~1.4 GB). Runs on a daemon thread — judging proceeds in
 * parallel and simply skips the pull for images that already landed.
 */
@Component
public class ImagePrePuller {

    private static final Logger log = LoggerFactory.getLogger(ImagePrePuller.class);

    private static final List<String> LANGUAGES =
            List.of("CPP", "C", "JAVA", "PYTHON", "JAVASCRIPT", "GO", "RUST");

    private final DockerSandbox sandbox;

    public ImagePrePuller(DockerSandbox sandbox) {
        this.sandbox = sandbox;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void prePullImages() {
        Set<String> images = new LinkedHashSet<>();
        for (String lang : LANGUAGES) {
            images.add(DockerSandbox.getLanguageSpec(lang).image());
        }

        Thread puller = new Thread(() -> {
            log.info("Pre-pulling {} sandbox images: {}", images.size(), images);
            for (String image : images) {
                try {
                    sandbox.pullImageIfNeeded(image);
                } catch (Exception e) {
                    log.warn("Pre-pull failed for {} (will retry on first use): {}", image, e.getMessage());
                }
            }
            log.info("Sandbox image pre-pull complete");
        }, "sandbox-image-prepull");
        puller.setDaemon(true);
        puller.start();
    }
}
