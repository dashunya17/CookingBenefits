FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN chmod +x gradlew

RUN ./gradlew build -x test

EXPOSE 8080

# Найдем любой JAR файл
CMD sh -c 'java -jar $(find . -name "*.jar" -type f | head -1)'