package iq.earthlink.social.groupservice.category.dto;

import iq.earthlink.social.classes.enumeration.TimeInterval;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryStats {

  private long allCategoriesCount;
  private long newCategoriesCount;
  private List<CreatedCategories> createdCategories;
  private TimeInterval timeInterval;
  private Timestamp fromDate;
}
