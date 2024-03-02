package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.personservice.person.ComplaintManager;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import iq.earthlink.social.personservice.person.rest.JsonComplaintData;
import iq.earthlink.social.personservice.person.rest.JsonPersonComplaint;
import iq.earthlink.social.personservice.security.SecurityProvider;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "Complaint Api", value = "ComplaintApi")
@RestController
@RequestMapping(value = "/api/v1/persons/complaints", produces = MediaType.APPLICATION_JSON_VALUE)
public class ComplaintApi {
    private final ComplaintManager complaintManager;
    private final Mapper mapper;
    private final PersonManager personManager;
    private final SecurityProvider securityProvider;

    public ComplaintApi(
            ComplaintManager complaintManager,
            Mapper mapper, PersonManager personManager, SecurityProvider securityProvider) {
        this.complaintManager = complaintManager;
        this.mapper = mapper;
        this.personManager = personManager;
        this.securityProvider = securityProvider;
    }

    @ApiOperation("Creates new complaint against provided person")
    @PostMapping
    public JsonPersonComplaint createComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid JsonComplaintData data) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);

        PersonComplaint complaint = complaintManager
                .createComplaint(authorizationHeader, currentUser, data);
        return mapper.map(complaint, JsonPersonComplaint.class);
    }

    @ApiOperation("Returns the list of created complaints")
    @GetMapping
    public Page<JsonPersonComplaint> findComplaints(
            @RequestHeader("Authorization") String authorizationHeader,

            @ApiParam("Filters person complaints by groups person joined")
            @RequestParam(required = false) List<Long> groupIds,

            @ApiParam(value = "Filters person complaints by persons ids")
            @RequestParam(required = false) List<Long> personIds,

            @ApiParam(value = "Filters person complaints by complaint state: PENDING (default), REJECTED, USER_BANNED_GROUP, or USER_BANNED")
            @RequestParam(required = false, defaultValue = "PENDING") PersonComplaint.PersonComplaintState state,

            Pageable page) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);

        return complaintManager.findComplaints(authorizationHeader, currentUser, groupIds, personIds, state, page)
                .map(c -> mapper.map(c, JsonPersonComplaint.class));
    }

    @ApiOperation("Removes complaint against provided person")
    @DeleteMapping("/{complaintId}")
    public void removeComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long complaintId) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);
        complaintManager.removeComplaint(currentUser, complaintId);
    }

    @ApiOperation("The moderation endpoint for rejecting created complaints")
    @PatchMapping("/{complaintId}/reject")
    public void moderateComplaint(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long complaintId,
            @ApiParam("The reason why complaint should be rejected")
            @RequestParam String reason,
            @ApiParam("Flag to resolve all pending person complaints as 'REJECTED'")
            @RequestParam(value = "resolveAll", defaultValue = "true", required = false) Boolean resolveAll

    ) {
        Long personId = securityProvider.getPersonIdFromAuthorization(authorizationHeader);
        Person currentUser = personManager.getPersonByIdInternal(personId);
        complaintManager.moderate(authorizationHeader, currentUser, complaintId, reason,
                PersonComplaint.PersonComplaintState.REJECTED, resolveAll);
    }
}
