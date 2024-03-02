# This docker file builds a docker image for nodes
# With config must be given as parameter
# Sample usage
# docker build -f node.Dockerfile . -t node --build-arg CONFIG_LOCATION=node/cmd/config.json
# docker run  -p 5672:5672 -p 15672:15672  node
############################
# STEP 1 build executable binary
############################
FROM golang:alpine AS builder
# Install git.
# Git is required for fetching the dependencies.
RUN apk update && apk add --no-cache git
ADD . $GOPATH/src/transcode-service
WORKDIR $GOPATH/src/transcode-service
COPY . .
# Fetch dependencies.
# Using go get.
RUN go get ./...
# Build the binary.
RUN go install $GOPATH/src/transcode-service/node/cmd
############################
# STEP 2 build a small image
############################
FROM alpine

RUN apk add  --no-cache ffmpeg
# Copy our static executable.
COPY --from=builder /go/bin/cmd /go/bin/cmd
# Copy node config
ARG CONFIG_LOCATION
COPY $CONFIG_LOCATION /config.json
EXPOSE 8034
ENTRYPOINT /go/bin/cmd

