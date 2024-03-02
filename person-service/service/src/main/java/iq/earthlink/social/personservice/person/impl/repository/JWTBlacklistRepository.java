package iq.earthlink.social.personservice.person.impl.repository;

import iq.earthlink.social.personservice.person.model.JWTBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface JWTBlacklistRepository extends JpaRepository<JWTBlacklist,Long> {

    @Query("SELECT j FROM JWTBlacklist j WHERE token = :token")
    JWTBlacklist findByToken(String token);

    @Modifying
    @Query("DELETE FROM JWTBlacklist where created_at < :deleteBefore")
    void deleteByCreatedDate(Date deleteBefore);

}
