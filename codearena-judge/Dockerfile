# gradle image ships Gradle preinstalled — avoids the flaky wrapper download
# of the distribution from services.gradle.org on every source change.
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle.kts settings.gradle.kts ./
COPY codearena-api/build.gradle.kts codearena-api/
COPY codearena-judge/build.gradle.kts codearena-judge/

# Warm the dependency cache in its own layer (survives src-only changes)
RUN gradle :codearena-judge:dependencies --no-daemon -q > /dev/null || true

COPY codearena-judge/src codearena-judge/src

RUN gradle :codearena-judge:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/codearena-judge/build/libs/*.jar app.jar

# Runs as root: the judge needs write access to the mounted host docker.sock
# to spawn sandbox containers (a non-root user inside the container does not
# share the host's docker group GID, so it gets permission denied).
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
