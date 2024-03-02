package iq.earthlink.social.postservice.event;

import com.google.gson.JsonObject;
import io.minio.StatObjectResponse;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioFileStorage;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.postservice.post.PostMediaService;
import iq.earthlink.social.postservice.post.model.Post;
import iq.earthlink.social.postservice.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

class TranscodeVideoEventListenerTest {
    @InjectMocks
    private TranscodeVideoEventListener transcodeVideoEventListener;
    @Mock
    private PostRepository postRepository;
    @Mock
    private FileStorageProvider storageProvider;
    @Mock
    private PostMediaService mediaService;
    @Mock
    private MinioFileStorage minioFileStorage;
    @Mock
    private StatObjectResponse statObjectResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void receivePostActivityEvents__shouldUpdateMediaInformation() {
        //given
        JsonObject event = new JsonObject();
        event.addProperty(CommonConstants.POST_ID, 1L);
        String path = "example.jpg";
        event.addProperty(CommonConstants.STORAGE_LOCATION, path);
        event.addProperty(CommonConstants.ID, path);

        Post post = new Post();
        post.setId(1L);

        List<MediaFile> files = new ArrayList<>();
        MediaFile file = new MediaFile();
        file.setId(1L);
        file.setPath(path);
        files.add(file);

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(mediaService.findPostFiles(1L)).willReturn(files);
        given(storageProvider.getStorage(StorageType.MINIO)).willReturn(minioFileStorage);
        given(minioFileStorage.getObjectData(any())).willReturn(statObjectResponse);

        //when
        transcodeVideoEventListener.receivePostActivityEvents(event.toString());

        //then
        verify(postRepository).findById(1L);
        verify(mediaService).findPostFiles(1L);
        verify(storageProvider).getStorage(StorageType.MINIO);
        verify(mediaService).savePostMediaFiles(files, 1L);
        assertThat(file.getTranscodedFile()).isNotNull();
        assertThat(file.getTranscodedFile().getPath()).isEqualTo(event.get(CommonConstants.STORAGE_LOCATION).getAsString());
    }
}