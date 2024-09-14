package com.prpa.Shortz.integration.controller;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.time.Instant;
import java.util.*;

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

    private ShortzUser urlOwner;

    @BeforeEach
    public void setup() {
        urlOwner = ShortzUser.builder()
                .username(USER_USERNAME)
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID).urlCreationLimit(UNLIMITED_URL_COUNT)
                .role(ADMIN).enabled(true).build();

        ShortzUser urlLimitedOwner = ShortzUser.builder()
                .username(USER_URL_LIMIT_1_USERNAME)
                .email(USER_EMAIL + "z")
                .urlCreationLimit(1) // Limited
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID + 1)
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
        shortzUserRepository.save(urlLimitedOwner);
    }

    // *********************************
    // GET URLs management panel
    // *********************************

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuario acionar a pagina para listar as suas urls.")
    public void whenUserGetUriManagement_shouldSucceed() {
        mockMvc.perform(get("/user/uris")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário acionar a pagina para listar as suas uris mas não há uris disponíveis.")
    public void whenUserGetUriManagementWithNoUris_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/uris")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário acionar a pagina que não existe deve redirecionar á pagina 0.")
    public void whenUserGetUriManagementWithNonexistentPage_shouldRedirectToP0() {
        shortUrlRepository.save(testUrl);
        String PAGE_THAT_DOES_NOT_EXIST = "999";
        mockMvc.perform(get("/user/uris")
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
    public void whenUserPostDeleteUri_shouldRemoveSpecifiedUri() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteWithInvalidUUID_shouldRedirectGetUriManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(false);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url que não existe deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteNonexistentUri_shouldRedirectGetUriManagement() {
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
        mockMvc.perform(post("/user/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteMalformedUUID_shouldRedirectGetUriManagement() {
        //Given
        final String MALFORMED_UUID = "thisIsAMalformedUUID";
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(MALFORMED_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/uris/delete").with(csrf())
                        .sessionAttr("uuidShortUriIdMap", uuidToShortUrlIdMapMock)
                        .param("id", MALFORMED_UUID))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }


    // *********************************
    // GET System URIs management panel
    // *********************************

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se o o usuario acionar a pagina para listar as uris do sistema.")
    public void whenUserGetSystemUriManagement_shouldSucceed() {
        mockMvc.perform(get("/user/adm/uris")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se o o usuário acionar a pagina para listar as suas uris mas não há uris disponíveis.")
    public void whenUserGetSystemUriManagementWithNoUris_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/adm/uris")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
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
        mockMvc.perform(get("/user/adm/uris")
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
    public void whenUserPostDeleteSystemUri_shouldRemoveSpecifiedUri() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteWithInvalidUUID_shouldRedirectGetSystemUriManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(TEST_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(false);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url que não existe deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteNonexistentUri_shouldRedirectGetSystemUriManagement() {
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
        mockMvc.perform(post("/user/adm/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário confirmar a deleção de uma url com UUID invalido deve redirecionar á pagina de urlManagement.")
    public void whenUserPostDeleteMalformedUUID_shouldRedirectGetSystemUriManagement() {
        //Given
        final String MALFORMED_UUID = "thisIsAMalformedUUID";
        final ShortUrl saved = shortUrlRepository.save(testUrl);

        // When
        final var uuidToShortUrlIdMapMock = mock(Map.class);
        when(uuidToShortUrlIdMapMock.get(MALFORMED_UUID)).thenReturn(saved.getId());
        when(uuidToShortUrlIdMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortUrlRepository.existsById(saved.getId())).isTrue();

        // Then
        mockMvc.perform(post("/user/adm/uris/delete").with(csrf())
                        .sessionAttr("urlDTOIdMap", uuidToShortUrlIdMapMock)
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
    public void whenAnonymousGetNewUriForm_shouldOk() {
        mockMvc.perform(get("/user/uris/new")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/user/login"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário get o form para criar uma nova url deve 200 OK.")
    public void whenUserGetNewUriForm_shouldOk() {
        mockMvc.perform(get("/user/uris/new")
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

            Optional<String> encodedUri = urlShortener.encodeUri(prefixedUri);
            assertThat(encodedUri).isPresent();
            String expectedString = encodedUri.get();

            mockMvc.perform(post("/user/uris/generate")
                            .accept(MediaType.TEXT_PLAIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"uri\": \"%s\"}".formatted(validUri.toString()))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(expectedString));
        }
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post para gerar slug com uri vazia deve retornar vazio 400 BAD_REQUEST.")
    public void whenUserGenerateSlugFromEmptyUri_shouldReturnEmpty() {
        String[] EMPTY_URIS = {"", "{\"uri\": \"\"}"};
        for (String emptyUri : EMPTY_URIS) {
            mockMvc.perform(post("/user/uris/generate")
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
            String invalidUriRequestBody = "{\"uri\": \"%s\"}".formatted(invalidUri);
            mockMvc.perform(post("/user/uris/generate")
                            .accept(MediaType.TEXT_PLAIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidUriRequestBody)
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(""));
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

            Optional<String> encodedUri = urlShortener.encodeUri(prefixedUri);
            assertThat(encodedUri).isPresent();
            String encodedSlug = encodedUri.get();

            mockMvc.perform(post("/user/uris/new")
                            .accept(MediaType.TEXT_HTML)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("uri", prefixedUri.toString())
                            .param("slug", encodedSlug)
                            .with(csrf()))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/user/uris"));
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

        Optional<String> encodedUri = urlShortener.encodeUri(repeatedURIS[0]);
        assertThat(encodedUri).isPresent();
        String encodedSlug = encodedUri.get();

        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", repeatedURIS[0].toString())
                        .param("slug", encodedSlug)
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/uris"));

        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", repeatedURIS[1].toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/uris/newUri"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUriForm", "slug"))
                .andExpect(model().attributeHasFieldErrorCode("newUriForm", "slug", "slug.exists"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_URL_LIMIT_1_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o usuário inserir uma uri valida acima do limite de uris permitido para conta deve " +
            "retornar 400 BAD_REQUEST com mensagem de erro.")
    public void whenUserPostValidURIAboveUserLimit_shouldReturnErrorMessage400BadRequest() {
        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", "https://localhost.io/testing")
                        .param("slug", "abcdef")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/uris"));

        // This request will exceed the limit of one URL that the current user can add
        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", "https://localhost.io/user/can/add/just/one/url")
                        .param("slug", "zyxwvu")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/uris/newUri"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUriForm", "slug"))
                .andExpect(model().attributeHasFieldErrorCode("newUriForm", "slug", "error.newUriForm.user.limit"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma uri vazia deve retornar 400 BAD_REQUEST com mensagem de erro.")
    public void whenUserPostEmptyURI_shouldReturnErrorMessage400BadRequest() {
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

        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", "")
                        .param("slug", "testslug")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("/user/uris/newUri"))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("newUriForm", "uri"))
                .andExpect(model().attributeHasFieldErrorCode("newUriForm", "uri", "NotBlank"));
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma URI valida sem slug deve codificar a URI retornar 302 FOUND.")
    public void whenUserPostEmptySlugAndValidURI_shouldEncodeURIReturn302Found() {
        final URI VALID_URI = URI.create("https://localhost/something2");

        mockMvc.perform(post("/user/uris/new")
                        .accept(MediaType.TEXT_HTML)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("uri", VALID_URI.toString())
                        .param("slug", "")
                        .with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/user/uris"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma URI valida com slug com caracteres inválidos deve retornar 302 FOUND.")
    public void whenUserPostInvalidSlugAndValidURI_shouldEncodeURIReturn400BadRequest() {
        final URI VALID_URI = URI.create("https://localhost/something2");
        final String NON_DICTIONARY_CHARACTERS = "!@#$%^&*(/";

        for (char ch : NON_DICTIONARY_CHARACTERS .toCharArray()) {
            mockMvc.perform(post("/user/uris/new")
                            .accept(MediaType.TEXT_HTML)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("uri", VALID_URI.toString())
                            .param("slug", "bad" + ch + "slug")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeErrorCount("newUriForm", 1))
                    .andExpect(model().attributeHasFieldErrors("newUriForm", "slug"))
                    .andExpect(model().attributeHasFieldErrorCode("newUriForm", "slug", "error.slug.character"))
                    .andDo(print());
        }
    }


    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário post inserir uma URI invalida sem slug deve retornar 400 BAD_REQUEST com error msg.")
    public void whenUserPostEmptySlugAndInvalidURI_shouldReturn400BadRequest() {
        for (String invalidUri : getInvalidUris()) {
            System.out.println(invalidUri);
            mockMvc.perform(post("/user/uris/new")
                            .accept(MediaType.TEXT_HTML)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("uri", invalidUri)
                            .param("slug", "")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeErrorCount("newUriForm", 1))
                    .andExpect(model().attributeHasFieldErrors("newUriForm", "uri"))
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
        String invalidCharactersWithTLD = "http://local!@#$host.com/something";
        String invalidSchemeWithTLD = "gopher://localhost.com/something";
        String invalidURIChars = "!$%^&*()=+";

        List<String> invalidUris = new ArrayList<>(List.of(invalidNoTLDNoScheme, invalidSchemeWithoutTLD,
                invalidCharactersWithTLD, invalidSchemeWithTLD));

        invalidURIChars.chars()
                .mapToObj(ch -> (char) ch)
                .map(ch -> "https://invalid" + ch + "uri.com/path/")
                .forEach(invalidUris::add);

        return invalidUris.toArray(String[]::new);
    }

    // *********************************
    // GET /user/uris?search=
    // *********************************

    @Test
    @SneakyThrows
    @DirtiesContext
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por uma url que existe deve retornar o resultado corretamente.")
    public void whenUserGetUriManagementWithURISearchThatExists_shouldReturnResultsCorrectly() {
        final String URL_PRESENT = "http://localhost:9999/how-to-draw";
        final int expectedNumberOfSearchResults = 1;

        insertUrls(List.of("http://localhost:9999/user/uris",
                "http://localhost:9999/h2-console",
                URL_PRESENT));

        MvcResult mvcResult = mockMvc.perform(get("/user/uris")
                        .param("search", "how-to-draw")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.size()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.get(0)).isEqualTo(URL_PRESENT);
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por uma url que não existe deve retornar vazio.")
    public void whenUserGetUriManagementWithURISearchThatNotExist_shouldReturnEmpty() {
        final int expectedNumberOfSearchResults = 0;

        insertUrls(List.of("http://localhost:9999/user/uris?search=dsf",
                "http://localhost:9999/h2-console",
                "http://localhost:9999/how-to-draw"));

        MvcResult mvcResult = mockMvc.perform(get("/user/uris")
                        .param("search", "value not present")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.isEmpty()).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por algo que existe deve retornar o resultado corretamente.")
    public void whenUserGetUriManagementWithSlugSearchThatExists_shouldReturnResultsCorrectly() {
        final int expectedNumberOfSearchResults = 1;
        String PRESENT_URI = "http://localhost:9999/how-to-draw";
        String PRESENT_SLUG = urlShortener.encodeUri(URI.create(PRESENT_URI)).get().toString();

        insertUrls(List.of("http://localhost:9999/user/uris?search=dsf",
                "http://localhost:9999/h2-console",
                PRESENT_URI));

        MvcResult mvcResult = mockMvc.perform(get("/user/uris")
                        .param("search", PRESENT_SLUG)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.size()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.get(0)).isEqualTo(PRESENT_URI);
    }

    // *********************************
    // GET /user/adm/uris?search=
    // *********************************

    @Test
    @SneakyThrows
    @DirtiesContext
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por uma url que existe deve retornar o resultado corretamente.")
    public void whenUserGetSystemUriManagementWithURISearchThatExists_shouldReturnResultsCorrectly() {
        final String URL_PRESENT = "http://localhost:9999/how-to-draw";
        final int expectedNumberOfSearchResults = 1;

        insertUrls(List.of("http://localhost:9999/user/uris?search=dsf",
                "http://localhost:9999/h2-console",
                URL_PRESENT));

        MvcResult mvcResult = mockMvc.perform(get("/user/adm/uris")
                        .param("search", "how-to-draw")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.size()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.get(0)).isEqualTo(URL_PRESENT);
    }

    @Test
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por uma url que não existe deve retornar vazio.")
    public void whenUserGetSystemUriManagementWithURISearchThatNotExist_shouldReturnEmpty() {
        final int expectedNumberOfSearchResults = 0;

        insertUrls(List.of("http://localhost:9999/user/uris?search=dsf",
                "http://localhost:9999/h2-console",
                "http://localhost:9999/how-to-draw"));

        MvcResult mvcResult = mockMvc.perform(get("/user/adm/uris")
                        .param("search", "value not present")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.isEmpty()).isTrue();
    }

    @Test
    @SneakyThrows
    @DirtiesContext
    @SuppressWarnings("unchecked")
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Se o o usuário pesquisar por algo que existe deve retornar o resultado corretamente.")
    public void whenUserGetSystemUriManagementWithSlugSearchThatExists_shouldReturnResultsCorrectly() {
        final int expectedNumberOfSearchResults = 1;
        String PRESENT_URI = "http://localhost:9999/how-to-draw";
        String PRESENT_SLUG = urlShortener.encodeUri(URI.create(PRESENT_URI)).get().toString();

        insertUrls(List.of("http://localhost:9999/user/adm/uris?search=dsf",
                "http://localhost:9999/h2-console",
                PRESENT_URI));

        MvcResult mvcResult = mockMvc.perform(get("/user/uris")
                        .param("search", PRESENT_SLUG)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urisPage"))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isOk())
                .andReturn();

        Object urisPageObj = mvcResult.getModelAndView().getModel().get("urisPage");
        Page<ShortUrlDTO> urisPage = (Page<ShortUrlDTO>) urisPageObj;

        List<String> urisFound = urisPage.get()
                .map(ShortUrlDTO::getUri)
                .map(URI::toString)
                .toList();

        assertThat(urisPage.getTotalElements()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.size()).isEqualTo(expectedNumberOfSearchResults);
        assertThat(urisFound.get(0)).isEqualTo(PRESENT_URI);
    }


    private void insertUrls(List<String> urls) {
        shortUrlRepository.deleteAll();

        for (String url : urls) {
            ShortUrl shorturl = ShortUrl.builder()
                    .owner(urlOwner)
                    .creationTimestamp(Instant.now())
                    .hit(0)
                    .slug(urlShortener.encodeUri(URI.create(url)).get())
                    .uri(url)
                    .build();

            shortUrlRepository.save(shorturl);
        }
    }


}
