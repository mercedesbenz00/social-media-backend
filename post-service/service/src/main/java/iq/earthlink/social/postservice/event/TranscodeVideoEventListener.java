package iq.earthlink.social.postservice.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.minio.StatObjectResponse;
import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileTranscoded;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.postservice.post.PostMediaService;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static iq.earthlink.social.common.util.CommonConstants.TRANSCODE_EVENT_QUEUE;

@Component
@Slf4j
public class TranscodeVideoEventListener {

    private final PostRepository postRepository;

    private final FileStorageProvider storageProvider;
    private final PostMediaService mediaService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscodeVideoEventListener.class);

    public TranscodeVideoEventListener(PostRepository postRepository, FileStorageProvider storageProvider, PostMediaService mediaService) {
        this.postRepository = postRepository;
        this.storageProvider = storageProvider;
        this.mediaService = mediaService;
    }

    @RabbitListener(queues = {TRANSCODE_EVENT_QUEUE})
    public void receivePostActivityEvents(@Payload String event) {
        LOGGER.debug("Received Transcode event: {} ", event);
        JsonObject json = (JsonObject) JsonParser.parseString(event);
        updateMediaInformation(json);
    }

    private void updateMediaInformation(JsonObject event) {
        Post post = postRepository.findById(event.get(CommonConstants.POST_ID).getAsLong())
                .orElseThrow(() -> new NotFoundException("error.not.found.post", event.get(CommonConstants.POST_ID).getAsLong()));
        if (Objects.nonNull(post)) {
            FileStorage storage = storageProvider.getStorage(StorageType.MINIO);
            StatObjectResponse statObject = storage.getObjectData(event.get(CommonConstants.STORAGE_LOCATION).getAsString());
            List<MediaFile> files = mediaService.findPostFiles(post.getId());
            files.forEach(file -> {
                if (event.get(CommonConstants.ID).getAsString().equals(file.getPath())) {
                    MediaFileTranscoded transcodedFile = new MediaFileTranscoded();
                    transcodedFile.setOwnerId(post.getId());
                    transcodedFile.setFileType(MediaFileType.POST_MEDIA);
                    transcodedFile.setMimeType(statObject.contentType());
                    transcodedFile.setPath(event.get(CommonConstants.STORAGE_LOCATION).getAsString());
                    transcodedFile.setSize(statObject.size());
                    transcodedFile.setStorageType(StorageType.MINIO);
                    file.setTranscodedFile(transcodedFile);
                }
            });
            mediaService.savePostMediaFiles(files, post.getId());
        }
    }
}
