package iq.earthlink.social.personservice.controller.rest.person.v1.internal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import iq.earthlink.social.common.util.SynapseUtil;
import iq.earthlink.social.personservice.data.dto.synapse.*;
import iq.earthlink.social.personservice.person.PersonManager;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.security.SecurityProvider;
import iq.earthlink.social.personservice.util.Constants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


/**
 * The API controller responsible for managing matrix user credentials.
 */
@Api(tags = "Chat Synapse Api", value = "ChatSynapseApi")
@RestController
@RequestMapping(value = "/_matrix-internal/identity/v1/check_credentials", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class ChatSynapseApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatSynapseApi.class);

    private final SecurityProvider securityProvider;
    private final PersonManager personManager;

    @Value("${chat.synapse.serverName}")
    private String synapseServerName;

    public ChatSynapseApi(SecurityProvider securityProvider, PersonManager personManager) {
        this.securityProvider = securityProvider;
        this.personManager = personManager;
    }

    @GetMapping()
    @ApiOperation("End point for synapse chat server plugin for password verification.")
    public SynapsePasswordResponse checkSynapseCredentialsGet() {
        return SynapsePasswordResponse.builder()
                .auth(SynapseAuth.builder()
                        .mxid(SynapseUtil.USE_POST_REQUEST)
                        .build()
                )
                .build();
    }

    @PostMapping()
    @ApiOperation("End point for synapse chat server plugin for password verification.")
    @ApiResponses(
            @ApiResponse(message = "Tokens", code = 200, response = SynapsePasswordResponse.class)
    )
    public SynapsePasswordResponse checkSynapseCredentials(
            @RequestBody SynapsePasswordRequest synapsePasswordRequest) {

        LOGGER.info("checkSynapseCredentials synapsePasswordRequest = {}", synapsePasswordRequest);

        String token = synapsePasswordRequest.getUser().getPassword();
        UUID uuid = UUID.fromString(securityProvider.getSubjectFromJWT(token));
        Person person = personManager.getPersonByUuid(uuid);
        String personUsername = SynapseUtil.generateMatrixUserId(person.getId(), synapseServerName);

        if (!personUsername.equals(synapsePasswordRequest.getUser().getId()) || person.getDeletedDate() != null) {
            return SynapsePasswordResponse.builder()
                    .auth(SynapseAuth.builder()
                            .mxid(SynapseUtil.NOT_FOUND)
                            .build()
                    )
                    .build();
        }

        String medium = Constants.EMAIL;
        String address = person.getEmail();

        if (StringUtils.isEmpty(person.getEmail())) {
            medium = Constants.USERNAME;
            address = person.getUsername();
        }

        return SynapsePasswordResponse.builder()
                .auth(SynapseAuth.builder()
                        .mxid(SynapseUtil.USER_PREFIX + person.getId())
                        .success(true)
                        .profile(SynapseProfile.builder()
                                .displayName(person.getDisplayName())
                                .threePids(new SynapseThreePid[]{
                                        SynapseThreePid.builder()
                                                .medium(medium)
                                                .address(address)
                                                .build()
                                })
                                .build())
                        .build())
                .build();
    }

}
