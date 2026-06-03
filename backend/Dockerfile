FROM maven:3.9.10-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S platform && adduser -S platform -G platform
WORKDIR /app
COPY --from=build /workspace/target/resume-platform-api-*.jar app.jar
USER platform
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=4s --start-period=35s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

