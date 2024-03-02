FROM gradle:jdk11 AS builder
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --stacktrace

FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/src/build/libs/person-service-0.0.1-SNAPSHOT.jar /app/
RUN addgroup --system --gid 101 social && adduser --system --disabled-login --ingroup social --no-create-home --home /nonexistent --gecos "social user" --shell /bin/false --uid 101 social \
    && chown social:social /app/person-service-0.0.1-SNAPSHOT.jar

EXPOSE 8030
WORKDIR /app
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar","/app/person-service-0.0.1-SNAPSHOT.jar"]
