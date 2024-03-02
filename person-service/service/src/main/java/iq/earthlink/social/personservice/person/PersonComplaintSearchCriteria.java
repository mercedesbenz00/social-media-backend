package iq.earthlink.social.personservice.person;

import iq.earthlink.social.personservice.dto.PersonDTO;
import iq.earthlink.social.personservice.dto.PersonStatus;
import iq.earthlink.social.personservice.person.model.PersonComplaint;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class PersonComplaintSearchCriteria {
  private String query;
  private PersonDTO currentUser;
  private Long[] personIds;
  PersonComplaint.PersonComplaintState complaintState;
  PersonStatus personStatus;
  @DateTimeFormat(pattern="yyyy-MM-dd")
  LocalDate fromDate;
  @DateTimeFormat(pattern="yyyy-MM-dd")
  LocalDate toDate;
  @Builder.Default
  private Double similarityThreshold = 0.2;
}
