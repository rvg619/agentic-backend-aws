FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the Maven wrapper and pom.xml first for better caching
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Find the JAR file and copy it to a known location
RUN find target -name "*.jar" -exec cp {} app.jar \;

# Expose the port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]