FROM gradle:jdk11 AS builder
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --stacktrace

FROM alpine:latest AS apm
ARG APM_VERSION=1.16.0

RUN apk update \
    && apk upgrade \
    && apk add --no-cache \
    ca-certificates \
    curl \
    && update-ca-certificates 2>/dev/null || true

RUN curl https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/${APM_VERSION}/elastic-apm-agent-${APM_VERSION}.jar -o /apm-agent.jar


FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/src/build/libs/group-service-0.0.1-SNAPSHOT.jar /app/
RUN addgroup --system --gid 101 social && adduser --system --disabled-login --ingroup social --no-create-home --home /nonexistent --gecos "social user" --shell /bin/false --uid 101 social \
    && chown social:social /app/group-service-0.0.1-SNAPSHOT.jar

COPY --from=apm --chown=social:social /apm-agent.jar /app/apm-agent.jar

EXPOSE 8010
WORKDIR /app
ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-javaagent:/app/apm-agent.jar", "-Delastic.apm.server_urls=https://apm-elk.elcld.net", "-jar", "/app/group-service-0.0.1-SNAPSHOT.jar" ]
# ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/app/group-service-0.0.1-SNAPSHOT.jar"]
