package iq.earthlink.social.personservice.security.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Entity
@Data
public class Authority implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authority_seq_gen")
  @SequenceGenerator(name = "authority_seq_gen", sequenceName = "authority_seq_gen", allocationSize = 1)
  private Long id;

  @NotEmpty
  @Column(unique = true, nullable = false)
  private String code;
}
