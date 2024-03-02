# Distributed Media Encoder

A transcoder that distributes load among multiple nodes.


## Usage

- Set a config file with the following info:
    1. Bus Info.
    2. Master Db and storage info.
    3. Nodes storage info.
    4. Callback info
      
  Precisely in this format:
 ```json
{
  "master_info": {
    "temp_folder": "",
    "storage_location": {
      "access_key": "",
      "secret_key": "",
      "bucket": "",
      "end_point": ""
    },
    "db_config": {
      "dialect": "postgres",
      "connection_string": ""
    }
  },
  "node_info": {
    "temp_folder": "",
    "storage_location": {
      "access_key": "",
      "secret_key": "",
      "bucket": "",
      "end_point": ""
    }
  },
  "event_bus": {
    "ip": "",
    "port": "",
    "management_port": "",
    "user_name": "",
    "password": ""
  },
  "callback_info": {
    "callback_url": "http://localhost",
    "security_header": "my secret value"
  }
}
``` 

- Run the outside services to send and receive from the following queues respectively (SERVICE_TO_MASTER, MASTER_TO_SERVICES).
- Send this model of the file to be encoded:

```json
{
  "id": "f1faec8525144edc98100b09fc3d2cd5",
  "transcode_profiles": [
    {
      "id": "6ba550c8a6914f89b8202488171555b0",
      "encoder": "x264",
      "number_of_resulting_files": 4,
      "transcode_command": "ffmpeg",
      "output_file_extension": "mp4"
    }
  ],
  "storage_profile": {
    "url": "http://mirrors.standaloneinstaller.com/video-sample/jellyfish-25-mbps-hd-hevc.mp4",
    "storage_locations": [
      {
        "type": "TYPE_LOCATION_TRANSCODED_FULL",
        "bucket": "cinetrans",
        "path": "",
        "id": "4324233"
      },
      {
        "type": "TYPE_LOCATION_TRANSCODED_CHUNK",
        "bucket": "cinetrans",
        "path": "",
        "id": "4324232"
      },
      {
        "type": "TYPE_LOCATION_SOURCE",
        "bucket": "cinetrans",
        "path": "",
        "id": "4324231"
      }
    ],
    "id": "83dc5c1dcfcf4c33aa56cdad2e82b9983"
  },
  "transcode_status": {
    "id": "9a24fcbbe4604efa88ad748a134664ef"
  }
} 
```
- expect this model:
```json
{
  "id": "f1faec8525144edc98100b09fc3d2cd5",
  "storage_location": {
    "id": "4324233",
    "object_name": "f1faec8525144edc98100b09fc3d2cd51961a1ec-120e-ab98-98cc-8edb53c84341.mp4",
    "end_point": "",
    "bucket": "cinetrans",
    "access_key": "",
    "secret_key": "",
    "type": "TYPE_LOCATION_TRANSCODED_FULL"
  },
  "meta_data": {
    "video_tracks": 1,
    "audio_tracks": 1,
    "video_codec": "h264",
    "audio_codec": "aac",
    "channels": 2,
    "time": 21.288,
    "width": 1280,
    "height": 720,
    "rate": "48000",
    "pixel_log": "yuv420p",
    "data": 0
  }
}
```
## Development running

```bash
# 1. copy config file and adjust if needed
cp ./config/dev.json ./config.json

# 2. start rabbit/postgres/minio
docker-compose -f docker-compose-deb.yml up -d

# 3. install required system libraries
sudo apt install ffmpeg 

# 4. run master
go run ./master/cmd/master.go

# 5. run node
go run ./node/cmd/node.go
```

## Production simulation

```bash
# use CONFIG_LOCATION env variable to provide custom location (/config.json - default)
# check compose file for more details
# run everything at once
docker-compose -f docker-compose-full.yml up -d
```

## Testing
```bash
# 
curl --location --request POST 'http://localhost:8033/transcode/' \
--header 'Content-Type: application/json' \
--data-raw '{"id": "test", "url": "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "callback_url": "[YOUR_CALLBACK_URL]"}'
```

## TODOs

- Refine transcoder interface. [Done]
- Allow the transcoder to produce multiple variations of the same profile. [Done]
- Clean up task to clean MP4s and list files. [Done]
- Insure the right order when stitching chunks. [Done]
- Optimize Configurations:

        1. Inject the config to the nodes.[Won't Do][Should use config server in the future]
        2. Refactor config file. [Done]
        2. Remove any reference to hard coded information. [Done]
- Create common file utilities package to be shared between master and nodes. [Done]
- Handle errors:

        1. Handle transcode errors.
        2. Handle in-app errors.
        
- Create temp folders and do some initialization operations. [Done]
- Change the real file status when the chunks had error or they are done.
- Add resume/retry mechanism.
- Handle running multiple bus message receivers in a more elegant way.
- Do batch processing for the same file in the same node.
- Move the stitching function to the transcoder interface. [Done]
- Test the docker image in full operation. [Done]
- Add MP3 Transcoding Profile.
- Add Adaptive Transcoding Profile.
