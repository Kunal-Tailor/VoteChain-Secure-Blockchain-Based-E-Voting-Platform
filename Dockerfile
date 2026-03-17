FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies first (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy all source files
COPY src ./src

# Build fat JAR (no tests, no JavaFX needed for API server)
RUN mvn clean package -DskipTests -B

# ── Runtime image ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR
COPY --from=build /app/target/MPJ-1.0.jar app.jar

# Spark Java default port
EXPOSE 4567

# Run ApiOnlyMain (no JavaFX — just the REST API)
CMD ["java", "-jar", "app.jar"]
