# gradle image ships Gradle preinstalled — avoids the flaky wrapper download
# of the distribution from services.gradle.org on every source change.
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace

COPY build.gradle.kts settings.gradle.kts ./
COPY codearena-api/build.gradle.kts codearena-api/
COPY codearena-judge/build.gradle.kts codearena-judge/

# Warm the dependency cache in its own layer (survives src-only changes)
RUN gradle :codearena-api:dependencies --no-daemon -q > /dev/null || true

COPY codearena-api/src codearena-api/src

RUN gradle :codearena-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/codearena-api/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
