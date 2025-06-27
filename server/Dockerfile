# Use Maven to build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Package the application (skip tests to speed up)
RUN mvn clean package -DskipTests

# Use a minimal JDK base image for running the app
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy env.properties file (optional, or mount it later via volume)
COPY env.prop ./env.properties

# Expose the port configured in application.properties
EXPOSE 8080

# Set the environment variable for Spring config import
ENV SPRING_CONFIG_IMPORT=env.properties

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
