FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN chmod +x gradlew

RUN ./gradlew build -x test

EXPOSE 8080

CMD ["java", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-jar", "build/libs/healthy-food-backend.jar"]
