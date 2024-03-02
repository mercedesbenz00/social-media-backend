package iq.earthlink.social.personservice.util;

import iq.earthlink.social.common.util.Code;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.exception.NotUniqueException;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.service.CityService;
import lombok.NoArgsConstructor;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static iq.earthlink.social.personservice.util.Constants.DOT;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;

@Component
@NoArgsConstructor
public class ProfileUtil {

    private CommonProperties commonProperties;
    private CityService cityService;


    @Autowired
    public ProfileUtil(
            CommonProperties commonProperties,
            CityService cityService
    ) {
        this.commonProperties = commonProperties;
        this.cityService = cityService;
    }

    public void checkDisplayName(String displayName) {
        if (isNotBlank(displayName) && commonProperties.getDefaultDisplayName().equalsIgnoreCase(displayName)) {
            throw new ForbiddenException("error.display.name.cannot.be", commonProperties.getDefaultDisplayName());
        }
    }

    public void checkCity(Long cityId) {
        if (cityId != null && (cityService.findById(cityId).isEmpty())) {
            throw new BadRequestException("error.city.not.found", cityId);
        }
    }

    public void checkAge(Date dateOfBirth) {
        if (dateOfBirth != null) {
            Date now = new Date(System.currentTimeMillis());
            int minAge = commonProperties.getMinAge();
            if (minAge > 0) {
                int age = (int) ChronoUnit.YEARS.between(convertToLocalDate(dateOfBirth), convertToLocalDate(now));
                if (age < minAge) {
                    throw new BadRequestException("error.user.age.not.valid", minAge);
                }
            }
        }
    }

    public void checkBio(String bio) {
        if (bio != null && bio.length() > commonProperties.getBioMaxLength()) {
            throw new BadRequestException("error.bio.length.exceeded", commonProperties.getBioMaxLength());
        }
    }

    public void generateUsername(@Nonnull PersonRepository personRepository, @Nonnull Person person, int attempt) {
        // Generate person username, ensure uniqueness:
        String username;
        if (isNotBlank(person.getFirstName()) && isNotBlank(person.getLastName())) {
            username = deleteWhitespace(
                    joinWith(DOT,
                            person.getFirstName(),
                            person.getLastName(),
                            Code.next(3)
                    ));
        } else {
            String[] emailParts = person.getEmail().split("@");
            Faker faker = new Faker();
            username = deleteWhitespace(
                    joinWith(DOT,
                            emailParts[0],
                            faker.code().isbn10(),
                            Code.next(3)
                    ));
        }

        try {
            if (personRepository.findByUsernameIgnoreCase(username).isPresent()) {
                throw new NotUniqueException("error.person.username.already.exists");
            }
            person.setUsername(username);
        } catch (NotUniqueException ex) {
            if (attempt < commonProperties.getUsernameGeneratorMaxAttempts()) {
                // If username is not unique, generate it again:
                generateUsername(personRepository, person, ++attempt);
            } else {
                throw new NotUniqueException("Generated username is not unique");
            }
        }
    }

    public void generateDisplayName(@Nonnull Person person) {
        // Generate person display name if it is not set:
        if (isNotBlank(person.getFirstName()) && isNotBlank(person.getLastName())) {
            person.setDisplayName(normalizeSpace(format("%s %s", person.getFirstName(), person.getLastName())));
        } else {
            String[] emailParts = person.getEmail().split("@");
            person.setDisplayName(emailParts[0]);
        }
    }

    public boolean isPersonInfoProvided(@Nonnull Person person) {
        return isNotBlank(person.getFirstName()) &&
                isNotBlank(person.getLastName());
    }

    private LocalDate convertToLocalDate(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
