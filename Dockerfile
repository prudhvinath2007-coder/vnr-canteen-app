# Multi-stage Dockerfile for VNR VJIET Canteen Web Application
# Stage 1: Build JAR
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Setup Maven mirror in global config to prevent Render 429 rate limiting on Maven Central
RUN mkdir -p /root/.m2
COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .
COPY src ./src

# Build fat jar using custom mirror and wagon retry options
RUN mvn clean package -s /root/.m2/settings.xml -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.retryHandler.requestSentRetryEnabled=true

# Stage 2: Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S canteen && adduser -S canteen -G canteen
USER canteen

COPY --from=build /app/target/canteen-management-system-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]
