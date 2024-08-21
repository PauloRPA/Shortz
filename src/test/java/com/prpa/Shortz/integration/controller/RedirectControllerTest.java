package com.prpa.Shortz.integration.controller;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.shortener.contract.UrlShortener;
import com.prpa.Shortz.repository.ShortUrlRepository;
import com.prpa.Shortz.repository.ShortzUserRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static com.prpa.Shortz.model.ShortzUser.UNLIMITED_URL_COUNT;
import static com.prpa.Shortz.model.enums.Role.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
public class RedirectControllerTest {

    public static final Long USER_ID = 2L;
    public static final String USER_PASSWORD = "123";
    public static final String USER_EMAIL = "test@user.com";
    public static final String USER_USERNAME = "testUser";

    public static final Long URL_ID = 1L;
    public static final Instant URL_CREATION_TIMESTAMP = Instant.now();
    public static final Integer URL_HITS = 9;
    public static final String URL_SLUG = "abcdef";
    public static final String URL_STRING = "https://test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortzUserRepository shortzUserRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UrlShortener urlShortener;

    private ShortUrl testUrl;

    @BeforeEach
    public void setup() {
        ShortzUser urlOwner = ShortzUser.builder()
                .username(USER_USERNAME)
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID).urlCreationLimit(UNLIMITED_URL_COUNT)
                .role(ADMIN).enabled(true).build();

        testUrl = ShortUrl.builder()
                .id(URL_ID)
                .owner(urlOwner)
                .creationTimestamp(URL_CREATION_TIMESTAMP)
                .hit(URL_HITS)
                .slug(URL_SLUG)
                .uri(URL_STRING)
                .build();

        shortzUserRepository.save(urlOwner);
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se user get /validSlug deve redirecionar a url salva no banco de dados")
    public void whenGetValidSlug_shouldRedirectToOriginalURL308PermanentRedirect() {
        shortUrlRepository.save(testUrl);

        mockMvc.perform(get("/" + URL_SLUG)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isPermanentRedirect())
                .andExpect(redirectedUrl(URL_STRING))
                .andDo(print());

    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se user get /favicon.ico deve redirecionar para /images/favicon.ico")
    public void whenGetFavicon_shouldRedirectToFaviconAndFound302() {
        mockMvc.perform(get("/favicon.ico")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/images/favicon.ico"))
                .andDo(print());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se user get /SlugThatDoesNotExist deve redirecionar a raiz")
    public void whenGetInvalidSlug_shouldRedirectRoot302Found() {
        String INVALID_SLUG = "/ThisSlugDoesNotExistOnTheDatabase";
        String EXPECTED_REDIRECT_ROOT = "/";

        mockMvc.perform(get(INVALID_SLUG)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(EXPECTED_REDIRECT_ROOT))
                .andDo(print());

    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se user get /validSlug deve redirecionar a url salva no banco de dados e aumentar o hitCounter")
    public void whenGetValidSlug_shouldIncreaseHitCountAndPermanentRedirect308() {
        ShortUrl saved = shortUrlRepository.save(testUrl);
        final Integer hitCount = testUrl.getHit();

        assertThat(hitCount).isEqualTo(URL_HITS);

        mockMvc.perform(get("/" + URL_SLUG)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isPermanentRedirect())
                .andExpect(redirectedUrl(URL_STRING))
                .andDo(print());

        Optional<ShortUrl> savedUriAfterRequest = shortUrlRepository.findById(saved.getId());
        assertThat(savedUriAfterRequest.isPresent()).isTrue();
        assertThat(savedUriAfterRequest.get().getHit()).isEqualTo(URL_HITS + 1);
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se user get uma url do sistema que tem um slug equivalente deve usar a url do sistema")
    public void whenGetSystemSlug_shouldReturnSystemPage() {
        final String[] systemUrls = {"/user/uris", "/user/adm/uris"};
        testUrl.setId(null);

        for (String systemUrl: systemUrls) {
            testUrl.setSlug(systemUrl);
            shortUrlRepository.deleteAll();
            shortUrlRepository.save(testUrl);
            mockMvc.perform(get(systemUrl)
                            .accept(MediaType.TEXT_HTML))
                    .andExpect(status().isOk())
                    .andDo(print());
        }
    }
}
