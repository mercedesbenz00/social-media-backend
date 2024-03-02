package iq.earthlink.social.personservice.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq_gen")
  @SequenceGenerator(name = "role_seq_gen", sequenceName = "role_seq_gen", allocationSize = 1)
  private Long id;

  @NotEmpty
  @Column(unique = true, nullable = false)
  private String code;
}
