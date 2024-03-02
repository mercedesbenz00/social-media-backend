package iq.earthlink.social.postservice.post;

import iq.earthlink.social.classes.data.dto.PostResponse;
import iq.earthlink.social.classes.enumeration.PostState;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.dto.PostPublicDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * This is the backend for frontend interface and all methods should be used only by rest API controller
 **/
public interface BFFPostManager {
    @Nonnull
    PostResponse createPost(String authorizationHeader,
                            @Nonnull PersonDTO personDTO,
                            @Nonnull PostData postData,
                            @Nullable MultipartFile[] files);

    @Nonnull
    PostResponse updatePost(String authorizationHeader, @Nonnull PersonDTO person, @Nonnull Long postId, UpdatePostData data, MultipartFile[] files);

    PostResponse getPostById(PersonDTO currentUser, @Nonnull Long postId);

    Page<PostResponse> findPosts(PersonDTO currentUser, @Nonnull PostSearchCriteria criteria, @Nonnull Pageable page);

    List<PostResponse> getPostsWithDetailsInternal(String authorizationHeader, List<UUID> postUuids);

    Page<PostResponse> findPostsWithComplaints(String authorizationHeader, PersonDTO person, Long groupId, PostComplaintState complainState, PostState postState, Pageable page);

    @Nonnull
    PostResponse rejectPostByComplaint(String authorizationHeader, PersonDTO person, String reason, Long complaintId);


    PostPublicDTO getPostByUuid(UUID postUuid);

}
