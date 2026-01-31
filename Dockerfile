FROM gradle:7.4.0-jdk17

WORKDIR /src

COPY . .

RUN gradle installDist

CMD ./src