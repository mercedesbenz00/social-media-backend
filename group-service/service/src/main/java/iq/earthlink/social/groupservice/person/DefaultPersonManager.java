package iq.earthlink.social.groupservice.person;

import iq.earthlink.social.classes.data.dto.JsonMediaFile;
import iq.earthlink.social.classes.data.dto.JsonSizedImage;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileState;
import iq.earthlink.social.common.file.SizedImage;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.exception.NotFoundException;
import iq.earthlink.social.groupservice.group.GroupMediaService;
import iq.earthlink.social.groupservice.person.dto.PersonDTO;
import iq.earthlink.social.groupservice.person.model.Person;
import iq.earthlink.social.groupservice.person.repository.PersonRepository;
import iq.earthlink.social.personservice.rest.PersonRestService;
import iq.earthlink.social.security.config.ServerAuthProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Service("personManager")
@Log4j2
@RequiredArgsConstructor
public class DefaultPersonManager implements PersonManager {
    private final PersonRepository personRepository;
    private final PersonRestService personRestService;
    private final GroupMediaService groupMediaService;
    private final ServerAuthProperties authProperties;

    public List<PersonDTO> getByDisplayNameWithExclusion(String query, List<Long> excludePersonIds, int queryLimit) {
        var persons = personRepository.getByDisplayNameWithExclusion(query, excludePersonIds, queryLimit);

        return getPersonDTOS(persons);
    }

    @Override
    public PersonDTO getPersonByUuid(UUID personUuid) {
        var person = personRepository.getPersonByUuid(personUuid)
                .orElseThrow(() -> new NotFoundException("error.person.not.found.byId", personUuid));
        return getPersonDTO(person);
    }

    @Override
    public PersonDTO getPersonByPersonId(Long personId) {
        var person = personRepository.getPersonByPersonId(personId);
        if (person.isEmpty()) {
            try {
                var jsonPersonProfile = personRestService.getPersonByIdInternal(authProperties.getApiAuthHeader(), personId);
                if (ObjectUtils.isEmpty(jsonPersonProfile)) {
                    throw new NotFoundException("error.person.not.found.byId", personId);
                }
                PersonDTO personDTO = PersonDTO
                        .builder()
                        .uuid(jsonPersonProfile.getUuid())
                        .personId(jsonPersonProfile.getId())
                        .displayName(jsonPersonProfile.getDisplayName())
                        .avatar(jsonPersonProfile.getAvatar())
                        .roles(jsonPersonProfile.getRoles())
                        .isVerifiedAccount(jsonPersonProfile.getIsVerifiedAccount())
                        .createdAt(jsonPersonProfile.getCreatedAt())
                        .build();
                personRepository.save(generatePersonEntity(personDTO));
                return personDTO;
            } catch (Exception ex) {
                log.error("Couldn't fetch person by person id: {}, reason: {}", personId, ex.getMessage());
                throw new NotFoundException("error.person.not.found.byId", personId);
            }
        }

        return getPersonDTO(person.get());
    }

    @Override
    public List<PersonDTO> getPersonsByPersonIdIn(List<Long> personIds) {
        var persons = personRepository.getPersonByPersonIdIn(personIds);

        return getPersonDTOS(persons);
    }

    @Override
    public void savePerson(PersonDTO personDTO) {
        var existingPerson = personRepository.getPersonByUuid(personDTO.getUuid());
        if (existingPerson.isEmpty()) {
            var person = generatePersonEntity(personDTO);
            if (person != null) {
                personRepository.save(person);
            }
        }
    }

    @Override
    public void updatePerson(PersonDTO personDTO) {
        var person = personRepository.getPersonByUuid(personDTO.getUuid())
                .orElseThrow(() -> new NotFoundException("error.person.not.found.byId", personDTO.getUuid()));
        person.setDisplayName(firstNonNull(personDTO.getDisplayName(), person.getDisplayName()));
        person.setVerifiedAccount(firstNonNull(personDTO.isVerifiedAccount(), person.isVerifiedAccount()));
        if (personDTO.getAvatar() != null && personDTO.getAvatar().getPath() != null) {
            var avatar = generateAvatarEntity(personDTO.getAvatar());
            avatar.setOwnerId(personDTO.getPersonId());
            person.setAvatar(avatar);
        } else {
            person.setAvatar(null);
        }
        personRepository.save(person);
    }

    private Person generatePersonEntity(PersonDTO personDTO) {
        if (personDTO != null) {
            log.info("generating new person entity with uuid: {}", personDTO.getUuid());
            Person person = Person
                    .builder()
                    .personId(personDTO.getPersonId())
                    .uuid(personDTO.getUuid())
                    .personRoles(personDTO.getRoles())
                    .isVerifiedAccount(personDTO.isVerifiedAccount())
                    .displayName(personDTO.getDisplayName())
                    .createdAt(personDTO.getCreatedAt())
                    .build();
            if (personDTO.getAvatar() != null && personDTO.getAvatar().getPath() != null) {
                var avatar = generateAvatarEntity(personDTO.getAvatar());
                avatar.setOwnerId(personDTO.getPersonId());
                person.setAvatar(avatar);
            }
            return person;
        }
        return new Person();
    }

    private MediaFile generateAvatarEntity(@NonNull JsonMediaFile jsonMediaFile) {
        var avatar = MediaFile
                .builder()
                .fileType(jsonMediaFile.getFileType())
                .mimeType(jsonMediaFile.getMimeType())
                .path(jsonMediaFile.getPath())
                .storageType(StorageType.MINIO)
                .size(jsonMediaFile.getSize())
                .state(MediaFileState.CREATED)
                .build();
        if (!CollectionUtils.isEmpty(jsonMediaFile.getSizedImages())) {
            var sizedImages = jsonMediaFile.getSizedImages();
            Set<SizedImage> sizedImageSet = new HashSet<>();
            for (List<JsonSizedImage> sizedImageList : sizedImages.values()) {
                sizedImageSet.addAll(sizedImageList.stream()
                        .filter(sizedImage -> sizedImage.getPath() != null)
                        .map(sizedImage -> SizedImage
                                .builder()
                                .mimeType(sizedImage.getMimeType())
                                .size(sizedImage.getSize())
                                .path(sizedImage.getPath())
                                .imageSizeType(sizedImage.getImageSizeType())
                                .storageType(StorageType.MINIO)
                                .createdAt(sizedImage.getCreatedAt())
                                .build())
                        .toList());
            }
            avatar.setSizedImages(sizedImageSet);
        }
        return avatar;
    }

    //for migration purpose only
    @Override
    public Long getCount() {
        return personRepository.count();
    }

    private List<PersonDTO> getPersonDTOS(List<Person> persons) {
        if (CollectionUtils.isEmpty(persons)) {
            return List.of();
        }
        return persons.stream().map(this::getPersonDTO).toList();
    }

    private PersonDTO getPersonDTO(Person person) {
        var personDTO = PersonDTO
                .builder()
                .personId(person.getPersonId())
                .uuid(person.getUuid())
                .displayName(person.getDisplayName())
                .isVerifiedAccount(person.isVerifiedAccount())
                .roles(person.getPersonRoles())
                .build();
        if (person.getAvatar() != null) {
            var avatar = person.getAvatar();
            var listJsonMediaFiles = groupMediaService.convertMediaFilesToJsonMediaFiles(List.of(avatar));
            personDTO.setAvatar(listJsonMediaFiles.get(0));
        }
        return personDTO;
    }
}
