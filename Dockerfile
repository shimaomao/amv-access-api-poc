# anapsix/alpine-java is a docker image
# based on alpine with "bash" support
FROM anapsix/alpine-java:8_server-jre

VOLUME /tmp

EXPOSE 9000

ADD web/build/libs/amv-access-api-0.0.1-SNAPSHOT.jar app.jar

RUN bash -c 'touch /app.jar'

ENTRYPOINT [ "java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=production","-jar","/app.jar"]