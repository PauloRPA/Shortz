package com.prpa.Shortz.integration.controller;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.repository.ShortzUserRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

import static com.prpa.Shortz.model.ShortzUser.UNLIMITED_URL_COUNT;
import static com.prpa.Shortz.model.enums.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
public class ShortzUserControllerTest {

    public static final Long USER_ID = 2L;
    public static final String USER_PASSWORD = "123";
    public static final String USER_EMAIL = "test@user.com";
    public static final String USER_USERNAME = "testUser";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortzUserRepository shortzUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ShortzUser testUser;
    private ShortzUserDTO testUserDTO;

    @BeforeEach
    public void setup() {
        testUser = ShortzUser.builder()
                .username(USER_USERNAME)
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID).urlCount(UNLIMITED_URL_COUNT)
                .role(USER).enabled(true).build();

        testUserDTO = ShortzUserDTO.builder()
                .username(testUser.getUsername())
                .email(testUser.getEmail())
                .urlCount(testUser.getUrlCount())
                .role(testUser.getRole())
                .enabled(testUser.getEnabled())
                .build();
    }

    @SneakyThrows @Test @WithAnonymousUser
    public void whenAnonymousGetLogin_shouldSucceed() {
        mockMvc.perform(get("/user/login").accept(MediaType.TEXT_HTML))
                .andExpect(view().name("user/login"))
                .andExpect(status().isOk());
    }

    @SneakyThrows @Test @WithAnonymousUser
    public void whenAnonymousGetUnauthorizedEndpoint_shouldRedirectToLogin() {
        mockMvc.perform(get("/user/thisShouldBeForbidden")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("http://localhost/user/login"))
                .andExpect(status().isFound());
    }

    @SneakyThrows @Test @WithAnonymousUser
    @DirtiesContext
    public void whenAnonymousPostLogin_shouldLoginAndRedirect() {
        shortzUserRepository.save(testUser);
        mockMvc.perform(post("/user/login").with(csrf())
                        .param("username", USER_USERNAME)
                        .param("password", USER_PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(status().isFound())
                .andDo(print());
    }

    @SneakyThrows @Test @WithMockUser(roles = "USER")
    public void whenAuthenticatedUserTriesToLoginAgain_shouldForbidden() {
        mockMvc.perform(get("/user/login"))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows @Test @WithMockUser(roles = "USER")
    public void whenAuthenticatedUserTriesToRegister_shouldForbidden() {
        mockMvc.perform(get("/user/register"))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows @Test @WithMockUser(roles = "USER")
    public void whenAuthenticatedUserTriesToLogout_shouldSuccess() {
        mockMvc.perform(post("/user/logout")
                        .with(csrf())
                        .accept("text/html"))
                .andExpect(redirectedUrl("/"))
                .andExpect(status().isFound());
    }


    // REGISTRATION
    @SneakyThrows @Test @WithAnonymousUser
    public void whenAnonymousUserTriesToRegister_shouldSuccess() {
        mockMvc.perform(post("/user/register").with(csrf())
                                .accept("text/html")
                                .param("username", "newuser")
                                .param("email", "newuser@email.com")
                                .param("password", "newuserpasswd")
                                .param("confirmPassword", "newuserpasswd"))
                .andExpect(redirectedUrl("/"))
                .andExpect(status().isFound());
    }

    @SneakyThrows @Test @WithAnonymousUser
    public void whenAnonymousUserTriesToRegisterWithSpacedUsername_shouldTrimAndSuccess() {
        String usernameWithSpace = "   newuser   ";

        mockMvc.perform(post("/user/register").with(csrf())
                        .accept("text/html")
                        .param("username", usernameWithSpace)
                        .param("email", "newuser@email.com")
                        .param("password", "newuserpasswd")
                        .param("confirmPassword", "newuserpasswd"))
                .andExpect(redirectedUrl("/"))
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(usernameWithSpace.trim())).isPresent();
        assertThat(shortzUserRepository.findByUsernameIgnoreCase(usernameWithSpace)).isEmpty();
    }


    @SneakyThrows @Test @WithAnonymousUser
    public void whenUserTriesToRegisterWithInvalidParams_shouldFail() {
        mockMvc.perform(post("/user/register").with(csrf())
                        .accept("text/html")
                        .param("username", "r")
                        .param("email", "newuseremail.com")
                        .param("password", "")
                        .param("confirmPassword", "newuserpasswd"))
                .andExpect(model().attributeHasFieldErrorCode("userForm","username", "Length"))
                .andExpect(model().attributeHasFieldErrorCode("userForm","email", "Email"))
                .andExpect(model().attributeHasFieldErrors("userForm","password"))
                .andExpect(model().attributeHasFieldErrorCode("userForm","confirmPassword", "error.match.confirmPassword"))
                .andExpect(status().isOk()).andDo(print());
    }


    @SneakyThrows @Test @WithAnonymousUser
    @DirtiesContext
    public void whenUserTriesToRegisterWithPresentUsernameOrEmail_shouldFail() {
        shortzUserRepository.save(testUser);
        mockMvc.perform(post("/user/register").with(csrf())
                        .accept("text/html")
                        .param("username", USER_USERNAME)
                        .param("email", USER_EMAIL)
                        .param("password", "passwordeasytoguess")
                        .param("confirmPassword", "passwordeasytoguess"))
                .andExpect(model().attributeHasFieldErrorCode("userForm","username", "error.exists"))
                .andExpect(model().attributeHasFieldErrorCode("userForm","email", "error.exists"))
                .andExpect(status().isOk()).andDo(print());
    }

    // GET ADM_PANEL
    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetUserManagement_shouldSucceed() {
        mockMvc.perform(get("/user/adm")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("userPage"))
                .andExpect(status().isOk());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetUserManagementWithNoUsers_shouldSucceed() {
        shortzUserRepository.deleteAll();
        
        mockMvc.perform(get("/user/adm")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("userPage"))
                .andExpect(status().isOk());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetUserManagementWithNonexistentPage_shouldRedirectToP0() {
        String PAGE_THAT_DOESNT_EXIST = "999";
        mockMvc.perform(get("/user/adm")
                        .param("p", PAGE_THAT_DOESNT_EXIST)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    // GET adm edit panel
    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    @DirtiesContext
    public void whenUserGetEditPageWithValidUUID_shouldReturnEditPageOk() {
        // Given
        shortzUserRepository.save(testUser);
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        ModelAndView mav = mockMvc.perform(get("/user/adm/edit")
                        .sessionAttr("uuidToUsernameMap", uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(model().attributeExists("editForm"))
                .andExpect(status().isOk())
                .andReturn().getModelAndView();

        assertThat(mav).isNotNull();
        assertThat(mav.getModel().get("editForm")).isEqualTo(testUserDTO);
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetEditPageWithMalformedIdArgument_shouldRedirectToGetUserManagement() {
        mockMvc.perform(get("/user/adm/edit")
                        .param("id", "InvalidUUID")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetEditPageWithInvalidUUID_shouldRedirectToGetUserManagement() {
        mockMvc.perform(get("/user/adm/edit")
                        .param("id", "88b4cb00-b54f-4ddd-9c89-60a2c3c2d955")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetEditPageWithNoUUID_shouldRedirectToGetUserManagement() {
        mockMvc.perform(get("/user/adm/edit")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN")
    public void whenUserGetEditPageForNonExistentUser_shouldRedirectToGetUserManagement() {
        // Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        mockMvc.perform(get("/user/adm/edit")
                        .sessionAttr("uuidToUsernameMap", uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound());

        verify(uuidToUsernameMapMock).clear();
    }

    // POST adm edit panel
    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostEditPageWithValidUser_shouldUpdateDBAndRedirectToGetUserManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        final ShortzUserDTO TEST_USER = testUserDTO;
        String NEW_USERNAME = USER_USERNAME.repeat(2);

        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        mockMvc.perform(post("/user/adm/update").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .param("username", NEW_USERNAME)
                        .param("email", USER_EMAIL)
                        .param("urlCount", String.valueOf(-1))
                        .param("role", String.valueOf(Role.ADMIN))
                        .param("enabled", String.valueOf(true)))
                .andExpect(model().hasNoErrors())
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByEmailIgnoreCase(USER_EMAIL)).isPresent();
        assertThat(shortzUserRepository.findByEmailIgnoreCase(USER_EMAIL).get().getUsername()).isEqualTo(NEW_USERNAME);
    }

    @CsvSource({
            ",NotBlank",
            "thisUsernameAlreadyExist,error.exists",
            "a,Length"
    })
    @ParameterizedTest
    @SneakyThrows @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostEditPageWithInvalidUsername_shouldReturnWithFieldErrors(String currentTestUsername, String errorcode) {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        ShortzUser preExistentUser = ShortzUser.builder()
                .username("thisUsernameAlreadyExist")
                .email("yay@mail.com")
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID + 1).urlCount(UNLIMITED_URL_COUNT)
                .role(USER).enabled(true).build();

        shortzUserRepository.save(testUser);
        shortzUserRepository.save(preExistentUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        mockMvc.perform(post("/user/adm/update").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .param("username", currentTestUsername) //Empty username
                        .param("email", USER_EMAIL)
                        .param("urlCount", String.valueOf(-1))
                        .param("role", String.valueOf(Role.ADMIN))
                        .param("enabled", String.valueOf(true)))
                .andExpect(model().attributeHasFieldErrors("editForm", "username" ))
                .andExpect(model().attributeHasFieldErrorCode("editForm", "username", errorcode ))
                .andExpect(status().isOk());
    }

    @CsvSource({
            ",NotBlank",
            "thisIsAMalformedEmail,Email",
            "thisEmailAlready@exists.com,error.exists"
    })
    @ParameterizedTest
    @SneakyThrows @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostEditPageWithInvalidEmail_shouldReturnWithFieldErrors(String currentTestEmail, String errorcode) {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

        ShortzUser preExistentUser = ShortzUser.builder()
                .username("User")
                .email("thisEmailAlready@exists.com")
                .password(passwordEncoder.encode(USER_PASSWORD))
                .id(USER_ID + 1).urlCount(UNLIMITED_URL_COUNT)
                .role(USER).enabled(true).build();

        shortzUserRepository.save(testUser);
        shortzUserRepository.save(preExistentUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        mockMvc.perform(post("/user/adm/update").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .param("username", USER_USERNAME) //Empty username
                        .param("email", currentTestEmail)
                        .param("urlCount", String.valueOf(-1))
                        .param("role", String.valueOf(Role.ADMIN))
                        .param("enabled", String.valueOf(true)))
                .andExpect(model().attributeHasFieldErrors("editForm", "email" ))
                .andExpect(model().attributeHasFieldErrorCode("editForm", "email", errorcode ))
                .andExpect(status().isOk());
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostEditPageAndTheUserDoesNotExist_shouldRedirectGetUserManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn("This user is not present on the DB");
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        // Then
        mockMvc.perform(post("/user/adm/update").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID))
                        .param("username", USER_USERNAME)
                        .param("email", USER_EMAIL)
                        .param("urlCount", String.valueOf(-1))
                        .param("role", String.valueOf(Role.ADMIN))
                        .param("enabled", String.valueOf(true)))
                .andExpect(status().isFound());

        verify(uuidToUsernameMapMock).clear();
    }

    // Post adm delete
    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostDelete_shouldRemoveSpecifiedUser() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();

        // Then
        mockMvc.perform(post("/user/adm/delete").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isEmpty();
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostDeleteWithInvalidUUID_shouldRedirectGetUserManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(false);

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();

        // Then
        mockMvc.perform(post("/user/adm/delete").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostDeleteNonexistentUser_shouldRedirectGetUserManagement() {
        //Given
        final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(TEST_UUID)).thenReturn("ThisUserDoesNotExist");
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();

        // Then
        mockMvc.perform(post("/user/adm/delete").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", String.valueOf(TEST_UUID)))
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();
    }

    @SneakyThrows @Test @WithMockUser(roles = "ADMIN") @DirtiesContext
    public void whenUserPostDeleteMalformedUUID_shouldRedirectGetUserManagement() {
        //Given
        final String MALFORMED_UUID = "thisIsAMalformedUUID";
        shortzUserRepository.save(testUser);

        // When
        final var uuidToUsernameMapMock = mock(Map.class);
        when(uuidToUsernameMapMock.get(MALFORMED_UUID)).thenReturn(USER_USERNAME);
        when(uuidToUsernameMapMock.containsKey(any())).thenReturn(true);

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();

        // Then
        mockMvc.perform(post("/user/adm/delete").with(csrf())
                        .sessionAttr("uuidToUsernameMap" ,uuidToUsernameMapMock)
                        .param("id", MALFORMED_UUID))
                .andExpect(status().isFound());

        assertThat(shortzUserRepository.findByUsernameIgnoreCase(USER_USERNAME)).isPresent();
    }

}
