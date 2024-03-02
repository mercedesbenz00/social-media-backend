package iq.earthlink.social.personservice.person.impl;

import iq.earthlink.social.classes.enumeration.MediaFileType;
import iq.earthlink.social.common.audit.AuditMarker;
import iq.earthlink.social.common.audit.AuditMessage;
import iq.earthlink.social.common.audit.EventAction;
import iq.earthlink.social.common.file.MediaFile;
import iq.earthlink.social.common.file.MediaFileRepository;
import iq.earthlink.social.common.file.MediaFileTranscodedRepository;
import iq.earthlink.social.common.file.SizedImageRepository;
import iq.earthlink.social.common.file.rest.AbstractMediaService;
import iq.earthlink.social.common.filestorage.FileStorageProvider;
import iq.earthlink.social.common.filestorage.StorageType;
import iq.earthlink.social.common.filestorage.minio.MinioProperties;
import iq.earthlink.social.common.util.CommonConstants;
import iq.earthlink.social.common.util.FileUtil;
import iq.earthlink.social.exception.BadRequestException;
import iq.earthlink.social.exception.ForbiddenException;
import iq.earthlink.social.personservice.person.PersonMediaService;
import iq.earthlink.social.personservice.person.impl.repository.PersonRepository;
import iq.earthlink.social.personservice.person.model.Person;
import iq.earthlink.social.personservice.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Optional;

import static iq.earthlink.social.common.util.CommonConstants.MEDIA_TO_PROCESS;
import static iq.earthlink.social.common.util.CommonConstants.PERSON_SERVICE;
import static iq.earthlink.social.common.util.Preconditions.checkNotNull;

@Service
@Slf4j
public class DefaultPersonMediaService extends AbstractMediaService implements PersonMediaService {

    private static final Logger LOG = LogManager.getLogger(DefaultPersonMediaService.class);

    private final PersonRepository personRepository;
    private final KafkaProducerService kafkaProducerService;

    public DefaultPersonMediaService(
            MediaFileRepository fileRepository,
            MediaFileTranscodedRepository fileTranscodedRepository,
            PersonRepository personRepository,
            FileStorageProvider fileStorageProvider,
            MinioProperties minioProperties, KafkaProducerService kafkaProducerService,
            SizedImageRepository sizedImageRepository) {
        super(fileRepository, fileTranscodedRepository, sizedImageRepository, fileStorageProvider, minioProperties);
        this.personRepository = personRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @NotNull
    @Override
    @Transactional
    public MediaFile uploadAvatar(Person person, MultipartFile file, Person currentUser) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, "person");
        checkPermissions(person.getId(), currentUser);
        checkContentType(file);

        String logMessage = String.format("Uploading new avatar for the person \"%s\"", person.getUsername());

        log.info(logMessage);
        LOG.info(AuditMarker.getMarker(), AuditMessage.create(EventAction.MUTE, logMessage, currentUser.getId(), person.getId()));

        removeFiles(person.getId(), MediaFileType.AVATAR);
        person.setAvatar(uploadFile(person.getId(), MediaFileType.AVATAR, file));
        personRepository.save(person);
        kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(person.getAvatar(), PERSON_SERVICE));
        kafkaProducerService.sendMessage(CommonConstants.PERSON_UPDATED, person);
        return person.getAvatar();
    }

    @Nonnull
    @Override
    public Optional<MediaFile> findAvatar(Long personId) {
        return findFile(personId, MediaFileType.AVATAR);
    }

    @Override
    public InputStream downloadAvatar(MediaFile file) {
        if (file.getFileType() != MediaFileType.AVATAR) {
            throw new IllegalStateException();
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeAvatar(Person person, Person currentUser) {
        checkPermissions(person.getId(), currentUser);
        removeFiles(person.getId(), MediaFileType.AVATAR);
        person.setAvatar(null);
        kafkaProducerService.sendMessage(CommonConstants.PERSON_UPDATED, person);
        personRepository.save(person);
    }

    @NotNull
    @Override
    @Transactional
    public MediaFile uploadCover(Person person, MultipartFile file, Person currentUser) {
        checkNotNull(person, ERROR_CHECK_NOT_NULL, "person");
        checkPermissions(person.getId(), currentUser);
        checkContentType(file);

        log.info(String.format("Uploading new cover for the person \"%s\"", person.getUsername()));

        removeFiles(person.getId(), MediaFileType.COVER);
        person.setCover(uploadFile(person.getId(), MediaFileType.COVER, file));
        personRepository.save(person);
        kafkaProducerService.sendMessage(MEDIA_TO_PROCESS, getImageProcessRequest(person.getCover(), PERSON_SERVICE));
        return person.getCover();
    }

    @NotNull
    @Override
    public Optional<MediaFile> findCover(Long personId) {
        return findFile(personId, MediaFileType.COVER);
    }

    @Override
    public InputStream downloadCover(MediaFile file) {
        if (file.getFileType() != MediaFileType.COVER) {
            throw new IllegalStateException();
        }
        return downloadFile(file);
    }

    @Override
    @Transactional
    public void removeCover(Person person, Person currentUser) {
        checkPermissions(person.getId(), currentUser);
        removeFiles(person.getId(), MediaFileType.COVER);
        person.setCover(null);
        personRepository.save(person);
    }

    @Override
    public String getFileURL(MediaFile file) {
        return super.getFileUrl(file);
    }

    @Override
    public String getFileURL(StorageType storageType, String path) {
        return super.getFileUrl(storageType, path);
    }

    private void checkPermissions(Long personIdToChange, Person currentUser) {
        if (!currentUser.isAdmin() && !currentUser.getId().equals(personIdToChange))
            throw new ForbiddenException("error.person.can.not.modify.avatar.or.cover");
    }

    private static void checkContentType(MultipartFile file) {
        if (!FileUtil.isImage(file)) {
            throw new BadRequestException("invalid.image.file");
        }
    }
}
