package iq.earthlink.social.personservice.person.model;


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
@Table(
        indexes = {
                @Index(name = "jwt_token_idx", columnList = "token", unique = true)
        }
)
public class JWTBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_seq_gen")
    @SequenceGenerator(name = "token_seq_gen", sequenceName = "token_seq_gen", allocationSize = 1)
    private Long id;

    @Length(max = 4000)
    @Column(length = 4000)
    private String token;

    @NotNull
    @CreatedDate
    private Date createdAt;
}
