package iq.earthlink.social.groupservice.controller.rest.v1.tag;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.groupservice.tag.Tag;
import iq.earthlink.social.groupservice.tag.TagManager;
import iq.earthlink.social.groupservice.util.RoleUtil;
import iq.earthlink.social.security.DefaultSecurityProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Api(value = "The Tag API allows to manage tags for the groups")
@RestController
@RequestMapping("/api/v1/tags")
public class TagApi {

  private final TagManager tagManager;
  private final DefaultSecurityProvider securityProvider;
  private final RoleUtil roleUtil;

  public TagApi(TagManager tagManager, DefaultSecurityProvider securityProvider, RoleUtil roleUtil) {
    this.tagManager = tagManager;
    this.securityProvider = securityProvider;
    this.roleUtil = roleUtil;
  }

  @PostMapping
  @ApiOperation("Create tag")
  public Tag createTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @RequestParam String name
  ) {
    Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    return tagManager.createTag(personId, name);
  }

  @GetMapping
  @ApiOperation("Get all tags")
  public Page<Tag> getTags(
      @RequestParam(required = false) String query,
      Pageable page) {

    return tagManager.findTags(query, page);
  }

  @GetMapping("/{tagId}")
  @ApiOperation("Get tag by id")
  public Tag getTag(
      @PathVariable
      @ApiParam("id of tag to be retrieved") Long tagId) {

    return tagManager.getTag(tagId);
  }

  @DeleteMapping("/{tagId}")
  @ApiOperation("Delete tag")
  public void deleteTag(
      @RequestHeader("Authorization") String authorizationHeader,
      @PathVariable("tagId")
      @ApiParam("id of tag to be deleted") Long tagId) {
    Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    String[] personRoles = securityProvider.getRolesFromAuthorization(authorizationHeader);
    boolean isAdmin = roleUtil.isAdmin(personRoles);

    tagManager.delete(personId, isAdmin, tagId);
  }

}
