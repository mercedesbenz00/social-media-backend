package iq.earthlink.social.groupservice.person;

import iq.earthlink.social.groupservice.person.dto.PersonDTO;

import java.util.List;
import java.util.UUID;

public interface PersonManager {

    List<PersonDTO> getByDisplayNameWithExclusion(String query, List<Long> excludePersonIds, int queryLimit);

    PersonDTO getPersonByUuid(UUID personUuid);

    PersonDTO getPersonByPersonId(Long personId);

    List<PersonDTO> getPersonsByPersonIdIn(List<Long> personIds);

    Long getCount();

    void savePerson(PersonDTO personDTO);

    void updatePerson(PersonDTO personDTO);
}
