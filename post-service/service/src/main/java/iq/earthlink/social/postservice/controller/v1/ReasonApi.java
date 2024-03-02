package iq.earthlink.social.postservice.controller.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.postservice.person.CurrentUser;
import iq.earthlink.social.postservice.person.dto.PersonDTO;
import iq.earthlink.social.postservice.post.complaint.ReasonManager;
import iq.earthlink.social.postservice.post.complaint.ReasonSearchCriteria;
import iq.earthlink.social.postservice.post.complaint.model.Reason;
import iq.earthlink.social.postservice.post.rest.JsonReasonWithLocalization;
import iq.earthlink.social.postservice.post.rest.ReasonRequest;
import org.dozer.Mapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Locale;

@Api(value = "ComplaintReasonApi", tags = "Complaint Reason Api")
@RestController
@RequestMapping(value = "/api/v1/posts/complaints/reasons", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReasonApi {

    private final ReasonManager reasonManager;
    private final Mapper mapper;

    public ReasonApi(
            ReasonManager reasonManager,
            Mapper mapper) {
        this.reasonManager = reasonManager;
        this.mapper = mapper;
    }

    @PostMapping
    @ApiOperation("Creates new complaint reason")
    @ApiResponses(
            @ApiResponse(message = "Complaint reason successfully created", code = 200, response = Reason.class)
    )
    public JsonReasonWithLocalization createComplaintReason(@RequestBody @Valid ReasonRequest reasonRequest,
                                                            @CurrentUser PersonDTO person) {

        LocaleContextHolder.setLocale(Locale.getDefault());
        return mapper.map(reasonManager.createComplaintReason(reasonRequest, person), JsonReasonWithLocalization.class);
    }

    @PutMapping("/{reasonId}")
    @ApiOperation("Updates complaint reason by id")
    @ApiResponses(
            @ApiResponse(message = "Complaint reason successfully updated", code = 200, response = Reason.class)
    )
    public JsonReasonWithLocalization updateComplaintReason(@PathVariable("reasonId") Long reasonId,
                                                            @RequestBody @Valid ReasonRequest reasonRequest,
                                                            @CurrentUser PersonDTO person) {

        return mapper.map(reasonManager.updateComplaintReason(reasonId, reasonRequest, person), JsonReasonWithLocalization.class);
    }

    @DeleteMapping(value = "/{reasonId}")
    @ApiOperation("Removes reason by id")
    @ApiResponses(
            @ApiResponse(message = "Complaint reason successfully removed", code = 200)
    )
    public void deleteComplaintReason(@PathVariable Long reasonId,
                                      @CurrentUser PersonDTO person) {

        reasonManager.removeComplaintReason(reasonId, person);
    }

    @GetMapping("/{reasonId}")
    @ApiOperation("Get complaint reason by id")
    @ApiResponses(
            @ApiResponse(message = "Get complaint reason", code = 200)
    )
    public JsonReasonWithLocalization getReason(@PathVariable Long reasonId) {
        return mapper.map(reasonManager.getComplaintReason(reasonId), JsonReasonWithLocalization.class);
    }

    @GetMapping
    @ApiOperation("Find reasons by the criteria")
    public Page<JsonReasonWithLocalization> getReasons(
            @ApiParam("Filters reasons if the name matched by the query")
            @RequestParam(required = false) String query,
            Pageable pageable) {

        ReasonSearchCriteria criteria = ReasonSearchCriteria.builder()
                .query(query)
                .locale(LocaleContextHolder.getLocale().getLanguage())
                .build();

        return reasonManager.findComplaintReasons(criteria, pageable).map(c -> mapper.map(c, JsonReasonWithLocalization.class));
    }

}
