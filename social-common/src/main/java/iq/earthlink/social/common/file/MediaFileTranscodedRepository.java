package iq.earthlink.social.common.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaFileTranscodedRepository extends JpaRepository<MediaFileTranscoded, Long> {

  List<MediaFileTranscoded> findByIdIn(List<Long> ids);

}
