FROM gradle:jdk11 AS builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build

FROM java:8
COPY --from=builder /home/gradle/src/easytext.web/build/libs/group-service-0.0.1-SNAPSHOT.jar /app/
WORKDIR /app
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Dspring.datasource.url=jdbc:postgresql://host.docker.internal:5432/group_service", "-jar","/group-service-0.0.1-SNAPSHOT.jar"]
