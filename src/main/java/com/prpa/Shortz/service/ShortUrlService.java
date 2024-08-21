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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
public class ShortUrlService {

    @Value("${default.urlshortener.dictionary}")
    private String DICTIONARY;

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
                .uri(URI.create(shortUrl.getUri()))
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
                .uri(URI.create(shortUrl.getUri()))
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
                .uri(uri.toString())
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
}

    public Set<String> filterInvalidCharsFromSlug(String slug) {
        final Set<String> validChars = DICTIONARY.chars()
                .mapToObj(c -> (char) c)
                .map(Object::toString)
                .collect(Collectors.toSet());

        return slug.chars()
                .mapToObj(c -> (char) c)
                .map(Object::toString)
                .filter(not(validChars::contains))
                .collect(Collectors.toSet());
    }

}



