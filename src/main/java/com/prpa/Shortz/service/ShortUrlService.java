package com.prpa.Shortz.service;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
import com.prpa.Shortz.model.form.ShortUrlForm;
import com.prpa.Shortz.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;

    @Autowired
    public ShortUrlService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    public Page<ShortUrl> findAll(Integer pageNumber, Integer pageSize) {
        return shortUrlRepository.findAll(Pageable.ofSize(pageSize).withPage(pageNumber));
    }

    public Page<ShortUrl> findAll(Integer pageNumber, Integer pageSize, Specification<ShortUrl> search) {
        return shortUrlRepository.findAll(search, Pageable.ofSize(pageSize).withPage(pageNumber));
    }

    public boolean deleteById(Long shortUrlId) {
        Objects.requireNonNull(shortUrlId, "The shorturl id must not be null.");

        final boolean exists = shortUrlRepository.existsById(shortUrlId);
        shortUrlRepository.deleteById(shortUrlId);
        return exists;
    }

    public void save(ShortUrlForm form, ShortzUser owner) {
        final ShortUrl newShortUrl = ShortUrl.builder()
                .uri(form.getUri())
                .owner(owner)
                .slug(form.getSlug())
                .creationTimestamp(Instant.now())
                .hit(0)
                .build();
        shortUrlRepository.save(newShortUrl);
    }

    public boolean existsBySlug(String slug) {
        if (slug.trim().isBlank()) return false;
        return shortUrlRepository.existsBySlug(slug);
    }

    public boolean isUrlCreationOverLimit(ShortzUser owner, Integer limit) {
        if (limit.equals(ShortzUser.UNLIMITED_URL_COUNT)) return false;
        return shortUrlRepository.countByOwner(owner) >= limit;
    }

    public Optional<ShortUrl> findUrlBySlug(String slug) {
        return shortUrlRepository.findBySlug(slug);
    }

    public void onRedirect(ShortUrl redirectShortUrl) {
        Optional<ShortUrl> uriToUpdate = shortUrlRepository.findById(redirectShortUrl.getId());
        if (uriToUpdate.isEmpty()) return;

        ShortUrl shortUrl = uriToUpdate.get();
        shortUrl.setHit(shortUrl.getHit() + 1);
        shortUrlRepository.save(shortUrl);
    }

    public Page<ShortUrlDTO> pageToDTO(Page<ShortUrl> uris) {
        return uris.map(shortUrl -> ShortUrlDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .creationTimestamp(shortUrl.getCreationTimestamp())
                        .owner(shortUrl.getOwner().getUsername())
                        .hit(shortUrl.getHit())
                        .slug(shortUrl.getSlug())
                        .uri(URI.create(shortUrl.getUri()))
                .build());
    }
}