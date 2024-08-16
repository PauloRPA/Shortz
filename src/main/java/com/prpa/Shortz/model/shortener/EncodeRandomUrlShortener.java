package com.prpa.Shortz.model.shortener;

import com.prpa.Shortz.model.shortener.contract.StringEncoder;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Retorna fragmentos de um hash a partir de uma url respeitando um dicionário. O tamanho do fragmento é descrito
 * pelo windowSize e pode ser aumentado ao codificar uma url. Caso o fragmento seja maior que a url
 * codificada, caracteres serão gerados aleatoriamente respeitando o dicionário.
 * Os fragmentos de hash retornados por essa classe podem ser usados como slugs de uma página.
 */
public class EncodeRandomUrlShortener implements UrlShortener {

    private static final String DEFAULT_DICTIONARY = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";
    private static final Set<String> DEFAULT_SUPPORTED_PROTOCOLS = Set.of(); // All protocols supported
    public static final Set<String> WEB_PROTOCOLS = Set.of("http", "https");

    private final Integer windowSize;
    private final Random random;
    private final StringEncoder stringEncoder;
    private final Set<String> supportedProtocols;

    @Getter @Setter
    private String dictionary;

    public EncodeRandomUrlShortener(StringEncoder stringEncoder, Integer windowSize) {
        this(stringEncoder, windowSize, DEFAULT_DICTIONARY, new Random(), DEFAULT_SUPPORTED_PROTOCOLS);
    }

    public EncodeRandomUrlShortener(StringEncoder stringEncoder, Integer windowSize, Set<String> supportedProtocols) {
        this(stringEncoder, windowSize, DEFAULT_DICTIONARY, new Random(), supportedProtocols);
    }

    public EncodeRandomUrlShortener(StringEncoder stringEncoder,
                                    Integer windowSize,
                                    String dictionary,
                                    Random random,
                                    Set<String> supportedProtocols) {

        Objects.requireNonNull(dictionary, "Dictionary must not be null.");
        Objects.requireNonNull(random, "Random must not be null.");
        Objects.requireNonNull(stringEncoder, "stringEncoder must not be null.");
        if (supportedProtocols == null) throw new IllegalArgumentException("Supported protocols must not be null.");
        if (windowSize < 1) throw new IllegalArgumentException("windowSize must not be less than 1.");

        this.windowSize = windowSize;
        this.dictionary = dictionary;
        this.supportedProtocols = supportedProtocols;

        this.stringEncoder = stringEncoder;
        this.random = random;
    }

    private Optional<String> encodeUrlToWindowSize(URI uri, int windowSize) {
        Objects.requireNonNull(uri, "Url must not be null.");
        if (uri.getScheme() == null) return Optional.empty();
        if (uri.getHost() == null) return Optional.empty();
        if (uri.getPath() == null) return Optional.empty();
        if (!supportedProtocols.isEmpty() && !supportedProtocols.contains(uri.getScheme())) return Optional.empty();

        var encodedURI = new StringBuilder(stringEncoder.encode(uri.getHost() + uri.getPath()));

        if (windowSize > encodedURI.length()) {
            final int diff = windowSize - encodedURI.length();
            for (int i = 0; i < diff; i++) {
                encodedURI.append(randomDictionaryChar());
            }
        }

        String validEncodedUrl = replaceInvalidChars(encodedURI, "_");
        return Optional.of(validEncodedUrl.substring(0, windowSize));
    }

    private String replaceInvalidChars(StringBuilder encodedURL, String replacement) {
        final String INVALID_CHARS_REGEX = "[^" + this.dictionary + "]"; // Not valid chars [^valid_chars]
        return encodedURL.toString().replaceAll(INVALID_CHARS_REGEX, replacement);
    }

    private char randomDictionaryChar() {
        return dictionary.charAt(random.nextInt(dictionary.length()));
    }

    @Override
    public Optional<String> encodeUri(URI uri) {
        return encodeUrlToWindowSize(uri, windowSize);
    }

    @Override
    public Optional<String> encodeUri(URI uri, int increaseWindowBy) {
        if (increaseWindowBy < 0) throw new IllegalArgumentException("increaseWindowBy must not be negative.");
        return encodeUrlToWindowSize(uri, windowSize + increaseWindowBy);
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return this.supportedProtocols;
    }

    @Override
    public int getWindowSize() {
        return this.windowSize;
    }

}
