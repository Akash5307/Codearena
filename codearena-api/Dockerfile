FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY ../gradle gradle
COPY ../gradlew .
COPY ../build.gradle.kts .
COPY ../settings.gradle.kts .
COPY codearena-api/build.gradle.kts codearena-api/
COPY codearena-judge/build.gradle.kts codearena-judge/

RUN ./gradlew :codearena-api:dependencies --no-daemon || true

COPY codearena-api/src codearena-api/src

RUN ./gradlew :codearena-api:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/codearena-api/build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
