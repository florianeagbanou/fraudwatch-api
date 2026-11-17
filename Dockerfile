# Stage 1: build the jar with Maven (tests already run in CI, so they are skipped here)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Stage 2: small runtime image with only the JRE and the jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Never run as root inside the container
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/fraudwatch-api-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
