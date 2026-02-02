FROM gradle:8.7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar -x test

FROM openjdk:17-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/healthy-food-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]