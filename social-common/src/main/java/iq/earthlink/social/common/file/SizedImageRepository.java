package iq.earthlink.social.common.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;

public interface SizedImageRepository extends JpaRepository<SizedImage, Long> {

    @Modifying
    void deleteAllByIdIn(Collection<Long> ids);
}
