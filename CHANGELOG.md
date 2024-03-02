# 0.0.4 (2024-01-24)

##Post service
- Added group, group member, person entities
- Added repositories and services for new entities
- Subscribed to the group, group member, and person kafka events
- person service and group service rest API calls replaced by local person and group/group member service calls
- created v2 of Post APIs(Create post, Get post, Find posts, Update post, Find posts with complaints, Reject post by complaints)
- Kafka consumer configuration updated, to receive only committed messages

##Group service
- Added new person entity
- Added repositories and services for new entity
- Subscribed to the person kafka events
- Created scheduler to push groups and group members to kafka topic
- Added person details to the JsonGroupMember response
- Refactored service layer to use local person manager instead of doing rest call to person service
- Added configuration for kafka producer to submit messages in transaction
- Removed person statistics rabbit events, and used kafka events

##Person service
- Subscribed to the group member and follower/following kafka events
- Removed rabbitMQ producer for follower/following count
- Removed rabbitMQ listeners for group count and follower/following count stat events
- Created scheduler to push persons to kafka topic
- Added configuration for kafka producer to submit messages in transaction
- Migrated email notification to the notification service

# 0.0.3 (2024-01-08)

##Group service
- Removed permission assignment for the pending user
- added permission assignment/revoking on group member state update functionality

##Post service
- Group member validation: added group member state validation.

# 0.0.2 (2023-10-05)

##Person service
- Extracted common security features to the new module
- Fixed jwt authentication filter
- Onboarding flow: made birthday and gender as optional

## User feed aggregator
- Post detail dto changes

# 0.0.1 (2023-09-25)
## Groups service
- Optimized "Find my groups" API response time
- Optimized "Find frequently posts groups" API response time
- Sending the notification to the Notification service changed from rabbitMQ to Kafka

## Post service
- Create comment with Media file API updates: response payload will have mediaFile attribute
- Optimized get post with details internal API response time
- Post collection API changes: added the validation to check if user already has post in his collections before adding the post to collection
- Added new linkMeta attribute to the post entity, where will be stored link preview details
- Sending the notification to the Notification service changed from rabbitMQ to Kafka

## Person service
- Sending the notification to the Notification service changed from rabbitMQ to Kafka

## Notification service
- RabbitMQ listener replaced with Kafka event listener

## Audit service
- Added the pagination to "Find audit logs" API

## User feed aggregator
- Created initial version of the given service, integrated with post-service, group-service and feed aggregator service

## Feed aggregator
- Created initial version of the given service which will fetch recent/topK post ids from the redis instance

## Post event processor
- Created initial version of the given service which will receive kafka events for post activities like post likes, comments and views and store it in topK data structure of the redis-stack.

## New post processor
- Created initial version of the given service which will receive events for each created post and store in the redis list by group id key.

