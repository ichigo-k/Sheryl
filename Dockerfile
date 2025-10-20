# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build

# Set working directory
WORKDIR /app

# Copy Maven files first for caching
COPY pom.xml .
COPY src ./src

# Install Maven and build the app
RUN apt-get update && apt-get install -y maven \
    && mvn clean package -DskipTests \
    && mvn dependency:go-offline

# Stage 2: Run
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port Sheryl runs on
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
