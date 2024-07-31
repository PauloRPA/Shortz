package com.prpa.Shortz.unit.model;

import com.prpa.Shortz.model.shortener.DigestToBase64StringEncoder;
import com.prpa.Shortz.model.shortener.EncodeRandomUrlShortener;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

import static com.prpa.Shortz.model.shortener.EncodeRandomUrlShortener.WEB_PROTOCOLS;
import static org.assertj.core.api.Assertions.assertThat;

public class EncodeRandomUrlShortenerTest {

    private final int INITIAL_WINDOW_SIZE = 6;
    private final URI VALID_URL = new URI("https://www.youtube.com/?video=id");
    private final URI FTP_PROTOCOL_URL = new URI("ftp://www.youtube.com/");

    private UrlShortener encodeUrlShortener;

    public EncodeRandomUrlShortenerTest() throws URISyntaxException {}

    @BeforeEach
    void setUp() {
        encodeUrlShortener = new EncodeRandomUrlShortener(new DigestToBase64StringEncoder(), INITIAL_WINDOW_SIZE);
    }

    @Test
    @DisplayName("Quando encurtar uma url deve retornar um hash no tamanho adequado.")
    public void whenShortUrl_shouldEncodeAndReturnHashRequiredWindowSize() throws NoSuchAlgorithmException {
        final int initialWindow = encodeUrlShortener.getWindowSize();
        Optional<String> encodedUrlInitialWindow = encodeUrlShortener.encodeUrl(VALID_URL);
        Optional<String> encodedUrlIncreaseWindow = encodeUrlShortener.encodeUrl(VALID_URL, 1);

        assertThat(encodedUrlInitialWindow.isPresent()).isTrue();
        assertThat(encodedUrlInitialWindow.get().length()).isEqualTo(initialWindow);

        assertThat(encodedUrlIncreaseWindow.isPresent()).isTrue();
        assertThat(encodedUrlIncreaseWindow.get().length()).isEqualTo(initialWindow + 1);
        // Mesmo resultado do encoding com janela menor, só que neste caso tem um char a mais à direita.
        assertThat(encodedUrlInitialWindow.get()).isEqualTo(encodedUrlIncreaseWindow.get().substring(0, initialWindow));
    }

    @Test
    @DisplayName("Retorna um hash valido quando qualquer protocolo é permitido.")
    public void whenShortUrlValidProtocol_shouldReturnHash() throws NoSuchAlgorithmException {
        var urlShortener = new EncodeRandomUrlShortener(new DigestToBase64StringEncoder(), INITIAL_WINDOW_SIZE);
        Optional<String> encodedUrl = urlShortener.encodeUrl(FTP_PROTOCOL_URL);

        assertThat(encodedUrl.isPresent()).isTrue();
    }

    @Test
    @DisplayName("Retorna vazio quando um protocolo invalido é informado.")
    public void whenShortUrlInvalidProtocol_shouldReturnEmpty() throws NoSuchAlgorithmException {
        var urlShortener = new EncodeRandomUrlShortener(new DigestToBase64StringEncoder(), INITIAL_WINDOW_SIZE, WEB_PROTOCOLS);
        Optional<String> encodedUrl = urlShortener.encodeUrl(FTP_PROTOCOL_URL);

        assertThat(encodedUrl.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("A janela informada é maior que o tamanho da str codificada, chars aleatórios devem ser gerados.")
    public void whenShortUrlWindowLargerOutOfBounds_shouldGenerateRandomChars() throws NoSuchAlgorithmException {
        final int NUMBER_OF_SAMPLES = 10;
        final int OUT_OF_BOUNDS_WINDOW_SIZE = 99;
        final String[] samples = new String[NUMBER_OF_SAMPLES];

        // A janela informada é maior que o tamanho da str codificada (idêntica a str de entrada)
        UrlShortener urlShortener = new EncodeRandomUrlShortener(s -> s, OUT_OF_BOUNDS_WINDOW_SIZE);

        for (int i = 0; i < NUMBER_OF_SAMPLES; i++) {
            Optional<String> encodedUrl = urlShortener.encodeUrl(VALID_URL);
            assertThat(encodedUrl.isPresent()).isTrue();
            assertThat(encodedUrl.get().length()).isEqualTo(urlShortener.getWindowSize());
            samples[i] = encodedUrl.get();
        }

        // Todas as amostras devem ser diferentes entre si
        assertThat(Arrays.stream(samples).distinct().count()).isEqualTo(NUMBER_OF_SAMPLES);
    }

    @Test
    @DisplayName("O encoder apenas retorna chars inválidos para uma url. Todos chars devem ser substituídos por _")
    public void whenShortUrlAnsStringEncoderReturnsInvalidChars_shouldReplaceCharsTo_() throws NoSuchAlgorithmException {
        String urlUnsafeChars = "!@#$%^&*(=;:'\"";
        final int initialWindow = urlUnsafeChars.length();
        UrlShortener urlShortener = new EncodeRandomUrlShortener(s -> urlUnsafeChars, initialWindow);

        assertThat(urlShortener.encodeUrl(VALID_URL)).isPresent();
        assertThat(urlShortener.encodeUrl(VALID_URL).get()).isEqualTo("_".repeat(urlUnsafeChars.length()));
    }
}
