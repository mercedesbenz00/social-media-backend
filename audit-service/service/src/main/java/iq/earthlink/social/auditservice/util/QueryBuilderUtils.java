package iq.earthlink.social.auditservice.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;

public interface QueryBuilderUtils {

    static Query termQuery(String field, FieldValue value) {
        QueryVariant queryVariant;
        if (FieldValue.Kind.String.equals(value._kind())) {
            queryVariant = new TermQuery.Builder()
                    .caseInsensitive(true)
                    .field(field).value(value).build();
        } else {
            queryVariant = new TermQuery.Builder()
                    .field(field).value(value).build();
        }
        return new Query(queryVariant);
    }
}
