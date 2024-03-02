package iq.earthlink.social.postprocessorservice.repository;


import iq.earthlink.social.postprocessorservice.dto.PostEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface RecentPostRepository {
    void addPostToList(PostEvent post, String messageKey);
}
