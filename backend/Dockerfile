# =============================================================================
# Multi-stage build: Gradle + JDK 21 -> JRE 21 runtime
# Build:  docker build -t ffa-app:latest ./backend
# =============================================================================
FROM gradle:8.14-jdk21-alpine AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q
COPY src ./src
RUN gradle bootJar -x test --no-daemon -q

FROM eclipse-temurin:21-jre-alpine AS runtime
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION} \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /workspace/build/libs/app.jar /app/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
