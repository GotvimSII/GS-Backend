FROM azul/zulu-openjdk-alpine:21-jre-headless-latest
RUN /bin/sh -c 'apk add --no-cache argon2-dev'
WORKDIR /app
COPY build/libs/auth-ms-all.jar authms.jar
ENTRYPOINT [ "java", "-jar", "/app/authms.jar", "-P:migrate=migrate" ]