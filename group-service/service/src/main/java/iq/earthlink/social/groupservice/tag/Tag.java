package iq.earthlink.social.groupservice.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Tag {

    @Id
    @GeneratedValue
    private Long id;

    @Length(max = 255, min = 1)
    @Column(name = "tag", unique = true)
    @NotNull
    private String tagName;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    private Long authorId;
}
