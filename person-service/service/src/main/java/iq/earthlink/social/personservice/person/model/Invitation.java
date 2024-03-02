package iq.earthlink.social.personservice.person.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    private String id;

    @NotNull
    private Date createdAt;

    @NotNull
    @ManyToOne
    private Person inviter;

    @NotNull
    private String firstName;
    @NotNull
    private String lastName;
    @NotNull
    private String email;

}
