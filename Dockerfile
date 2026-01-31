FROM gradle:7.4.0-jdk17

WORKDIR /src

COPY . .

RUN gradle installDist --no-build-cache --no-daemon

CMD ./src