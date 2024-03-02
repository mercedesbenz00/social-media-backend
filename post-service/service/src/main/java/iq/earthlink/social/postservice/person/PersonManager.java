package iq.earthlink.social.postservice.person;

import iq.earthlink.social.postservice.person.dto.PersonDTO;

import java.util.List;
import java.util.UUID;

public interface PersonManager {
    PersonDTO getPersonByUuid(UUID personUuid);

    List<PersonDTO> getPersonsByUuids(List<UUID> personUuids);

    Long getCount();

    void updateAllPostAuthorUUID();

    void savePerson(PersonDTO personDTO);

    void updatePerson(PersonDTO personDTO);

    List<PersonDTO> getPersonsByIds(List<Long> personIds);
}
