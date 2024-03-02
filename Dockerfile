FROM openjdk:17-jdk-slim

ARG service
ARG jvm_args=" -Xms256m -Xmx512m "

WORKDIR /app

RUN echo ${service}

ADD $service/service/build/libs/*.jar /app/app.jar

CMD ["java", "-Xmx512M", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
