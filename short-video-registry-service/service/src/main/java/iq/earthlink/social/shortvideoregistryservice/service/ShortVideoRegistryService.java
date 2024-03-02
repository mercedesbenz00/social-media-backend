package iq.earthlink.social.shortvideoregistryservice.service;

import com.datastax.oss.protocol.internal.util.Bytes;
import iq.earthlink.social.classes.enumeration.PrivacyLevel;
import iq.earthlink.social.classes.enumeration.VoteType;
import iq.earthlink.social.common.util.CommonUtil;
import iq.earthlink.social.common.util.DateUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.shortvideoregistryservice.dto.*;
import iq.earthlink.social.shortvideoregistryservice.model.*;
import iq.earthlink.social.shortvideoregistryservice.repository.*;
import iq.earthlink.social.shortvideoregistryservice.util.SecurityContextUtils;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static iq.earthlink.social.classes.enumeration.PrivacyLevel.SELECTED_GROUPS;
import static iq.earthlink.social.classes.enumeration.PrivacyLevel.SELECTED_USERS;
import static iq.earthlink.social.common.util.CommonConstants.ERROR_CHECK_NOT_NULL;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
public class ShortVideoRegistryService {
    public static final String CATEGORIES = "categories";
    private static final String USER_ID = "userId";
    private static final String VIDEO_ID = "videoId";
    private static final String ERROR_VIDEO_NOT_FOUND = "error.video.not.found";
    private final Mapper mapper;
    private final ShortVideoRepository shortVideoRepository;
    private final CassandraTemplate cassandraTemplate;
    private final ShortVideoConfigurationRepository shortVideoConfigurationRepository;
    private final ShortVideosOfCategoryRepository shortVideosOfCategoryRepository;
    private final ShortVideosByAuthorRepository shortVideosByAuthorRepository;
    private final ShortVideosOfFriendsRepository shortVideosOfFriendsRepository;
    private final ShortVideoAuthorFriendsRepository shortVideoAuthorFriendsRepository;
    private final ShortVideoVoteRepository shortVideoVoteRepository;
    private final ShortVideoStatsRepository shortVideoStatsRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityContextUtils securityContextUtils;


    public ShortVideoRegistryService(ShortVideoRepository shortVideoRepository,
                                     ShortVideoConfigurationRepository shortVideoConfigurationRepository,
                                     CategoryRepository categoryRepository,
                                     SecurityContextUtils securityContextUtils,
                                     Mapper mapper,
                                     CassandraTemplate cassandraTemplate,
                                     ShortVideosOfCategoryRepository shortVideosOfCategoryRepository,
                                     ShortVideosByAuthorRepository shortVideosByAuthorRepository,
                                     ShortVideosOfFriendsRepository shortVideosOfFriendsRepository,
                                     ShortVideoAuthorFriendsRepository shortVideoAuthorFriendsRepository, ShortVideoVoteRepository shortVideoVoteRepository, ShortVideoStatsRepository shortVideoStatsRepository) {
        this.shortVideoRepository = shortVideoRepository;
        this.shortVideoConfigurationRepository = shortVideoConfigurationRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
        this.securityContextUtils = securityContextUtils;
        this.cassandraTemplate = cassandraTemplate;
        this.shortVideosOfCategoryRepository = shortVideosOfCategoryRepository;
        this.shortVideosByAuthorRepository = shortVideosByAuthorRepository;
        this.shortVideosOfFriendsRepository = shortVideosOfFriendsRepository;
        this.shortVideoAuthorFriendsRepository = shortVideoAuthorFriendsRepository;
        this.shortVideoVoteRepository = shortVideoVoteRepository;
        this.shortVideoStatsRepository = shortVideoStatsRepository;
    }


    public ShortVideoDTO findShortVideoById(UUID videoId) {
        ShortVideo video = shortVideoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ERROR_VIDEO_NOT_FOUND, videoId.toString()));
        ShortVideoDTO shortVideoDTO = mapper.map(video, ShortVideoDTO.class);
        shortVideoDTO.setCategories(getVideoCategories(video.getCategories()));
        setShortVideoStats(videoId, shortVideoDTO);
        return shortVideoDTO;
    }

    public CassandraPageDTO<ShortVideoDTO> findShortVideosByAuthor(@Nonnull Long authorId, String fromDate, PageInfo pageInfo) {
        checkNotNull(authorId, ERROR_CHECK_NOT_NULL, "authorId");

        LocalDate date = DateUtil.getDateFromString(fromDate);
        int year = date.getYear();

        Slice<ShortVideoDTO> authorVideos = shortVideosByAuthorRepository
                .findByAuthorIdAndYearAndCreatedAtGreaterThanEqual(authorId, year, date, pageInfo.getPageable())
                .orElseThrow(() -> new NotFoundException("error.not.found.videos.for.author", authorId))
                .map(shortVideosByAuthor -> mapper.map(shortVideosByAuthor, ShortVideoDTO.class));
        authorVideos.forEach(v -> setShortVideoStats(v.getId(), v));
        return getPaginatedResult(authorVideos);
    }

    public CassandraPageDTO<ShortVideoDTO> findShortVideosByCategory(@Nonnull List<UUID> categoryIds, String fromDate, PageInfo pageInfo) {
        checkNotNull(categoryIds, ERROR_CHECK_NOT_NULL, CATEGORIES);

        LocalDate date = DateUtil.getDateFromString(fromDate);
        int year = date.getYear();
        Timestamp timestamp = Timestamp.valueOf(date.atStartOfDay());
        Slice<ShortVideoDTO> shortVideosOfCategories = shortVideosOfCategoryRepository
                .findByCategoryIdInAndYearAndCreatedAtGreaterThanEqual(categoryIds, year, timestamp, pageInfo.getPageable())
                .orElseThrow(() -> new NotFoundException("error.not.found.videos.by.categories", categoryIds))
                .map(shortVideosOfCategory -> mapper.map(shortVideosOfCategory, ShortVideoDTO.class));
        shortVideosOfCategories.forEach(v -> setShortVideoStats(v.getId(), v));
        return getPaginatedResult(shortVideosOfCategories);
    }

    public CassandraPageDTO<ShortVideoDTO> findShortVideosOfFriends(Long userId, String friendUserName, String fromDate, PageInfo pageInfo) {
        checkNotNull(friendUserName, ERROR_CHECK_NOT_NULL, "friendUserName");

        LocalDate date = DateUtil.getDateFromString(fromDate);
        int year = date.getYear();
        Timestamp timestamp = Timestamp.valueOf(date.atStartOfDay());

        Slice<ShortVideoDTO> shortVideosOfFriends = shortVideosOfFriendsRepository
                .findByUserIdAndYearAndAuthorUserNameAndCreatedAtGreaterThanEqual(userId, year, friendUserName, timestamp, pageInfo.getPageable())
                .map(shortVideoOfFriends -> mapper.map(shortVideoOfFriends, ShortVideoDTO.class));
        shortVideosOfFriends.forEach(v -> setShortVideoStats(v.getId(), v));
        return getPaginatedResult(shortVideosOfFriends);
    }

    public Void setShortVideoConfiguration(ShortVideoConfigurationDTO configuration) {
        checkNotNull(configuration, ERROR_CHECK_NOT_NULL, "configuration");
        checkConfiguration(configuration);

        ShortVideoConfiguration shortVideoConfiguration = shortVideoConfigurationRepository
                .findByPersonId(configuration.getPersonId())
                .orElse(ShortVideoConfiguration
                        .builder()
                        .id(UUID.randomUUID())
                        .personId(configuration.getPersonId())
                        .build());

        shortVideoConfiguration.setPrivacyLevel(PrivacyLevel.valueOf(configuration.getPrivacyLevel().toString()));

        if (Objects.nonNull(configuration.getCommentsAllowed()))
            shortVideoConfiguration.setCommentsAllowed(configuration.getCommentsAllowed());

        if (SELECTED_USERS.equals(configuration.getPrivacyLevel())) {
            shortVideoConfiguration.setSelectedUsers(Set.copyOf(configuration.getSelectedUsers()));
        } else if (SELECTED_GROUPS.equals(configuration.getPrivacyLevel())) {
            shortVideoConfiguration.setSelectedGroups(Set.copyOf(configuration.getSelectedGroups()));
        } else {
            if (!CollectionUtils.isEmpty(shortVideoConfiguration.getSelectedUsers()))
                shortVideoConfiguration.getSelectedUsers().clear();
            if (!CollectionUtils.isEmpty(shortVideoConfiguration.getSelectedGroups()))
                shortVideoConfiguration.getSelectedGroups().clear();
        }

        shortVideoConfigurationRepository.save(shortVideoConfiguration);

        return null;
    }

    @NotNull
    public ShortVideoConfigurationDTO getShortVideoConfiguration() {
        Long personId = securityContextUtils.getCurrentPersonId();
        ShortVideoConfiguration shortVideoConfiguration = shortVideoConfigurationRepository
                .findByPersonId(personId)
                .orElseGet(() -> shortVideoConfigurationRepository.save(ShortVideoConfiguration
                        .builder()
                        .id(UUID.randomUUID())
                        .privacyLevel(PrivacyLevel.PUBLIC)
                        .personId(personId)
                        .selectedUsers(Set.of())
                        .selectedGroups(Set.of())
                        .commentsAllowed(true)
                        .build())
                );

        return mapper.map(shortVideoConfiguration, ShortVideoConfigurationDTO.class);
    }

    public ShortVideoDTO updateShortVideo(UUID videoId, UpdateShortVideoRequestDTO updateShortVideoDTO) {
        Long personId = securityContextUtils.getCurrentPersonId();

        ShortVideo video = shortVideoRepository.findById(videoId)
                .orElseThrow(() -> new NotFoundException(ERROR_VIDEO_NOT_FOUND, videoId.toString()));

        if (video.getAuthorId() != (personId)) {
            throw new ForbiddenException("error.person.not.authorized");
        }

        Set<UUID> categoryIds = getCategoryIds(updateShortVideoDTO.getCategories());
        video.getCategories().addAll(categoryIds);

        checkConfiguration(updateShortVideoDTO.getConfiguration());
        BeanUtils.copyProperties(updateShortVideoDTO, video, CommonUtil.getPropertyNamesToIgnore(updateShortVideoDTO, true, CATEGORIES));
        CassandraBatchOperations batchOps = cassandraTemplate.batchOps();
        saveShortVideoOfAuthor(video, batchOps);
        saveShortVideoOfCategory(video, batchOps);
        saveShortVideoOfFriends(Collections.emptySet(), video, batchOps);
        batchOps.insert(video);
        batchOps.execute();

        ShortVideoDTO saved = mapper.map(video, ShortVideoDTO.class);
        saved.setCategories(getVideoCategories(video.getCategories()));
        setShortVideoStats(videoId, saved);

        return saved;
    }

    public ShortVideoStatsDTO addShortVideoVote(Long userId, UUID videoId, Integer voteType) {
        checkNotNull(userId, ERROR_CHECK_NOT_NULL, USER_ID);
        checkNotNull(videoId, ERROR_CHECK_NOT_NULL, VIDEO_ID);
        checkNotNull(voteType, ERROR_CHECK_NOT_NULL, "voteType");

        // Validate vote type:
        VoteType type = VoteType.getVoteType(voteType);

        if (type == null) {
            throw new BadRequestException("error.vote.type.not.supported", voteType);
        }

        // Validate short video ID:
        if (shortVideoRepository.findById(videoId).isEmpty()){
            throw new NotFoundException(ERROR_VIDEO_NOT_FOUND, videoId.toString());
        }
        Optional<ShortVideoVote> personVote = shortVideoVoteRepository.findByPersonIdAndId(userId, videoId);

        boolean personVoteChanged = personVote.isPresent() && personVote.get().getVoteType() != voteType;

        ShortVideoVote vote = ShortVideoVote.builder()
                .personId(userId).id(videoId).voteType(voteType).createdAt(new Timestamp(System.currentTimeMillis())).build();

        if (personVote.isEmpty() || personVoteChanged) {
            shortVideoVoteRepository.save(vote);
            updateVoteStats(type, personVoteChanged, videoId, false);
        }
        // Return updated statistics:
        ShortVideoStats stats = shortVideoStatsRepository.findById(videoId).orElseGet(() -> ShortVideoStats.builder().id(videoId).build());
        return mapper.map(stats, ShortVideoStatsDTO.class);
    }

    public ShortVideoStatsDTO deleteShortVideoVote(Long userId, UUID videoId) {
        checkNotNull(userId, ERROR_CHECK_NOT_NULL, USER_ID);
        checkNotNull(videoId, ERROR_CHECK_NOT_NULL, VIDEO_ID);

        // Validate short video ID:
        if (shortVideoRepository.findById(videoId).isEmpty()){
            throw new NotFoundException(ERROR_VIDEO_NOT_FOUND, videoId.toString());
        }

        // Find user vote:
        Optional<ShortVideoVote> personVote = shortVideoVoteRepository.findByPersonIdAndId(userId, videoId);

        if (personVote.isPresent()) {
            VoteType personVoteType = VoteType.getVoteType(personVote.get().getVoteType());
            shortVideoVoteRepository.delete(personVote.get());
            updateVoteStats(personVoteType, false, videoId, true);
        }
        // Return updated  statistics:
        ShortVideoStats stats = shortVideoStatsRepository.findById(videoId).orElseGet(() -> ShortVideoStats.builder().id(videoId).build());
        return mapper.map(stats, ShortVideoStatsDTO.class);
    }

    public ShortVideoStatsDTO updateCommentStats(Long userId, UUID videoId, boolean commentDeleted) {
        checkNotNull(userId, ERROR_CHECK_NOT_NULL, USER_ID);
        checkNotNull(videoId, ERROR_CHECK_NOT_NULL, VIDEO_ID);

        long commentOffset = commentDeleted ? -1 : 1;
        shortVideoStatsRepository.updateComments(videoId, commentOffset);

        // Return updated  statistics:
        ShortVideoStats stats = shortVideoStatsRepository.findById(videoId).orElseGet(() -> ShortVideoStats.builder().id(videoId).build());
        return mapper.map(stats, ShortVideoStatsDTO.class);
    }

    public void saveShortVideoOfFriends(@Nonnull Set<ShortVideoFriendDTO> friends, ShortVideo shortVideo, CassandraBatchOperations batchOps) {
        if (!friends.isEmpty()) {
            friends.forEach(f -> {
                ShortVideosOfFriends svf = mapper.map(shortVideo, ShortVideosOfFriends.class);
                svf.setUserId(f.getUserId());
                svf.setAuthorUserName(f.getAuthorUserName());
                batchOps.insert(svf);
            });
        } else {
            Optional<List<ShortVideoAuthorFriends>> authorFriends = shortVideoAuthorFriendsRepository.findByAuthorId(shortVideo.getAuthorId());

            if (authorFriends.isPresent()) {
                val allFriends = authorFriends.get();
                allFriends.forEach(f -> {
                    ShortVideosOfFriends svf = mapper.map(shortVideo, ShortVideosOfFriends.class);
                    svf.setUserId(f.getFriendUserId());
                    svf.setAuthorUserName(f.getAuthorUsername());
                    batchOps.insert(svf);
                });
            }
        }
    }

    public void saveShortVideoOfCategory(ShortVideo shortVideo, CassandraBatchOperations batchOps) {
        shortVideo
                .getCategories()
                .forEach(c -> {
                    ShortVideosOfCategory svc = mapper.map(shortVideo, ShortVideosOfCategory.class);
                    svc.setCategoryId(c);
                    batchOps.insert(svc);
                });
    }

    public void saveShortVideoOfAuthor(ShortVideo shortVideo, CassandraBatchOperations batchOps) {
        batchOps.insert(mapper.map(shortVideo, ShortVideoByAuthor.class));
    }

    public void saveAuthorFriends(Set<ShortVideoFriendDTO> friends, Long authorId, CassandraBatchOperations batchOps) {
        friends.forEach(friend -> batchOps.insert(
                ShortVideoAuthorFriends
                        .builder()
                        .authorId(authorId)
                        .authorUsername(friend.getAuthorUserName())
                        .friendUserId(friend.getUserId())
                        .build()));
    }

    private void checkConfiguration(ShortVideoConfigurationDTO shortVideoConfigurationJson) {
        if (SELECTED_USERS.equals(shortVideoConfigurationJson.getPrivacyLevel()) && CollectionUtils.isEmpty(shortVideoConfigurationJson.getSelectedUsers())) {
            throw new BadRequestException("error.empty.or.wrong.users.list");
        }
        if (SELECTED_GROUPS.equals(shortVideoConfigurationJson.getPrivacyLevel()) && CollectionUtils.isEmpty(shortVideoConfigurationJson.getSelectedGroups())) {
            throw new BadRequestException("error.empty.or.wrong.groups.list");
        }
    }

    private Set<UUID> getCategoryIds(Set<ShortVideoCategoryDTO> categories) {
        Set<UUID> categoryIds = new HashSet<>();

        if (!CollectionUtils.isEmpty(categories)) {
            categories.forEach(category -> {
                UUID categoryId = category.getCategoryId();
                if (categoryId == null) {
                    Category shortVideoCategory = categoryRepository
                            .findByName(category.getName())
                            .orElseGet(() -> categoryRepository.save(Category
                                    .builder()
                                    .id(category.getCategoryId())
                                    .name(category.getName())
                                    .build())
                            );
                    categoryIds.add(shortVideoCategory.getId());
                } else {
                    categoryIds.add(categoryId);
                }
            });
        }
        return categoryIds;
    }

    private Set<ShortVideoCategoryDTO> getVideoCategories(UUID videoId) {
        Set<ShortVideoCategoryDTO> categoriesDto = new HashSet<>();
        Optional<ShortVideo> video = shortVideoRepository.findById(videoId);
        if (video.isPresent()) {
            Set<UUID> categoryIds = video.get().getCategories();
            categoriesDto = getVideoCategories(categoryIds);
        }
        return categoriesDto;
    }

    private Set<ShortVideoCategoryDTO> getVideoCategories(Set<UUID> categoryIds) {
        if(!Objects.isNull(categoryIds) && !categoryIds.isEmpty()) {
            Set<ShortVideoCategoryDTO> categoriesDto = new HashSet<>();
            Optional<List<Category>> objCategories = categoryRepository.findByIdIn(categoryIds);
            if (objCategories.isPresent()) {
                List<Category> categories = objCategories.get();
                categoriesDto = categories.stream().map(c -> mapper.map(c, ShortVideoCategoryDTO.class)).collect(Collectors.toSet());
            }
            return categoriesDto;
        }
        return Collections.emptySet();
    }

    private CassandraPageDTO<ShortVideoDTO> getPaginatedResult(Slice<ShortVideoDTO> slice) {
        String pagingState = null;

        if (slice.hasNext()) {
            CassandraPageRequest pageRequest = (CassandraPageRequest) slice.nextPageable();
            ByteBuffer bytes = pageRequest.getPagingState();

            pagingState = Bytes.toHexString(bytes);
        }
        slice.forEach(r -> r.setCategories(getVideoCategories(r.getId())));
        return new CassandraPageDTO<>(slice, pagingState);
    }

    private void updateVoteStats(VoteType voteType, boolean personVoteChanged, UUID videoId, boolean deleted) {
        assert voteType != null;
        long likesOffset;
        if (VoteType.UPVOTE.equals(voteType)) {
            likesOffset = deleted ? -1 : 1;
        } else {
            likesOffset = 0;
        }
        long dislikesOffset;
        if (VoteType.DOWNVOTE.equals(voteType)) {
            dislikesOffset = deleted ? -1 : 1;
        } else {
            dislikesOffset = 0;
        }

        if (personVoteChanged) {
            likesOffset = VoteType.UPVOTE.equals(voteType) ? 1 : -1;
            dislikesOffset = VoteType.DOWNVOTE.equals(voteType) ? 1 : -1;
        }
        shortVideoStatsRepository.updateVotes(videoId, likesOffset, dislikesOffset);
    }

    private void setShortVideoStats(UUID videoId, ShortVideoDTO shortVideoDTO) {
        ShortVideoStats stats = shortVideoStatsRepository.findById(videoId).orElse(ShortVideoStats.builder().id(videoId).build());
        shortVideoDTO.setStats(mapper.map(stats, ShortVideoStatsDTO.class));
    }
}
