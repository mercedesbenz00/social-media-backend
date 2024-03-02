package iq.earthlink.social.personservice.service;

import iq.earthlink.social.personservice.person.impl.repository.CityRepo;
import iq.earthlink.social.personservice.person.model.City;
import iq.earthlink.social.personservice.person.model.CityModel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.firstNonEmpty;

@Service
@Slf4j
@NoArgsConstructor
public class CityService {

    private CityRepo cityRepo;

    private static final String DEFAULT_LANGUAGE = "ar";

    @Autowired
    public CityService(CityRepo cityRepo) {
        this.cityRepo = cityRepo;
    }

    @Transactional
    public List<CityModel> getCities(
        @Nullable String locale,
        @Nullable String query) {

        return cityRepo.findCities(firstNonEmpty(locale, DEFAULT_LANGUAGE), query);
    }

    public Optional<City> findById(long cityId) {
        return cityRepo.findById(cityId);
    }
}
