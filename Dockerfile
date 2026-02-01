FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN chmod +x gradlew

RUN ./gradlew installDist

CMD ["./build/install/CookingBenefits/bin/CookingBenefits"]