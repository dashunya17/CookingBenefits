FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

COPY . .

# Собираем проект
RUN ./gradlew clean bootJar -x test

# Финальный образ
FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем собранный JAR
COPY --from=builder /app/build/libs/healthy-food-backend.jar app.jar

# Создаем не-root пользователя для безопасности
RUN useradd -m -u 1000 appuser
USER appuser

# Явно указываем порт
EXPOSE 8080

# Запускаем
ENTRYPOINT ["java", \
    "-Dserver.port=8080", \
    "-Dserver.address=0.0.0.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]