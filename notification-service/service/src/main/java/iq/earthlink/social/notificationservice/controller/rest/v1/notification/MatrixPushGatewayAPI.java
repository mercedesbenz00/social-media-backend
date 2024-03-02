package iq.earthlink.social.notificationservice.controller.rest.v1.notification;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotification;
import iq.earthlink.social.notificationservice.data.dto.matrix.MatrixNotificationResponse;
import iq.earthlink.social.notificationservice.service.matrix.MatrixNotificationManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "MatrixPushGatewayAPI", tags = "MatrixPushGateway API")
@RestController
@RequestMapping(value = "/_matrix/push/v1/notify", produces = MediaType.APPLICATION_JSON_VALUE)
public class MatrixPushGatewayAPI {

    private final MatrixNotificationManager matrixNotificationManager;

    public MatrixPushGatewayAPI(MatrixNotificationManager matrixNotificationManager) {
        this.matrixNotificationManager = matrixNotificationManager;
    }

    @PostMapping
    @ApiOperation("Returns list of notifications for configurable period.")
    public ResponseEntity<MatrixNotificationResponse> receiveMatrixNotification(@RequestBody MatrixNotification notification) {

        return ResponseEntity.status(HttpStatus.OK).body(matrixNotificationManager.pushNotification(notification));
    }
}
