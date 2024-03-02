# commentEntity-service

Social Network service for comments.

# Requirements

* Java 8 (JDK)
* PostgreSQL 9.X or later

# Configuration for dev mode

Create database (PostgreSQL) `earthlink`.
```
CREATE USER earthlink WITH encrypted password 'password';
CREATE DATABASE earthlink WITH OWNER earthlink;
```

Make sure params in `./src/main/resources/application.yml` are correct.
```
  datasource:
    url: jdbc:postgresql://localhost:5432/earthlink
    driver-class-name: org.postgresql.Driver
    username: earthlink
    password: password
```

# Start for dev mode
Launch from project folder. For Linux (OSX)
```
./gradlew
``` 
For Windows
```
gradlew.bat 
``` 

# Logging with Sentry
In order to collect logs by Sentry specify environment variable
```
SENTRY_DSN=https://public:private@host:port/1
```
