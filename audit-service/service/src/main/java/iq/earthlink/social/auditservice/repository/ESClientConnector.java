package iq.earthlink.social.auditservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import iq.earthlink.social.auditservice.model.AuditLog;
import iq.earthlink.social.auditservice.service.AuditLogSearchCriteria;
import iq.earthlink.social.auditservice.util.QueryBuilderUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static iq.earthlink.social.common.util.CommonConstants.*;

@Component
@RequiredArgsConstructor
public class ESClientConnector {
    private static final Logger LOGGER = LogManager.getLogger(ESClientConnector.class);

    @Value("${elastic.index}")
    private String index;

    @Autowired
    private ElasticsearchClient elasticsearchClient;
    @NonNull
    final ElasticsearchOperations esTemplate;

    public List<AuditLog> findAuditLogsBySearchCriteria(AuditLogSearchCriteria criteria){
        List<AuditLog> logs = new ArrayList<>();

        try {
            List<Query> queries = prepareQueryList(criteria);
            SearchResponse<AuditLog> response = elasticsearchClient.search(req -> req.index(index)
                            .query(query -> query
                                    .bool(bool -> bool
                                            .filter(queries))), AuditLog.class);
            logs = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return logs;
    }

    @SuppressWarnings("unchecked")
    public Page<AuditLog> findAuditLogsBySearchCriteria(AuditLogSearchCriteria criteria, Pageable page){
        return (Page<AuditLog>) SearchHitSupport.unwrapSearchHits(searchPage(criteria, page));
    }

    public List<AuditLog> findAllLogs() {
        List<AuditLog> logs = new ArrayList<>();
        try {
            SearchResponse<AuditLog> response = elasticsearchClient.search((new SearchRequest.Builder()).build(), AuditLog.class);
            logs = response.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return logs;
    }

    public IndexResponse insertAuditLog(AuditLog log) {
        IndexResponse response = null;
        try {
            IndexRequest<AuditLog> request = IndexRequest.of(i -> i.index(index).id(log.getId()).document(log));
            response = elasticsearchClient.index(request);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            response = new IndexResponse.Builder().result(Result.NoOp).build();
        }
        return response;
    }

    public SearchHits<AuditLog> searchHits(AuditLogSearchCriteria criteria, Pageable page) {
        CriteriaQuery query = buildSearchQuery(criteria);
        query.setPageable(page);

        return esTemplate.search(query, AuditLog.class);
    }

    public SearchPage<AuditLog> searchPage(AuditLogSearchCriteria criteria, Pageable page) {
        return SearchHitSupport.searchPageFor(searchHits(criteria, page), page);
    }

    private List<Query> prepareQueryList(AuditLogSearchCriteria criteria) {
        Map<String, FieldValue> conditionMap = new HashMap<>();
        if (criteria.getId() != null) {
            conditionMap.put(ID, FieldValue.of(criteria.getId()));
        }
        if (criteria.getAuthorId() != null) {
            conditionMap.put(AUTHOR_ID, FieldValue.of(criteria.getAuthorId()));
        }
        if (criteria.getCategory() != null) {
            conditionMap.put(CATEGORY, FieldValue.of(criteria.getCategory().name()));
        }
        if (criteria.getAction() != null) {
            conditionMap.put(ACTION, FieldValue.of(criteria.getAction().name()));
        }
        if (criteria.getQuery() != null) {
            conditionMap.put(MESSAGE, FieldValue.of(criteria.getQuery()));
        }

        return conditionMap.entrySet()
                .stream()
                .filter(entry -> !ObjectUtils.isEmpty(entry.getValue()))
                .map(entry -> QueryBuilderUtils.termQuery(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private CriteriaQuery buildSearchQuery(AuditLogSearchCriteria criteria) {
        var cq = new Criteria();
        if (criteria.getId() != null) {
            cq.and(new Criteria(ID).is(criteria.getId()));
        }
        if (criteria.getAuthorId() != null) {
            cq.and(new Criteria(AUTHOR_ID).is(criteria.getAuthorId()));
        }
        if (criteria.getCategory() != null) {
            cq.and(new Criteria(CATEGORY).is(criteria.getCategory()));
        }
        if (criteria.getAction() != null) {
            cq.and(new Criteria(ACTION).is(criteria.getAction()));
        }
        if (criteria.getQuery() != null) {
            cq.and(new Criteria(MESSAGE).contains(criteria.getQuery()));
        }
        return new CriteriaQuery(cq);
    }

}
