package com.prpa.Shortz.model.validation;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.form.ShortUrlForm;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import com.prpa.Shortz.service.ShortUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
public class ShortUrlFormValidator implements Validator {

    private static final String DEFAULT_SCHEME = "https://";

    private static final Charset URL_ENCODING_CHARSET = StandardCharsets.UTF_8;

    @Value("${default.urlshortener.dictionary}")
    private String DICTIONARY;

    private final UrlShortener urlShortener;

    private final ShortUrlService shortUrlService;

    @Autowired
    public ShortUrlFormValidator(UrlShortener urlShortener, ShortUrlService shortUrlService) {
        this.urlShortener = urlShortener;
        this.shortUrlService = shortUrlService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ShortUrlForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final ShortUrlForm formToValidate = (ShortUrlForm) target;

        Optional<URI> validFormatURI = toValidUriFormat(formToValidate.getUri());
        Optional<String> slug = Optional.ofNullable(formToValidate.getSlug());

        if (validFormatURI.isPresent() && (slug.isEmpty() || slug.get().isBlank())) {
            formToValidate.setUri(validFormatURI.get().toString());
            generateSlugIfEmpty(formToValidate);
            slug = Optional.ofNullable(formToValidate.getSlug());
        }

        validateUri(validFormatURI.orElse(URI.create("")), formToValidate, errors);
        validateSlug(slug.orElse(""), errors);

        ShortzUser owner = ((ShortzUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        if (shortUrlService.isUrlCreationOverLimit(owner, owner.getUrlCreationLimit())) {
            errors.rejectValue("slug", "error.newUriForm.user.limit");
        }

    }

    private void validateSlug(String slug, Errors errors) {
        if (!slug.isBlank() && shortUrlService.existsBySlug(slug)) {
            errors.rejectValue("slug", "slug.exists");
        }

        Set<String> invalidChars = filterInvalidCharsFromSlug(slug);
        if (!invalidChars.isEmpty()) {
            String invalidCharsStr = String.join("", invalidChars);
            errors.rejectValue("slug", "error.slug.character", new String[]{invalidCharsStr}, "The slug contains invalid characters");
        }
    }

    private void validateUri(URI uriAfterValidation, ShortUrlForm formToValidate, Errors errors) {
        final boolean uriBlank = uriAfterValidation.toString().isBlank();

        if (uriBlank && formToValidate.getUri() != null && !formToValidate.getUri().isBlank()) {
            errors.rejectValue("uri", "error.newUriForm.uri.invalid");
        }

        if (!uriBlank && !urlShortener.getSupportedProtocols().contains(uriAfterValidation.getScheme())) {
            errors.rejectValue("uri", "error.newUriForm.uri.unsupported.protocol");
        }
    }

    private void generateSlugIfEmpty(ShortUrlForm toBeValidated) {
        URI uri = URI.create(toBeValidated.getUri());
        String generatedSlug = urlShortener.encodeUri(uri).orElse("");
        toBeValidated.setSlug(generatedSlug);
    }

    public Set<String> filterInvalidCharsFromSlug(String slug) {
        if (slug == null || slug.isBlank()) return Set.of();

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


    public static Optional<URI> toValidUriFormat(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) return Optional.empty();
        uriStr = uriStr.trim();

        try {
            final boolean hasScheme = uriStr.contains("://");
            String schemePrefix = hasScheme ? "" : DEFAULT_SCHEME;
            uriStr = schemePrefix + uriStr;
            URI uri = new URI(uriStr);

            if (!uriStr.contains(".") && !hasScheme) return Optional.empty();
            if (uri.getScheme() == null) return Optional.empty();
            if (uri.getHost() == null) return Optional.empty();
            if (uri.getPath() == null) return Optional.empty();
            if (!uri.getHost().matches("[a-zA-z0-9-.]+")) return Optional.empty();

            if (!hasScheme) new URL(uriStr);

            return Optional.of(uri);
        } catch (URISyntaxException | MalformedURLException e) {
            return Optional.empty();
        }
    }


}
