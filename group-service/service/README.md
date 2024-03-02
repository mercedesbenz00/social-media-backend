# group-service

Social Network service for user groups.

# Requirements

* Java 8 (JDK)
* PostgreSQL 9.X or later
* RabbitMQ

# Configuration for dev/prod mode

Create database (PostgreSQL) `earthlink`.
```
CREATE USER earthlink WITH encrypted password 'password';
CREATE DATABASE earthlink WITH OWNER earthlink;
```
 
Make sure params in `./src/main/resources/application.yml` are correct.
```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/earthlink
    driver-class-name: org.postgresql.Driver
    username: earthlink
    password: example
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    
...
social:
  openid:
    keyId: 8810EE7F53ABDB0065EF6417FDEB7C20060E04D5
    jwksUrl: https://account4.shabakaty.com/core/.well-known/openid-configuration/jwks
    accessTokenHeaderName: access-token
...
  fileservice:
    url: http://10.1.5.110:5555/api
    configurationKey: socialntest2
...

# Start for dev mode
Launch from project folder. For Linux (OSX) 
```
./gradlew
``` 
For Windows
```
gradlew.bat 
``` 

# Start for production mode
The service can be deployed to docker from 
```
registry.earthlink.iq/group-service-project/group-service-repo:latest
```
In order to configure service use environment variables for overriding parameters from application.conf 
E.g.
```
#spring.datasource.url
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/earthlink
```
So use uppercase and substitute dot (.) with underscore (_). 3 instances of the service should be started in cluster.

# Logging with Sentry
In order to collect logs by Sentry specify environment variable
```aidl
SENTRY_DSN=https://public:private@host:port/1
```
