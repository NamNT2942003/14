# Stage 1: Build & Compile Java Code
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY src/ /app/src/
RUN mkdir -p /app/bin && javac -d /app/bin /app/src/main/java/com/quickbite/user/UserServiceApplication.java

# Stage 2: Lightweight Runtime Environment
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin /app/bin
EXPOSE 8081
CMD ["java", "-cp", "/app/bin", "com.quickbite.user.UserServiceApplication"]
