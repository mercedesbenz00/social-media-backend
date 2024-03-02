package iq.earthlink.social.groupservice.group.permission;

import iq.earthlink.social.groupservice.group.GroupMemberStatus;
import iq.earthlink.social.groupservice.group.GroupPermissionData;
import iq.earthlink.social.groupservice.group.UserGroup;
import iq.earthlink.social.groupservice.group.rest.JsonGroupPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.annotation.Nonnull;
import java.util.List;

public interface GroupPermissionManager {

    JsonGroupPermission setPermission(
            @Nonnull Long personId,
            @Nonnull Boolean isAdmin,
            @Nonnull UserGroup group,
            @Nonnull GroupPermissionData data);

    GroupPermission setPermissionInternal(@Nonnull Long personId, @Nonnull UserGroup group, @Nonnull GroupPermissionData data);

    void removePermission(
            @Nonnull Long personId,
            @Nonnull Boolean isAdmin,
            @Nonnull Long permissionId);

    void removeAllGroupPermissionsInternal(@Nonnull Long groupId);

    void removeUserGroupPermissionsInternal(@Nonnull Long personId, @Nonnull Long groupId);

    boolean hasPermission(Long personId, Long groupId, GroupMemberStatus permission);

    Page<JsonGroupPermission> findPermissions(@Nonnull PermissionSearchCriteria criteria,
                                              @Nonnull Long personId, boolean isAdmin,
                                              @Nonnull String authorizationHeader, Pageable page);

    List<GroupPermission> findPermissionsInternal(@Nonnull PermissionSearchCriteria criteria);

    List<GroupPermission> findPermissionsInternal(Long personId, Long groupId);

    List<Long> getAccessibleGroups(@Nonnull Long personId, @Nonnull Boolean isAdmin, List<Long> groupIds);
}
