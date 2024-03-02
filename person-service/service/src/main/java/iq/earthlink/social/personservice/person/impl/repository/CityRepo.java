package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.City;
import iq.earthlink.social.personservice.person.model.CityModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by ukman on 11/28/18.
 */
@Repository
public interface CityRepo extends JpaRepository<City, Long> {

  @Query("SELECT new iq.earthlink.social.personservice.person.model.CityModel(c.id, coalesce(l.name, c.name) as name) FROM City c " +
          "LEFT JOIN c.localizations l on LOWER(l.locale)=:#{#locale} "
      + "WHERE (?#{#query} IS NULL "
      + "OR LOWER(coalesce(l.name, c.name)) LIKE CONCAT('%', LOWER(?#{#query ?:''}), '%')) "
      + "ORDER BY c.name ASC")
  List<CityModel> findCities(@Param("locale") String locale, @Param("query") String query);
}
