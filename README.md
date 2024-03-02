# Social Media Backend

Welcome to the Social Media Backend repository!

Our backend services for the Social Network Project are stored in the "social-media-backend" repository. We utilize the following technology stack:

- **Main Stack**: Spring Framework 2.3.10 with Java 11
- **Build Tool**: Gradle
- **Database**: PostgreSQL
- **Messaging**: RabbitMQ, Kafka
- **Push Notifications**: Firebase Cloud Messaging
- **Cloud Storage**: MinIO
- **Testing**: JUnit 5, Mockito
- **Documentation**: Swagger

## General Description

The Social Network Project aims to build a new social platform specifically designed for the Iraqi community. Our goal is to provide community members with a space to share interesting content and interact with each other in a convenient and organized manner. Instead of searching through multiple pages and feeds, our platform allows users to categorize and aggregate their content in one central location.

The backend component of the Social Network Project consists of several services that work together to provide the platform's functionality. Each service serves a specific purpose and integrates with others through the Feign client, RabbitMQ, and Kafka Messaging.

## Module List

### Person Service

Provides APIs for user authentication (Email, Google, Facebook, Apple), managing user profiles (add/get/update/delete and search), follower/following information, user reports/bans/blocks, and notification settings. Handles matrix push notification settings for each user. Provides statistics APIs for the admin dashboard.

### Group Services

Provides APIs for creating/updating/deleting user groups, managing group members (add/delete/invite), group notification settings, category handling, and group statistics APIs for the admin dashboard.

### Post Service

Provides APIs for creating/updating/deleting group posts, upvoting/downvoting posts, managing post comments (create/update/delete/upvote/downvote), creating/deleting/updating user stories, and managing post/comment reports. For group admin/moderators, there are APIs to approve/reject pending posts and moderate reported posts/comments. Additionally, there are post statistics APIs for the admin dashboard.

### Notification Service

Provides APIs for fetching user notifications, marking notifications as viewed, and managing users' push notification tokens. Integrates with other services through RabbitMQ to receive notifications. Currently, it receives push notifications and sends them through the Firebase Cloud Messaging provider. Also exposes APIs for the Matrix chat services to handle chat push notifications.

### Matrix Service

An open-source distributed chat service. Provides SDKs for different platforms (Web, Android, iOS).

### Transcoding Services

Responsible for transcoding video files. Generates videos for different resolutions and creates an m3u8 file that contains URLs for all generated files. The m3u8 file can be served by video players, which can automatically decide the resolution to stream based on bandwidth and can switch to another resolution on the fly.

### Social Common Module

In the social common module we have classes that are used by other services. During the refactoring if we find that services have duplicated codes, we extract them to the common module.


## Configuration for dev mode

Each service has its own configuration file (`***-service/service/src/main/resources/application.yml`) in which you need to set the following parameters or override them using environment variables:

- `PORT`: The port on which the service will be launched.

For each service, you need to set the correct `PORT` value to ensure proper communication between the services. Here is a list of ports used by the services:

- `person-service` - 8081
- `group-service` - 8082
- `post-service` - 8083
- `notification-service` - 8084
- `chat-service` - 8085
- `short-video-service` - 8088
- `short-video-user-service` - 8099
- `comment-service` - 8097

For more detailed information about the configuration of each component, including how to set up and configure them, please refer to our [Components Configurations](https://creativeadvtech.atlassian.net/wiki/spaces/SNP/pages/1946583063/Components+Configurations) page on our project's wiki.

## Launch project

### Using local machine

To launch the project on your local machine, follow these steps:
1. Ensure that you have Java 11 installed on your machine.
2. Set the right port for each service in application.yml file or override them using environment variables.
3. Run needed services using `./gradlew :services:$(service):bootRun` command in your terminal. For example, to run person-service, run `./gradlew :services:person-service:bootRun`.

### Using Docker

To launch the project using Docker on Windows, follow these steps:

1. Ensure that you have Docker installed and running on your machine.
2. In Makefile, change this:

```
compile:
   ./gradlew :services:$(service):clean :services:$(service):build --no-watch-fs
```
to this:
```
compile:
   gradlew :services:$(service):clean :services:$(service):build --no-watch-fs
```
4. In redisson.yml for person-service, group-service, post-service, change this:
```
  address: redis://${REDIS_HOST:-127.0.0.1}:${REDIS_PORT:-6379}
```
to this:
```
  address: redis://${REDIS_HOST:-host.docker.internal}:${REDIS_PORT:-6379}
```
5. You can comment services that you don't need in start_backend.sh file. Usually, you don't need to run all services locally, just person-service, group-service, post-service, notification-service.
6. Run the following commands in your Git bush terminal: 
```
  bash start_backend.sh
```
6. Wait for the project to build and launch. You can check the status of the build by running `docker ps` in your terminal.
7. For shutdown, run `bash stop_backend.sh` or `docker-compose down` in your terminal.


## REST API Documentation with Swagger

The API documentation is generated and presented using Swagger, a powerful tool for designing, building, and documenting RESTful APIs. Swagger provides a user-friendly interface that allows developers to explore and understand the API endpoints, their functionality, and the expected request and response formats.

To access the Swagger documentation, follow these steps:

1. Launch the application locally or deploy it with docker.

2. Open a web browser and navigate to the Swagger documentation URL. The URL at `/swagger-resources/`, `/swagger-ui/`, `/v2/api-docs/**` or `/v3/api-docs/**`.

For example for post-service it will look like this: http://localhost:8083/swagger-ui/
