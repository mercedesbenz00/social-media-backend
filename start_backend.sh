#!/bin/bash
set -e

docker-compose up -d db redis rabbit minio mc broker pact_broker elasticsearch && \
make service=audit-service all && \
make service=person-service all && \
make service=group-service all && \
make service=post-service all && \
make service=post-processor-service all && \
make service=post-event-processor-service all && \
make service=notification-service all && \
make service=feed-aggregator-service all && \
make service=matrix filePath=matrix build deploy && \
make service=transcode-service filePath=transcoding-service name=master. config=config.json build-go deploy && \
make service=transcode-node filePath=transcoding-service name=node. config=config.json build-go deploy
