package iq.earthlink.social.feedaggregatorservice.util;

import iq.earthlink.social.feedaggregatorservice.dto.PostDTO;
import iq.earthlink.social.feedaggregatorservice.dto.RecentPost;

import java.util.ArrayList;
import java.util.List;

public class SortedListsUtil {

    private SortedListsUtil() {
    }

    public static List<PostDTO> mergeSortedLists(List<List<RecentPost>> lists) {
        List<PostDTO> mergedList = new ArrayList<>();

        int[] indices = new int[lists.size()];

        while (true) {
            RecentPost mostRecentPost = null;
            int minIndex = -1;

            for (int i = 0; i < lists.size(); i++) {
                List<RecentPost> currentList = lists.get(i);
                int currentIndex = indices[i];

                if (currentIndex < currentList.size()) {
                    RecentPost currentPost = currentList.get(currentIndex);
                    if (mostRecentPost == null || currentPost.getPublishedAt() > mostRecentPost.getPublishedAt()) {
                        mostRecentPost = currentPost;
                        minIndex = i;
                    }
                }
            }

            if (mostRecentPost == null) {
                break;
            }

            PostDTO postDTO = new PostDTO();
            postDTO.setPostUuid(mostRecentPost.getPostUuid());
            postDTO.setUserGroupId(mostRecentPost.getUserGroupId());

            mergedList.add(postDTO);
            indices[minIndex]++;
        }

        return mergedList;
    }

    public static List<PostDTO> combineLists(List<List<PostDTO>> lists) {
        List<PostDTO> combinedList = new ArrayList<>();
        int maxLength = 0;

        for (List<PostDTO> list : lists) {
            if (list.size() > maxLength) {
                maxLength = list.size();
            }
        }

        for (int i = 0; i < maxLength; i++) {
            for (List<PostDTO> list : lists) {
                if (i < list.size()) {
                    combinedList.add(list.get(i));
                }
            }
        }

        return combinedList;
    }
}