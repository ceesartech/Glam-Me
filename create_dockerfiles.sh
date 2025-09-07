#!/bin/bash

SERVICES=("matching-service" "booking-service" "ride-service" "shopping-service" "social-service" "communication-service" "reviews-service")

for service in "${SERVICES[@]}"; do
    cat > "Dockerfile.${service}" << DOCKERFILE_EOF
# Multi-stage build for ${service}
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY common/ common/

# Copy ${service} specific files
COPY ${service}/ ${service}/

# Grant execute permission to gradlew
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew :${service}:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/${service}/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
DOCKERFILE_EOF

    echo "Created Dockerfile.${service}"
done

echo "All Dockerfiles created successfully!"
