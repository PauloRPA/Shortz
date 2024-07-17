package com.prpa.Shortz.integration.controller;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortUrlDTO;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;

import static com.prpa.Shortz.model.ShortzUser.UNLIMITED_URL_COUNT;
import static com.prpa.Shortz.model.enums.Role.ADMIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ShortUrlControllerTest {

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

    private ShortUrl testUrl;
    private ShortUrlDTO testUrlDTO;
    private ShortzUser urlOwner;

    @BeforeEach
    public void setup() throws MalformedURLException {
        urlOwner = ShortzUser.builder()
                .username(USER_USERNAME)
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID).urlCount(UNLIMITED_URL_COUNT)
                .role(ADMIN).enabled(true).build();

        URL originalUrl = new URL(URL_STRING);
        testUrl = ShortUrl.builder()
                .id(URL_ID)
                .owner(urlOwner)
                .creationTimestamp(URL_CREATION_TIMESTAMP)
                .hit(URL_HITS)
                .slug(URL_SLUG)
                .url(originalUrl)
                .build();

        shortzUserRepository.save(urlOwner);

        testUrlDTO = ShortUrlDTO.builder()
                .id(String.valueOf(testUrl.getId()))
                .creationTimestamp(testUrl.getCreationTimestamp())
                .hit(testUrl.getHit())
                .slug(testUrl.getSlug())
                .url(testUrl.getUrl())
                .build();
    }

    // GET URLs management panel
    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Quando o usuario acionar a pagina para listar as suas urls.")
    public void whenUserGetUrlManagement_shouldSucceed() {
        mockMvc.perform(get("/user/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Quando o usuário acionar a pagina para listar as suas urls mas não há urls disponíveis.")
    public void whenUserGetUrlManagementWithNoUrls_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithUserDetails(value = USER_USERNAME, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Quando o usuário acionar a pagina que não existe deve redirecionar á pagina 0.")
    public void whenUserGetUrlManagementWithNonexistentPage_shouldRedirectToP0() {
        shortUrlRepository.save(testUrl);
        String PAGE_THAT_DOES_NOT_EXIST = "999";
        mockMvc.perform(get("/user/urls")
                        .param("p", PAGE_THAT_DOES_NOT_EXIST)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    // GET System URLs management panel
    // ####################################################

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Quando o usuario acionar a pagina para listar as urls do sistema.")
    public void whenUserGetSystemUrlManagement_shouldSucceed() {
        mockMvc.perform(get("/user/adm/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Quando o usuário acionar a pagina para listar as suas urls mas não há urls disponíveis.")
    public void whenUserGetSystemUrlManagementWithNoUrls_shouldSucceed() {
        shortUrlRepository.deleteAll();

        mockMvc.perform(get("/user/adm/urls")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("urlsPage"))
                .andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Quando o usuário acionar a pagina que não existe deve redirecionar á pagina 0.")
    public void whenUserGetSystemUrlManagementWithNonexistentPage_shouldRedirectToP0() {
        shortUrlRepository.save(testUrl);
        String PAGE_THAT_DOES_NOT_EXIST = "999";
        mockMvc.perform(get("/user/adm/urls")
                        .param("p", PAGE_THAT_DOES_NOT_EXIST)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

}
