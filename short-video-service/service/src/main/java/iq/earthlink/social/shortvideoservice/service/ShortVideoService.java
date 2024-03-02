package iq.earthlink.social.shortvideoservice.service;

import feign.FeignException;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.commentservice.dto.JsonComment;
import iq.earthlink.social.commentservice.dto.JsonCommentData;
import iq.earthlink.social.commentservice.rest.CommentRestService;
import iq.earthlink.social.common.filestorage.FileStorage;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.rest.RestPageImpl;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.person.rest.JsonFollowing;
import iq.earthlink.social.personservice.rest.FollowingRestService;
import iq.earthlink.social.security.config.ServerAuthProperties;
import iq.earthlink.social.shortvideoregistryservice.dto.*;
import iq.earthlink.social.shortvideoregistryservice.rest.ShortVideoRegistryRestService;
import iq.earthlink.social.shortvideoservice.api.ShortVideoApiDelegate;
import iq.earthlink.social.shortvideoservice.model.*;
import iq.earthlink.social.shortvideoservice.utils.SecurityContextUtils;
import iq.earthlink.social.util.ExceptionUtil;
import org.apache.commons.lang3.StringUtils;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class ShortVideoService implements ShortVideoApiDelegate {

    public static final String THUMBNAIL_SUFFIX = "_thumbnail";

    private final KafkaProducerService kafkaProducerService;
    private final FileStorageProvider fileStorageProvider;
    private final ShortVideoRegistryRestService shortVideoRegistryRestService;
    private final FollowingRestService followingRestService;
    private final CommentRestService commentRestService;
    private final SecurityContextUtils securityContextUtils;
    private final Mapper mapper;
    private final ServerAuthProperties authProperties;

    public ShortVideoService(ServerAuthProperties authProperties, KafkaProducerService kafkaProducerService,
                             FileStorageProvider fileStorageProvider,
                             ShortVideoRegistryRestService shortVideoRegistryRestService,
                             FollowingRestService followingRestService, CommentRestService commentRestService,
                             SecurityContextUtils securityContextUtils,
                             Mapper mapper) {
        this.authProperties = authProperties;
        this.kafkaProducerService = kafkaProducerService;
        this.fileStorageProvider = fileStorageProvider;
        this.shortVideoRegistryRestService = shortVideoRegistryRestService;
        this.followingRestService = followingRestService;
        this.commentRestService = commentRestService;
        this.securityContextUtils = securityContextUtils;
        this.mapper = mapper;
    }

    @Override
    public CreateShortVideoResponseDTO addShortVideoData(CreateShortVideoDTO createShortVideoDTO) {
        String authorizationToken = securityContextUtils.getAuthorizationToken();

        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        PersonInfo personInfo = securityContextUtils.getCurrentPersonInfo();
        CreateShortVideoMessageDTO shortVideoDTO = mapper.map(createShortVideoDTO, CreateShortVideoMessageDTO.class);

        UUID uuid = UUID.randomUUID();
        String fileName = uuid.toString();
        shortVideoDTO.setId(uuid);
        shortVideoDTO.setBucket(storage.getBucketName());
        shortVideoDTO.setAuthorId(personInfo.getId());
        shortVideoDTO.setFriends(getFriends(authorizationToken, personInfo));

        String presignedURL = storage.getPresignedUrlForUpload(fileName);

        kafkaProducerService.sendMessage(CommonConstants.SHORT_VIDEO_TOPIC, shortVideoDTO);

        return CreateShortVideoResponseDTO.builder()
                .videoId(uuid)
                .presignedUrl(presignedURL)
                .build();
    }

    /**
     * API to upload short video from third party
     * @param apiKey - secret key
     * @param shortVideoDTO - video object
     * @return response containing video ID
     */
    @Override
    public UploadShortVideoResponseDTO uploadShortVideo(String apiKey, UploadShortVideoDTO shortVideoDTO) {
        validateRequiredInput(apiKey, shortVideoDTO);

        CreateShortVideoMessageDTO svMessage = mapper.map(shortVideoDTO, CreateShortVideoMessageDTO.class);

        UUID uuid = UUID.randomUUID();
        svMessage.setId(uuid);
        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        svMessage.setBucket(storage.getBucketName());

        kafkaProducerService.sendMessage(CommonConstants.SHORT_VIDEO_TOPIC, svMessage);

        return UploadShortVideoResponseDTO.builder()
                .videoId(uuid)
                .build();
    }

    @Override
    public Void deleteShortVideo(UUID videoId) {
        return ShortVideoApiDelegate.super.deleteShortVideo(videoId);
    }

    @Override
    public Void setShortVideoConfiguration(ShortVideoConfigurationRequestDTO requestDTO) {
        String authorizationToken = securityContextUtils.getAuthorizationToken();
        Long personId = securityContextUtils.getCurrentPersonId();
        ShortVideoConfigurationDTO shortVideoConfigurationDTO = mapper.map(requestDTO, ShortVideoConfigurationDTO.class);
        shortVideoConfigurationDTO.setPersonId(personId);
        try {
            shortVideoRegistryRestService
                    .setShortVideoConfiguration(authorizationToken, shortVideoConfigurationDTO);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex, new BadRequestException("error.set.short.video.configuration"),
                    HttpStatus.BAD_REQUEST);
        }

        return null;
    }

    @Override
    public ShortVideoConfigurationResponseDTO getShortVideoConfiguration() {
        String authorizationToken = securityContextUtils.getAuthorizationToken();
        ShortVideoConfigurationDTO configurationDTO = getShortVideoConfigurationSettings(authorizationToken);

        return mapper.map(configurationDTO, ShortVideoConfigurationResponseDTO.class);
    }

    @Override
    public ShortVideoResponseDTO findShortVideoById(UUID videoId) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            ShortVideoDTO sv = shortVideoRegistryRestService
                    .findShortVideoById(authorizationToken, videoId);
            setPresignedURLs(sv);
            return mapper.map(sv, ShortVideoResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex, new NotFoundException("error.video.not.found", videoId.toString()),
                    HttpStatus.NOT_FOUND);
        }
        return null;
    }

    @Override
    public ShortVideoListResponseDTO findShortVideosByAuthor(Long authorId, String fromDate, Integer size, String pagingState) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            CassandraPageDTO<ShortVideoDTO> videos = shortVideoRegistryRestService
                    .findShortVideosByAuthor(authorizationToken, authorId, fromDate, size, pagingState);
            videos.getContent().forEach(this::setPresignedURLs);
            return mapper.map(videos, ShortVideoListResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoListResponseDTO findShortVideosByCategories(List<String> categories, String fromDate, Integer size, String pagingState) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            CassandraPageDTO<ShortVideoDTO> videos = shortVideoRegistryRestService
                    .findShortVideosByCategories(authorizationToken, categories, fromDate, size, pagingState);
            videos.getContent().forEach(this::setPresignedURLs);
            return mapper.map(videos, ShortVideoListResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoListResponseDTO findShortVideosOfFriends(String friendUserName, String fromDate, Integer size, String pagingState) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            Long personId = securityContextUtils.getCurrentPersonId();

            CassandraPageDTO<ShortVideoDTO> videos = shortVideoRegistryRestService
                    .findShortVideosOfFriends(authorizationToken,
                            personId, friendUserName, fromDate, size, pagingState);
            videos.getContent().forEach(this::setPresignedURLs);
            return mapper.map(videos, ShortVideoListResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoResponseDTO updateShortVideo(UUID videoId, UpdateShortVideoDTO updateShortVideoDTO) {
        try {
            UpdateShortVideoRequestDTO requestDTO = mapper.map(updateShortVideoDTO, UpdateShortVideoRequestDTO.class);
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            ShortVideoDTO shortVideoDTO = shortVideoRegistryRestService
                    .updateShortVideo(authorizationToken, videoId, requestDTO);
            setPresignedURLs(shortVideoDTO);
            return mapper.map(shortVideoDTO, ShortVideoResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoStatsResponseDTO addComment(ShortVideoCommentDTO shortVideoCommentDTO) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            commentRestService.createComment(authorizationToken,
                    mapper.map(shortVideoCommentDTO, JsonCommentData.class));
            ShortVideoStatsDTO shortVideoDTO = shortVideoRegistryRestService
                    .updateCommentStats(authorizationToken, shortVideoCommentDTO.getVideoId(), false);

            return mapper.map(shortVideoDTO, ShortVideoStatsResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoCommentDTO updateComment(Long commentId, ShortVideoCommentDTO shortVideoCommentDTO) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            return mapper.map(commentRestService.updateComment(authorizationToken, commentId,
                    mapper.map(shortVideoCommentDTO, JsonCommentData.class)), ShortVideoCommentDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoStatsResponseDTO removeComment(Long commentId, UUID videoId) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            commentRestService.removeComment(authorizationToken, commentId, videoId);
            ShortVideoStatsDTO shortVideoDTO = shortVideoRegistryRestService
                    .updateCommentStats(authorizationToken, videoId, true);

            return mapper.map(shortVideoDTO, ShortVideoStatsResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public Page<ShortVideoCommentDTO> findComments(UUID videoId, Boolean showAll, Pageable pageable) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            return commentRestService.findComments(authorizationToken,
                    videoId, showAll, pageable.getPageNumber(), pageable.getPageSize()).map(c -> mapper.map(c, ShortVideoCommentDTO.class));
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoCommentDTO getComment(Long commentId, UUID videoId) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            JsonComment comment = commentRestService.getComment(authorizationToken,
                    commentId, videoId);
            return mapper.map(comment, ShortVideoCommentDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoStatsResponseDTO reply(Long commentId, ShortVideoCommentDTO shortVideoCommentDTO) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            Long personId = securityContextUtils.getCurrentPersonId();
            shortVideoCommentDTO.setAuthorId(personId);
            commentRestService.reply(authorizationToken, commentId,
                    mapper.map(shortVideoCommentDTO, JsonCommentData.class));
            ShortVideoStatsDTO shortVideoDTO = shortVideoRegistryRestService
                    .updateCommentStats(authorizationToken, shortVideoCommentDTO.getVideoId(), false);

            return mapper.map(shortVideoDTO, ShortVideoStatsResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public Page<ShortVideoCommentDTO> getCommentReplies(Long commentId, UUID videoId, Boolean showAll, Pageable pageable) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            return commentRestService.getCommentReplies(authorizationToken,
                    commentId, videoId, showAll, pageable.getPageNumber(), pageable.getPageSize()).map(c -> mapper.map(c, ShortVideoCommentDTO.class));
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoStatsResponseDTO addShortVideoVote(UUID videoId, Integer voteType) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();
            ShortVideoStatsDTO shortVideoDTO = shortVideoRegistryRestService
                    .addShortVideoVote(authorizationToken, videoId, voteType);
            return mapper.map(shortVideoDTO, ShortVideoStatsResponseDTO.class);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    @Override
    public ShortVideoStatsResponseDTO removeShortVideoVote(UUID videoId) {
        try {
            String authorizationToken = securityContextUtils.getAuthorizationToken();

            return mapper.map(
                    shortVideoRegistryRestService.deleteShortVideoVote(authorizationToken, videoId),
                    ShortVideoStatsResponseDTO.class
            );

        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex);
        }
        return null;
    }

    private ShortVideoConfigurationDTO getShortVideoConfigurationSettings(String authorizationToken) {
        ShortVideoConfigurationDTO configurationDTO = ShortVideoConfigurationDTO
                .builder()
                .privacyLevel(PrivacyLevel.PUBLIC)
                .build();
        if (StringUtils.isBlank(authorizationToken))
            throw new ForbiddenException("error.person.not.authorized");
        try {
            configurationDTO = shortVideoRegistryRestService.getShortVideoConfiguration(authorizationToken);
        } catch (FeignException ex) {
            ExceptionUtil.processFeignException(ex, new NotFoundException("error.get.short.video.configuration"),
                    HttpStatus.NOT_FOUND);
        }
        return configurationDTO;
    }

    private String getPresignedURL(String fileName, String bucket) {
        FileStorage storage = fileStorageProvider.getStorage(StorageType.MINIO);
        return storage.getPresignedUrl(fileName, bucket);
    }

    private void setPresignedURLs(ShortVideoDTO dto) {
        dto.setUrl(getPresignedURL(dto.getId().toString(), dto.getBucket()));
        dto.setThumbnailUrl(getPresignedURL(dto.getId().toString() + THUMBNAIL_SUFFIX, dto.getBucket()));
    }

    private Set<ShortVideoFriendDTO> getFriends(String authorizationToken, PersonInfo personInfo) {
        RestPageImpl<JsonFollowing> followers = followingRestService.findSubscribers(authorizationToken,
                personInfo.getId(), 0, Integer.MAX_VALUE);
        return followers.stream().map(f -> ShortVideoFriendDTO.builder()
                .userId(f.getSubscriber().getId())
                .authorUserName(personInfo.getUsername())
                .build()).collect(Collectors.toSet());
    }

    private void validateRequiredInput(String apiKey, UploadShortVideoDTO shortVideoDTO) {
        checkNotNull(apiKey, "error.check.not.null", "apiKey");

        if (!StringUtils.equals(authProperties.getApiSecretKey(), apiKey)) {
            throw new ForbiddenException("error.api.key.invalid");
        }

        if (StringUtils.isEmpty(shortVideoDTO.getUrl())) {
            throw new BadRequestException("error.required.parameter.empty", "url");
        }

        if (StringUtils.isEmpty(shortVideoDTO.getTitle())) {
            throw new BadRequestException("error.required.parameter.empty", "title");
        }

        try {
            URL url = new URL(shortVideoDTO.getUrl());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout((1000 * 30)); //30 min

            int responseCode = urlConnection.getResponseCode();
            if (HttpURLConnection.HTTP_OK != responseCode || !FileUtil.isVideo(urlConnection.getContentType())) {
                throw new BadRequestException("error.video.url.invalid");
            }
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

}
