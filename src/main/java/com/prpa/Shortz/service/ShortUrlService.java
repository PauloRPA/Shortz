package com.prpa.Shortz.service;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
import com.prpa.Shortz.model.form.ShortUrlForm;
import com.prpa.Shortz.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@Service
public class ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;

    @Autowired
    public ShortUrlService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    public Page<ShortUrlDTO> findAllUrlsByUserToDTO(Integer pageNumber, Integer pageSize, ShortzUser user) {
        Page<ShortUrl> all = shortUrlRepository.findAllByOwner(Pageable.ofSize(pageSize).withPage(pageNumber), user);
        return all.map(shortUrl -> ShortUrlDTO.builder()
                .id(String.valueOf(shortUrl.getId()))
                .creationTimestamp(shortUrl.getCreationTimestamp())
                .owner(shortUrl.getOwner().getUsername())
                .hit(shortUrl.getHit())
                .slug(shortUrl.getSlug())
                .url(URI.create(shortUrl.getUrl()))
                .build());
    }

    public Page<ShortUrlDTO> findAllToDTO(Integer pageNumber, Integer pageSize) {
        Page<ShortUrl> all = shortUrlRepository.findAll(Pageable.ofSize(pageSize).withPage(pageNumber));
        return all.map(shortUrl -> ShortUrlDTO.builder()
                .id(String.valueOf(shortUrl.getId()))
                .creationTimestamp(shortUrl.getCreationTimestamp())
                .owner(shortUrl.getOwner().getUsername())
                .hit(shortUrl.getHit())
                .slug(shortUrl.getSlug())
                .url(URI.create(shortUrl.getUrl()))
                .build());
    }

    public boolean deleteById(Long shortUrlId) {
        Objects.requireNonNull(shortUrlId, "The shorturl id must not be null.");

        final boolean exists = shortUrlRepository.existsById(shortUrlId);
        shortUrlRepository.deleteById(shortUrlId);
        return exists;
    }

    public void save(URI uri, ShortUrlForm form, ShortzUser owner) {
        final ShortUrl newShortUrl = ShortUrl.builder()
                .url(uri.toString())
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

    public boolean isUrlCountOverLimit(ShortzUser owner, Integer limit) {
        if (limit.equals(ShortzUser.UNLIMITED_URL_COUNT)) return false;
        return shortUrlRepository.countByOwner(owner) >= limit;
    }
}
