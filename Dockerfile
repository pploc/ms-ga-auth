FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
# Carries the wrapper plus the build scripts applied from build.gradle.
COPY gradle gradle
COPY build.gradle settings.gradle ./
# Published contracts: both the OpenAPI spec and the Avro schemas are code-generated from here.
COPY api api
COPY src src

RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Runs unprivileged. The Kubernetes manifests set runAsNonRoot, which refuses to start a container
# whose image has no non-root user, and readOnlyRootFilesystem, which is why /tmp is mounted in.
RUN addgroup -S app && adduser -S -G app -u 10001 app
COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar
USER 10001

EXPOSE 8082
# MaxRAMPercentage, not -Xmx: the JVM then sizes the heap from the container's memory limit rather
# than the host's, so changing the limit in the manifest is enough.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
