FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle ./gradlew \
    --no-daemon dependencies || true

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test \
 && JAR=$(ls build/libs/*.jar | grep -v plain | head -n 1) \
 && cp "$JAR" /app.jar


FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 spring && chown spring:spring /app
USER spring
COPY --from=build /app.jar /app.jar
ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT} -jar /app.jar"]