FROM gradle:8.10.2-jdk21 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS=""
ENV SERVER_PORT=8080

COPY --from=builder /workspace/build/libs/ai-search-gpt-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
