FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

COPY . .

# Даем права на выполнение gradlew
RUN chmod +x gradlew

# Собираем проект
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем собранный JAR
COPY --from=builder /app/build/libs/healthy-food-backend.jar app.jar

# Явно указываем порт
EXPOSE 8080

# Запускаем
ENTRYPOINT ["java", \
    "-Dserver.port=8080", \
    "-Dserver.address=0.0.0.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]