package iq.earthlink.social.personservice.person.model;

import iq.earthlink.social.classes.enumeration.RegistrationState;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.personservice.data.Gender;
import iq.earthlink.social.personservice.security.model.Authority;
import iq.earthlink.social.personservice.security.model.Role;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    indexes = {
        @Index(name = "person_email_idx", columnList = "email", unique = true),
        @Index(name = "person_username_idx", columnList = "username", unique = true),
        @Index(name = "person_generated_username_idx", columnList = "displayName"),
        @Index(name = "person_email_reset_code_idx", columnList = "email, resetCode", unique = true)
    }
)
@ToString(of = {"id", "email"})
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Person implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq_gen")
  @SequenceGenerator(name = "person_seq_gen", sequenceName = "person_seq_gen", allocationSize = 1)
  private Long id;

  @Column(name = "person_uuid", length = 36, nullable = false, updatable = false, unique = true)
  private UUID uuid;

  @Length(max = 255)
  private String username;

  @Length(max = 255)
  private String firstName;

  @Length(max = 255)
  private String lastName;

  @NotNull
  @Column(nullable = false)
  private String displayName;

  private Date birthDate;

  @NotNull
  @CreatedDate
  private Date createdAt;

  @Length(max = 255, min = 3)
  private String email;

  private Long cityId;

  private String password;

  private String language;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private Date deletedDate;

  private Integer resetCode;

  private Date resetCodeExpireAt;

  private String confirmCode;

  @Enumerated(EnumType.STRING)
  private RegistrationState state;

  @Column(columnDefinition = "boolean default false")
  private boolean isConfirmed;

  @Column(columnDefinition = "boolean default false")
  private boolean isVerifiedAccount;

  @Column(columnDefinition = "boolean default false")
  private boolean isRegistrationCompleted;

  @Column(name = "account_non_locked", columnDefinition = "boolean default true")
  private boolean accountNonLocked;

  @Column(name = "failed_attempt")
  private int failedAttempt;

  @Column(name = "lock_time")
  private Date lockTime;

  @Column(columnDefinition = "int8 not null default 0")
  private long followerCount;

  @Column(columnDefinition = "int8 not null default 0")
  private long followingCount;

  @Column(columnDefinition = "int8 not null default 0")
  private long postCount;

  @Column(columnDefinition = "int8 not null default 0")
  private long groupCount;

  @Column(columnDefinition = "int8 not null default 0")
  private long interestCount;

  @ManyToMany(fetch = FetchType.EAGER)
  private Set<Role> personRoles = new HashSet<>();

  @ManyToMany(fetch = FetchType.EAGER)
  private Set<Authority> personAuthorities = new HashSet<>();

  @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
  private MediaFile cover;

  @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
  private MediaFile avatar;

  private String bio;

  @Formula("first_name || ' ' || last_name")
  private String firstLastName;

  @Formula("last_name || ' ' || first_name")
  private String lastFirstName;

  public Set<String> getRoles() {
    return personRoles.stream()
        .map(Role::getCode)
        .collect(Collectors.toSet());
  }

  public Set<String> getAuthorities() {
    return personAuthorities.stream()
        .map(Authority::getCode)
        .collect(Collectors.toSet());
  }

  public void activate() {
    this.deletedDate = null;
  }

  public void deactivate() {
    this.deletedDate = new Date();
  }

  public boolean isActive() {
    return this.deletedDate == null;
  }

  public boolean isAdmin() {
    return getRoles().contains("ADMIN");
  }

}

