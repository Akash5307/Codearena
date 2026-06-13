# Sandbox image for judging Kotlin submissions.
# The stock JDK images have no Kotlin compiler, so we add kotlinc on top of
# Temurin 21. Build this on the JUDGE HOST (the host Docker daemon spawns the
# sandbox containers) and tag it codearena-kotlin:21:
#
#   docker build -t codearena-kotlin:21 codearena-judge/sandbox-images/kotlin
#
# The judge only ever inspects this image locally (never pulls it), so it does
# not need to live in a registry.
FROM eclipse-temurin:21-jdk-alpine

ARG KOTLIN_VERSION=2.0.21
RUN apk add --no-cache bash curl unzip \
 && curl -fsSL "https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip" -o /tmp/kotlin.zip \
 && unzip -q /tmp/kotlin.zip -d /opt \
 && rm /tmp/kotlin.zip

ENV PATH="/opt/kotlinc/bin:${PATH}"
