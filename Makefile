service ?=
filePath ?=
ifndef $(filePath)
    filePath=.
endif
compile:
	./gradlew :services:$(service):clean :services:$(service):build --no-watch-fs --no-daemon

build:
	docker build -f $(filePath)/$(name)Dockerfile -t $(service):local --build-arg service=$(service) $(filePath)

build-go:
	docker build -f $(filePath)/$(name)Dockerfile -t $(service):local --build-arg service=$(service) $(filePath) --build-arg CONFIG_LOCATION=$(config)

deploy:
	docker-compose up -d $(service)

clean:
	docker-compose rm -sv $(service)

log:
	docker-compose logs -f $(service)

all: compile build deploy

matrix-check-params:
ifndef volume
echo "volume arg is not defined, using default: /var/social_media/matrix"
volume ?= /var/social_media/matrix
endif
ifndef server_name
server_name ?= "localhost"
endif


matrix-generate: matrix-check-params
	docker run -it \
		-v $(volume):/data \
        -e SYNAPSE_SERVER_NAME=$(server_name) \
        -e SYNAPSE_REPORT_STATS=yes \
        matrixdotorg/synapse:latest generate

matrix-deploy: matrix-generate
	docker-compose up -d matrix

.PHONY: all deploy compile build clean log
