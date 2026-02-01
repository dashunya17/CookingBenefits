FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN chmod +x gradlew

RUN ./gradlew bootJar


CMD ["java", "-jar", "build/libs/CookingBenefits-*.jar"]