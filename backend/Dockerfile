# Use Maven image that already has Maven installed
FROM maven:3.9.6-amazoncorretto-17 AS builder

WORKDIR /build

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage with just JRE
FROM amazoncorretto:17

WORKDIR /app

# Copy JAR from build stage
COPY --from=builder /build/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]