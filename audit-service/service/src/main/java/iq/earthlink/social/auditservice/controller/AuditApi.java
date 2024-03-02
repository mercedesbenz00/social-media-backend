package iq.earthlink.social.auditservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import iq.earthlink.social.auditservice.dto.JsonAuditLog;
import iq.earthlink.social.auditservice.service.AuditLogSearchCriteria;
import iq.earthlink.social.auditservice.service.AuditService;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.audit.EventCategory;
import iq.earthlink.social.personservice.person.PersonInfo;
import iq.earthlink.social.personservice.rest.PersonRestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dozer.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Api(value = "AuditApi", tags = "Audit Api")
@RestController
@RequestMapping(value = "/api/v1/audit", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuditApi {
    private static final Logger LOGGER = LogManager.getLogger(AuditApi.class);

    private final AuditService auditService;
    private final Mapper mapper;
    private final PersonRestService personRestService;

    public AuditApi(AuditService auditService, Mapper mapper, PersonRestService personRestService) {
        this.auditService = auditService;
        this.mapper = mapper;
        this.personRestService = personRestService;
    }

    @ApiOperation("Returns the audit logs")
    @GetMapping
    public Page<JsonAuditLog> findAuditLogs(
            @RequestHeader("Authorization") String authorizationHeader,

            @ApiParam("Filters log messages by the query")
            @RequestParam(required = false) String query,

            @ApiParam("Filters logs by event category")
            @RequestParam(required = false) EventCategory category,

            @ApiParam("Filters logs by event action")
            @RequestParam(required = false) EventAction action,

            @ApiParam("Filters logs by event author IDs")
            @RequestParam(required = false) Long authorId, Pageable page) {

        PersonInfo currentUser = personRestService.getPersonProfile(authorizationHeader);

        AuditLogSearchCriteria criteria = AuditLogSearchCriteria.builder()
                .authorId(authorId)
                .query(query)
                .category(category)
                .action(action)
                .build();
        return currentUser.isAdmin() ?
                auditService.findLogsBySearchCriteria(criteria, page).map(g -> mapper.map(g, JsonAuditLog.class))
                : Page.empty();
    }
}

