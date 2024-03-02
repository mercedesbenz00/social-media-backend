package iq.earthlink.social.postservice.post.statistics;

import iq.earthlink.social.classes.data.dto.PostDetailStatistics;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.rest.ComplaintStatsDTO;
import iq.earthlink.social.postservice.post.rest.GroupStatsDTO;
import iq.earthlink.social.postservice.post.rest.PostStatisticsGap;

import javax.annotation.Nonnull;
import java.util.List;

public interface PostStatisticsManager {

    PostStatistics getPostStatisticsByPostId(Long postId);

    List<PostDetailStatistics> getPostStatisticsByPostIds(List<Long> postIds);

    void savePostStatistics(PostStatistics statistics);

    void deletePostStatistics(Long postId);

    void synchronizePostStatistics(@Nonnull PersonDTO person);

    List<PostStatisticsGap> comparePostStatistics(PersonDTO person);

    ComplaintStatsDTO getComplaintStatsByPerson(PersonDTO currentUser, Long personId);

    GroupStatsDTO getGroupStats(PersonDTO currentUser, Long groupId);
}
