package iq.earthlink.social.auditservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "#{@elasticIndexName}")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    private String id;
    @Field(type = FieldType.Text, name = "category")
    private String category;
    @Field(type = FieldType.Text, name = "action")
    private String action;
    private Long authorId;
    private Long referenceId;
    private String referenceName;
    private String message;
    private Date eventDate;
}
