FROM gradle:8.7-jdk17

WORKDIR /src

COPY . .

RUN gradle installDist --no-build-cache --no-daemon

CMD ./src