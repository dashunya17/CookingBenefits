FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN chmod +x gradlew


RUN ./gradlew installDist -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxPermSize=512m"

CMD ["./build/install/CookingBenefits/bin/CookingBenefits"]