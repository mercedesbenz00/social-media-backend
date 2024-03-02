FROM java:8
VOLUME /tmp
ADD person-service-0.0.1-SNAPSHOT.jar app.jar
RUN bash -c 'touch /app.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Dspring.datasource.url=jdbc:postgresql://host.docker.internal:5432/person_service", "-jar","/app.jar"]
