package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.personservice.person.BlockManager;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.rest.JsonPersonBlock;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Block Api", value = "BlockApi")
@RestController
@RequestMapping(value = "/api/v1/persons/{personId}/blocks", produces = MediaType.APPLICATION_JSON_VALUE)
public class BlockApi {

  private final BlockManager blockManager;
  private final PersonManager personManager;
  private final Mapper mapper;
  private final SecurityProvider securityProvider;

  public BlockApi(
          BlockManager blockManager,
          PersonManager personManager,
          Mapper mapper, SecurityProvider securityProvider) {
    this.blockManager = blockManager;
    this.personManager = personManager;
    this.mapper = mapper;
    this.securityProvider = securityProvider;
  }

  @ApiOperation("Returns blocks created by the person")
  @GetMapping
  public Page<JsonPersonBlock> findBlocks(@PathVariable Long personId, Pageable page) {

    return blockManager.findBlocks(personManager.getPersonByIdInternal(personId), page)
            .map(b -> mapper.map(b, JsonPersonBlock.class));
  }

  @ApiOperation("Creates new block against the person")
  @PostMapping
  public JsonPersonBlock createBlock(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable Long personId) {
    Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    Person currentUser = personManager.getPersonByIdInternal(currentUserId);
    return mapper.map(blockManager.createBlock(currentUser, personManager.getPersonByIdInternal(personId)), JsonPersonBlock.class);
  }

  @ApiOperation("Removes the block against provided person")
  @DeleteMapping
  public void deleteBlock(
          @RequestHeader("Authorization") String authorizationHeader,
          @PathVariable Long personId) {
    Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
    blockManager.removeBlock(currentUserId, personManager.getPersonByIdInternal(personId));
  }
}
