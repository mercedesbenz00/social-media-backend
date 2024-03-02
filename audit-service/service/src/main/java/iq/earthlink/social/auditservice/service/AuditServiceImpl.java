package iq.earthlink.social.auditservice.service;

import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import iq.earthlink.social.auditservice.model.AuditLog;
import iq.earthlink.social.auditservice.repository.ESClientConnector;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.inject.CreationException;
import org.elasticsearch.common.inject.spi.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private static final Logger LOGGER = LogManager.getLogger(AuditServiceImpl.class);

    private final ESClientConnector esClientConnector;

    @Override
    public AuditLog createAuditLog(AuditLog log) {
        IndexResponse response = esClientConnector.insertAuditLog(log);
        if (Result.Created.equals(response.result())) {
            return log;
        } else {
            throw new CreationException(Collections.singletonList(new Message(response.result().toString())));
        }
    }

    @Override
    public List<AuditLog> findLogsBySearchCriteria(AuditLogSearchCriteria criteria) {
        try {
            return esClientConnector.findAuditLogsBySearchCriteria(criteria);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @Override
    public Page<AuditLog> findLogsBySearchCriteria(AuditLogSearchCriteria criteria, Pageable page) {
        try {
            return esClientConnector.findAuditLogsBySearchCriteria(criteria, page);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Page.empty();
    }

    @Override
    public AuditLog getAuditLogById(String id) {
        List<AuditLog> logs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(esClientConnector.findAllLogs())) {
            logs = findLogsBySearchCriteria(AuditLogSearchCriteria.builder().id(id).build());
        }
        return !logs.isEmpty() ? logs.get(0) : null;
    }

}
