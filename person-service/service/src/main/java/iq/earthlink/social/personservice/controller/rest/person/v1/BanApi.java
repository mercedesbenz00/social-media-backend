package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.personservice.person.BanManager;
import iq.earthlink.social.personservice.person.BanSearchCriteria;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonBan;
import iq.earthlink.social.personservice.person.model.PersonGroupBan;
import iq.earthlink.social.personservice.person.rest.JsonPersonBan;
import iq.earthlink.social.personservice.person.rest.JsonPersonBanRequest;
import iq.earthlink.social.personservice.person.rest.JsonPersonGroupBan;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.apache.commons.collections.CollectionUtils;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Api(tags = "Ban Api", value = "BanApi")
@RestController
@RequestMapping(value = "/api/v1/persons/bans", produces = MediaType.APPLICATION_JSON_VALUE)
public class BanApi {

    private final BanManager banManager;
    private final PersonManager personManager;
    private final Mapper mapper;
    private final SecurityProvider securityProvider;

    public BanApi(BanManager banManager, PersonManager personManager, Mapper mapper, SecurityProvider securityProvider) {
        this.banManager = banManager;
        this.personManager = personManager;
        this.mapper = mapper;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates person ban")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If ban successfully created"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authenticated"),
            @ApiResponse(code = 404, message = "If target person is not found"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred"),
    })
    @PostMapping
    public JsonPersonBan createPersonBan(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonPersonBanRequest request) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);

        PersonBan ban = banManager.createBan(authorizationHeader, currentUser, request);
        return mapper.map(ban, JsonPersonBan.class);
    }

    @ApiOperation("Creates person group bans")
    @ApiResponses({
            @ApiResponse(code = 200, message = "If group ban successfully created"),
            @ApiResponse(code = 400, message = "If invalid data provided"),
            @ApiResponse(code = 401, message = "If user is not authenticated"),
            @ApiResponse(code = 404, message = "If target person is not found"),
            @ApiResponse(code = 500, message = "If any unexpected error occurred"),
    })
    @PostMapping("/groupBans")
    public List<JsonPersonGroupBan> createPersonGroupBans(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonPersonBanRequest request) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);

        List<PersonGroupBan> groupBans = banManager.createGroupBans(authorizationHeader, currentUser, request);
        return groupBans.stream().map(gb -> mapper.map(gb, JsonPersonGroupBan.class)).toList();
    }

    @ApiOperation("Returns person bans (can be filtered by the given criteria)")
    @GetMapping
    public Page<JsonPersonBan> getBans(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters bans by the person first, last, or display name matching the query")
            @RequestParam(required = false, defaultValue = "") String query,
            @ApiParam("Filters bans by the person ID")
            @RequestParam(required = false) Long personId,
            @ApiParam("Filters active bans only if set to true or omitted")
            @RequestParam(defaultValue="true") Boolean active,
            Pageable page) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        BanSearchCriteria criteria = BanSearchCriteria.builder()
                .query(query)
                .bannedPersonId(personId)
                .active(active)
                .build();

        return banManager.findBans(criteria, currentUser, page).map(b -> mapper.map(b, JsonPersonBan.class));
    }

    @ApiOperation("Returns group bans(can be filtered by the given criteria)")
    @GetMapping("/groupBans")
    public Page<JsonPersonGroupBan> getGroupBans(
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Filters bans by the person first, last, or display name matching the query")
            @RequestParam(required = false) String query,
            @ApiParam("Filters bans by the person ID")
            @RequestParam(required = false) Long[] personIds,
            @ApiParam("Filters bans by the list of Group IDs, required")
            @RequestParam() Set<Long> groupIds,
            @ApiParam("Filters active bans only if set to true or omitted")
            @RequestParam(defaultValue="true") Boolean active,
            Pageable page) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        BanSearchCriteria criteria = BanSearchCriteria.builder()
                .query(query)
                .bannedPersonIds(personIds)
                .groupIds(new ArrayList<>(groupIds))
                .active(active)
                .build();

        return banManager.findGroupBans(authorizationHeader, criteria, currentUser, page).map(b -> mapper.map(b, JsonPersonGroupBan.class));
    }

    @ApiOperation("Removes ban for person, if ban exists")
    @DeleteMapping("/{banId}")
    public void removeBan(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long banId) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        banManager.removeBan(authorizationHeader, currentUser, banId);
    }

    @ApiOperation("Removes group ban for person, if ban exists")
    @DeleteMapping("/groupBans/{groupBanId}")
    public void removeGroupBan(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupBanId) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        banManager.removeGroupBan(authorizationHeader, currentUser, groupBanId);
    }

    @PutMapping("/{banId}")
    @ApiOperation("Updates ban, if ban exists")
    @ApiResponses(
            @ApiResponse(message = "Ban successfully updated", code = 200, response = JsonPersonBan.class)
    )
    public JsonPersonBan updateBan(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long banId,
            @RequestBody @Valid JsonPersonBanRequest request) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        return mapper.map(banManager.updateBan(authorizationHeader, banId, currentUser, request), JsonPersonBan.class);
    }

    @PutMapping("/groupBans/{groupBanId}")
    @ApiOperation("Updates group ban, if exists")
    @ApiResponses(
            @ApiResponse(message = "Group ban successfully updated", code = 200, response = JsonPersonBan.class)
    )
    public JsonPersonGroupBan updateGroupBan(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long groupBanId,
            @RequestBody @Valid JsonPersonBanRequest request) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);

        return mapper.map(banManager.updateGroupBan(authorizationHeader, groupBanId, currentUser, request), JsonPersonGroupBan.class);
    }

    @ApiOperation("Checks if user is banned from groups")
    @GetMapping("/groupBans/exist")
    public Boolean isPersonBannedFromGroups (
            @ApiParam("Person ID, required")
            @RequestParam() Long personId,
            @ApiParam("Group ID, required")
            @RequestParam() Long groupId) {

        List<PersonGroupBan> bans = banManager.getActiveGroupBans(personId, groupId);

        // Find group bans filtered by provided group ID:
        return CollectionUtils.isNotEmpty(bans);
    }

    @ApiOperation("Find current user's active group bans")
    @GetMapping("/my-group-bans")
    public List<JsonPersonGroupBan> findMyGroupBans (
            @RequestHeader("Authorization") String authorizationHeader,
            @ApiParam("Group ID, required")
            @RequestParam() Long groupId) {

        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);

        // Find current user group bans filtered by provided group ID:
        List<PersonGroupBan> bans = banManager.getActiveGroupBans(currentUserId, groupId);

        return bans.stream().map(b -> mapper.map(b, JsonPersonGroupBan.class)).toList();
    }

    @ApiOperation("Ban person in group by complaint")
    @PatchMapping("/complaints/{complaintId}/groupBan")
    public JsonPersonGroupBan banPersonInGroupByComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long complaintId,
            @RequestParam("reason")
            @ApiParam("Ban Reason (text)") String reason,
            @RequestParam(value = "days", required = false, defaultValue = "1")
            @ApiParam("How many days person should be banned") Integer days,
            @ApiParam("Flag to resolve all pending person complaints within group as 'USER_BANNED_GROUP'")
            @RequestParam(value = "resolveAll", defaultValue = "true", required = false) Boolean resolveAll) {
        Long currentUserId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(currentUserId);
        return mapper.map(banManager.banPersonInGroupByComplaint(authorizationHeader, currentUser, reason, complaintId, days, resolveAll), JsonPersonGroupBan.class);
    }
}
