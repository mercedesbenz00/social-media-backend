package iq.earthlink.social.personservice.config;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.personservice.person.PersonMediaService;
import org.dozer.DozerConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

@Component
public class MediaFilePathConverter extends DozerConverter<MediaFile, JsonMediaFile> {


    private final PersonMediaService personMediaService;

    public MediaFilePathConverter(PersonMediaService personMediaService) {
        super(MediaFile.class, JsonMediaFile.class);
        this.personMediaService = personMediaService;
    }


    @Override
    public JsonMediaFile convertTo(MediaFile source, JsonMediaFile destination) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        Set<SizedImage> sizedImageSet = source.getSizedImages();

        Map<String, List<JsonSizedImage>> sizedImages = sizedImageSet.stream()
                .map(sizedImage -> JsonSizedImage
                        .builder()
                        .imageSizeType(sizedImage.getImageSizeType())
                        .size(sizedImage.getSize())
                        .path(personMediaService.getFileURL(StorageType.MINIO, sizedImage.getPath()))
                        .createdAt(sizedImage.getCreatedAt())
                        .mimeType(sizedImage.getMimeType())
                        .build()
                ).collect(groupingBy(image -> image.getMimeType().replace("image/", "")));

        return JsonMediaFile.builder()
                .id(source.getId())
                .fileType(source.getFileType())
                .mimeType(source.getMimeType())
                .ownerId(source.getOwnerId())
                .size(source.getSize())
                .sizedImages(sizedImages)
                .path(personMediaService.getFileURL(source))
                .createdAt(source.getCreatedAt())
                .build();
    }

    @Override
    public MediaFile convertFrom(JsonMediaFile source, MediaFile destination) {
        return null;
    }
}
