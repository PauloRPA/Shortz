package com.prpa.Shortz.repository;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long>, JpaSpecificationExecutor<ShortUrl> {

    Page<ShortUrl> findByOwner(ShortzUser owner, Pageable pageable);

    boolean existsBySlug(String slug);

    Integer countByOwner(ShortzUser owner);

    Optional<ShortUrl> findBySlug(String slug);
}
