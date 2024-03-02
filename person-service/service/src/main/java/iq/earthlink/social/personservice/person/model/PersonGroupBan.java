package iq.earthlink.social.personservice.person.model;

import io.swagger.annotations.ApiModelProperty;
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
@Table(indexes = {
        @Index(name = "person_group_ban_expired_at_idx", columnList = "expiredAt"),
        @Index(name = "person_group_ban_unique_idx", columnList = "author_id,banned_person_id,userGroupId", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class PersonGroupBan {

    @Id
    @ApiModelProperty("Id of person ban")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_group_ban_seq_gen")
    @SequenceGenerator(name = "person_group_ban_seq_gen", sequenceName = "person_group_ban_seq_gen", allocationSize = 1)
    private Long id;

    @NotNull
    @CreatedDate
    private Date createdAt;

    @NotNull
    private Long userGroupId;

    @ManyToOne
    @NotNull
    private Person author;

    @ManyToOne
    @NotNull
    private Person bannedPerson;

    @NotNull
    private Date expiredAt;

    @Length(max = 1000)
    @Column(length = 1000)
    private String reason;

    private Long reasonId;
}