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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.prpa.Shortz.model.ShortzUser.UNLIMITED_URL_COUNT;
import static com.prpa.Shortz.model.enums.Role.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext
@AutoConfigureMockMvc
@SpringBootTest(properties = {"SUPPORTED_PROTOCOLS = https,http,sftp"})
public class ShortUrlControllerTest {

    public static final Long USER_ID = 2L;
    public static final String USER_PASSWORD = "123";
    public static final String USER_EMAIL = "test@user.com";
    public static final String USER_USERNAME = "testUser";
    public static final String USER_URL_LIMIT_1_USERNAME = "testUserLimited";

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
                .id(USER_ID).urlCount(UNLIMITED_URL_COUNT)
                .role(ADMIN).enabled(true).build();

        ShortzUser urlLimitedOwner = ShortzUser.builder()
                .username(USER_URL_LIMIT_1_USERNAME)
                .email(USER_EMAIL + "z")
                .urlCount(1) // Limited
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID + 1)
                .role(ADMIN).enabled(true).build();

        testUrl = ShortUrl.builder()
                .id(URL_ID)
                .owner(urlOwner)
                .creationTimestamp(URL_CREATION_TIMESTAMP)
                .hit(URL_HITS)
                .slug(URL_SLUG)
                .url(URL_STRING)
                .build();

        shortzUserRepository.save(urlOwner);
        shortzUserRepository.save(urlLimitedOwner);
    }

    // *********************************
    // GET URLs management panel
    // *********************************

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuario acionar a pagina para listar as suas urls.")
    public void whenUserGetUrlManagement_shouldSucceed() {
        mockMvc.perform(get("/user/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário acionar a pagina para listar as suas urls mas não há urls disponíveis.")
    public void whenUserGetUrlManagementWithNoUrls_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário acionar a pagina que não existe deve redirecionar á pagina 0.")
    public void whenUserGetUrlManagementWithNonexistentPage_shouldRedirectToP0() {
        shortUrlRepository.save(testUrl);
        String PAGE_THAT_DOES_NOT_EXIST = "999";
        mockMvc.perform(get("/user/urls")
                        .param("p", PAGE_THAT_DOES_NOT_EXIST)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    // *********************************
    // Post url delete
    // *********************************

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url deve remover a url corretamente.")
    public void whenUserPostDeleteUrl_shouldRemoveSpecifiedUrl() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteWithInvalidUUID_shouldRedirectGetUrlManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(false);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url que não existe deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteNonexistentUrl_shouldRedirectGetUrlManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final long THIS_ID_DOES_NOT_EXIST = 99999L;
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);

        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(THIS_ID_DOES_NOT_EXIST);
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteMalformedUUID_shouldRedirectGetUrlManagement() {
        //Given
        final String MALFORMED_UUID = "thisIsAMalformedUUID";
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(MALFORMED_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", MALFORMED_UUID))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }


    // *********************************
    // GET System URLs management panel
    // *********************************

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se o o usuario acionar a pagina para listar as urls do sistema.")
    public void whenUserGetSystemUrlManagement_shouldSucceed() {
        mockMvc.perform(get("/user/adm/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se o o usuário acionar a pagina para listar as suas urls mas não há urls disponíveis.")
    public void whenUserGetSystemUrlManagementWithNoUrls_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/adm/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se o o usuário acionar a pagina que não existe deve redirecionar á pagina 0.")
    public void whenUserGetSystemUrlManagementWithNonexistentPage_shouldRedirectToP0() {
        shortUrlRepository.save(testUrl);
        String PAGE_THAT_DOES_NOT_EXIST = "999";
        mockMvc.perform(get("/user/adm/urls")
                        .param("p", PAGE_THAT_DOES_NOT_EXIST)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    // *********************************
    // Post url delete
    // *********************************

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url deve remover a url corretamente.")
    public void whenUserPostDeleteSystemUrl_shouldRemoveSpecifiedUrl() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteWithInvalidUUID_shouldRedirectGetSystemUrlManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(false);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url que não existe deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteNonexistentUrl_shouldRedirectGetSystemUrlManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final long THIS_ID_DOES_NOT_EXIST = 99999L;
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);

        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(THIS_ID_DOES_NOT_EXIST);
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteMalformedUUID_shouldRedirectGetSystemUrlManagement() {
        //Given
        final String MALFORMED_UUID = "thisIsAMalformedUUID";
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(MALFORMED_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/urls/delete").with(csrf())
                        .sessionAttr("uuidShortUrlIdMap", uuidToShortUrlIdMapMock)
                        .param("id", MALFORMED_UUID))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    // *********************************
    // Get add url endpoint
    // *********************************

    @Test
    @SneakyThrows
    @WithAnonymousUser
    @DisplayName("Se o alguém anonimo get o form para criar uma nova url 302 Found para /user/login.")
    public void whenAnonymousGetNewUrlForm_shouldOk() {
        mockMvc.perform(get("/user/urls/new")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/user/login"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário get o form para criar uma nova url deve 200 OK.")
    public void whenUserGetNewUrlForm_shouldOk() {
        mockMvc.perform(get("/user/urls/new")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk());
    }

    // *********************************
    // Post generate slug endpoint
    // *********************************

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post para gerar slug deve retornar plain text slug 200 OK.")
    public void whenUserGenerateSlugFromValidUri_shouldReturnDefaultPlainTextSlug() {
        URI[] validUris = getValidUris();

        for (URI validUri : validUris) {
            URI prefixedUri = validUri; //A URI enviada para UrlShortener deve possuir um scheme
            if (validUri.getScheme() == null) {
                prefixedUri = URI.create("http://" + validUri);
            }

            Optional<String> encodedUrl = urlShortener.encodeUrl(prefixedUri);
            assertThat(encodedUrl).isPresent();
            String expectedString = encodedUrl.get();

            mockMvc.perform(post("/user/urls/generate")
                            .accept(MediaType.TEXT_PLAIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\": \"%s\"}".formatted(validUri.toString()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(MockMvcResultMatchers.content().string(expectedString));
        }
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post para gerar slug com uri vazia deve retornar vazio 400 BAD_REQUEST.")
    public void whenUserGenerateSlugFromEmptyUri_shouldReturnEmpty() {
        String[] EMPTY_URIS = {"", "{\"url\": \"\"}"};
        for (String emptyUri : EMPTY_URIS) {
            mockMvc.perform(post("/user/urls/generate")
                            .accept(MediaType.TEXT_PLAIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyUri)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(""));
        }
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post para gerar slug com uri invalida deve retornar 400 BAD_REQUEST.")
    public void whenUserGenerateSlugFromInvalidUri_shouldReturnEmpty() {
        for (String invalidUri : getInvalidUris()) {
            String invalidUriRequestBody = "{\"url\": \"%s\"}".formatted(invalidUri);
            mockMvc.perform(post("/user/urls/generate")
                            .accept(MediaType.TEXT_PLAIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidUriRequestBody)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(MockMvcResultMatchers.content().string(""));
        }
    }

    // *********************************
    // POST new uri
    // *********************************

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post adicionar uma nova uri valida deve retornar 200 OK.")
    public void whenUserPostNewValidURI_should200Ok() {
        URI[] validUris = getValidUris();

        for (URI validUri : validUris) {
            URI prefixedUri = validUri; //A URI enviada para UrlShortener deve possuir um scheme
            if (validUri.getScheme() == null) {
                prefixedUri = URI.create("http://" + validUri);
            }

            Optional<String> encodedUrl = urlShortener.encodeUrl(prefixedUri);
            assertThat(encodedUrl).isPresent();
            String encodedSlug = encodedUrl.get();

            mockMvc.perform(post("/user/urls/new")
                            .accept(MediaType.TEXT_HTML)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("url", prefixedUri.toString())
                            .param("slug", encodedSlug)
                            .with(csrf()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/user/urls"));
        }
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário post inserir uma uri com slug existente deve retornar 400 BAD_REQUEST.")
    public void whenUserPostNewURIThatAlreadyExists_shouldReturnBindingErrorMessage() {
        URI[] repeatedURIS = new URI[]{
                URI.create("https://localhost:6969/something"),
                URI.create("http://localhost:6969/something")};

        Optional<String> encodedUrl = urlShortener.encodeUrl(repeatedURIS[0]);
        assertThat(encodedUrl).isPresent();
        String encodedSlug = encodedUrl.get();

        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("url", repeatedURIS[0].toString())
                        .param("slug", encodedSlug)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/urls"));

        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("url", repeatedURIS[1].toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/urls/newUrl"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUrlForm", "slug"))
                .andExpect(model().attributeHasFieldErrorCode("newUrlForm", "slug", "slug.exists"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_URL_LIMIT_1_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário inserir uma uri valida acima do limite de uris permitido para conta deve " +
            "retornar 400 BAD_REQUEST com mensagem de erro.")
    public void whenUserPostValidURIAboveUserLimit_shouldReturnErrorMessage400BadRequest() {
        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("url", "https://localhost.io/testing")
                        .param("slug", "abcdef")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/urls"));

        // This request will exceed the limit of one URL that the current user can add
        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("url", "https://localhost.io/user/can/add/just/one/url")
                        .param("slug", "zyxwvu")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/urls/newUrl"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUrlForm", "slug"))
                .andExpect(model().attributeHasFieldErrorCode("newUrlForm", "slug", "error.newUrlForm.user.limit"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma uri vazia deve retornar 400 BAD_REQUEST com mensagem de erro.")
    public void whenUserPostEmptyURI_shouldReturnErrorMessage400BadRequest() {
        final URI EMPTY_URI = URI.create("");
        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("slug", "testslug")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/uris/newUri"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUriForm", "uri"))
                .andExpect(model().attributeHasFieldErrorCode("newUriForm", "uri", "NotBlank"));

        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", "")
                        .param("slug", "testslug")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/urls/newUrl"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUrlForm", "url"))
                .andExpect(model().attributeHasFieldErrorCode("newUrlForm", "url", "NotBlank"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma URI valida sem slug deve codificar a URI retornar 302 FOUND.")
    public void whenUserPostEmptySlugAndValidURI_shouldEncodeURIReturn302Found() {
        final URI VALID_URI = URI.create("https://localhost/something2");

        mockMvc.perform(post("/user/urls/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("url", VALID_URI.toString())
                        .param("slug", "")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/urls"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma URI invalida sem slug deve retornar 400 BAD_REQUEST com error msg.")
    public void whenUserPostEmptySlugAndInvalidURI_shouldReturn400BadRequest() {
        for (String invalidUri : getInvalidUris()) {
            mockMvc.perform(post("/user/urls/new")
                            .accept(MediaType.TEXT_HTML)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("url", invalidUri)
                            .param("slug", "")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeErrorCount("newUrlForm", 1))
                    .andExpect(model().attributeHasFieldErrors("newUrlForm", "url"))
                    .andDo(print());
        }
    }

    /**
     * Uma URI só é valida caso a mesma possua o scheme e/ou TLD. A URI não pode NÂO conter ambos.
     * Além disso, a URI deve ter um scheme válido para encurtamento. Os schemes validos são definidos pela variável de
     * ambiente SUPPORTED_PROTOCOLS.
     * Para fins de teste estão sendo considerados validos os protocolos, http, https, sftp.
     *
     * @return Array com urls validas
     */
    private static URI[] getValidUris() {
        String validNoSchemeUriWithTLD = "localhost.com/something1";
        String validHTTPUriWithoutTLD = "http://localhost/something2";
        String validHTTPSUriWithoutTLD = "https://localhost/something3";
        String validSFTPUriWithoutTLD = "sftp://localhost/something4";

        String validHTTPUriWithTLD = "http://localhost.com/something5";
        String validHTTPSUriWithTLD = "https://localhost.com/something6";
        String validSFTPUriWithTLD = "sftp://localhost.com/something7";

        String[] validUrisString = {validNoSchemeUriWithTLD, validHTTPUriWithoutTLD, validHTTPSUriWithoutTLD,
                validSFTPUriWithoutTLD, validHTTPUriWithTLD, validHTTPSUriWithTLD, validSFTPUriWithTLD};

        return Arrays.stream(validUrisString).map(URI::create).toArray(URI[]::new);
    }

    /**
     * Uma URI é invalida caso a mesma possua caracteres inválidos, não possua um scheme valido (descrito na variável
     * de ambiente) e/ou esteja faltando o scheme E o TLD.
     *
     * @return Array com urls invalidas
     */
    private static String[] getInvalidUris() {
        String invalidNoTLDNoScheme = "localhost/something";
        String invalidSchemeWithoutTLD = "gopher://localhost/something";
        String invalidCharactersWithTLD = "http://localhos-=_+!@#$^&*()t.com/something";
        String invalidSchemeWithTLD = "gopher://localhost.com/something";

        return new String[]{invalidNoTLDNoScheme, invalidSchemeWithoutTLD,
                invalidCharactersWithTLD, invalidSchemeWithTLD};
    }
}
