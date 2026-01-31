FROM gradle:7.4.0-jdk17

WORKDIR /src

COPY /src .

RUN gradle installDist

CMD ./src