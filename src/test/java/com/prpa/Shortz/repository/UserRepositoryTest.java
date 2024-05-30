package com.prpa.Shortz.repository;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    public static final String ADMIN_ROLE_USERNAME = "adminName";
    public static final String ADMIN_ROLE_EMAIL = "adminEmail@email.com";
    public static final String USER_ROLE_USERNAME = "userName";
    public static final String USER_ROLE_EMAIL = "userEmail@email.com";

    @Autowired
    private ShortzUserRepository userRepository;

    private ShortzUser adminRoleUser;
    private ShortzUser userRoleUser;

    @BeforeEach
    public void setup() {
        adminRoleUser = new ShortzUser(0L, ADMIN_ROLE_USERNAME, ADMIN_ROLE_EMAIL,
                99, "passwd", Role.ADMIN, true);
        userRoleUser = new ShortzUser(0L, USER_ROLE_USERNAME, USER_ROLE_EMAIL,
                99, "passwd2", Role.USER, true);
    }

    @Test
    public void whenExistsByRole_shouldReturnFalse() {
        assertThat(userRepository.existsByRole(Role.ADMIN)).isFalse();
        assertThat(userRepository.existsByRole(Role.USER)).isFalse();
    }

    @Test
    public void whenExistsByRole_shouldReturnTrue() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
        assertThat(userRepository.existsByRole(Role.USER)).isTrue();
    }

    @Test
    public void whenFindValidUserByUsernameOrEmail_shouldReturnUser() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        var userByName = userRepository.findByUsernameOrEmailIgnoreCase(ADMIN_ROLE_USERNAME, "notAdminEmail@email.com");
        var userByEmail = userRepository.findByUsernameOrEmailIgnoreCase("NotAdminName", ADMIN_ROLE_EMAIL);

        assertThat(userByName.isPresent()).isTrue();
        assertThat(userByEmail.isPresent()).isTrue();

        assertThat(userByName.get().getUsername()).isEqualTo(adminRoleUser.getUsername());
        assertThat(userByEmail.get().getUsername()).isEqualTo(adminRoleUser.getUsername());
    }

    @Test
    public void whenFindInvalidUserByUsernameOrEmail_shouldReturnEmpty() {
        var userThatShouldNotBeFound = userRepository.findByUsernameOrEmailIgnoreCase("Not a human name", "Not an email");

        assertThat(userThatShouldNotBeFound.isPresent()).isFalse();
    }

    @Test
    public void whenExistsByUsernameIgnoreCase_shouldFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        assertThat(userRepository.existsByUsernameIgnoreCase(ADMIN_ROLE_USERNAME.toLowerCase())).isTrue();
        assertThat(userRepository.existsByUsernameIgnoreCase(USER_ROLE_USERNAME.toLowerCase())).isTrue();

        assertThat(userRepository.existsByUsernameIgnoreCase(ADMIN_ROLE_USERNAME.toUpperCase())).isTrue();
        assertThat(userRepository.existsByUsernameIgnoreCase(USER_ROLE_USERNAME.toUpperCase())).isTrue();

    }

    @Test
    public void whenExistsByUsernameIgnoreCase_shouldNotFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        assertThat(userRepository.existsByUsernameIgnoreCase("nonExistent username")).isFalse();
        assertThat(userRepository.existsByUsernameIgnoreCase("i do not exist")).isFalse();
    }

    @Test
    public void whenExistsByEmailIgnoreCase_shouldFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        assertThat(userRepository.existsByEmailIgnoreCase(ADMIN_ROLE_EMAIL.toLowerCase())).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase(USER_ROLE_EMAIL.toLowerCase())).isTrue();

        assertThat(userRepository.existsByEmailIgnoreCase(ADMIN_ROLE_EMAIL.toUpperCase())).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase(USER_ROLE_EMAIL.toUpperCase())).isTrue();

    }

    @Test
    public void whenExistsByEmailIgnoreCase_shouldNotFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);

        assertThat(userRepository.existsByEmailIgnoreCase("nonExistent@mail.com")).isFalse();
        assertThat(userRepository.existsByEmailIgnoreCase("not an actual email")).isFalse();
    }

    @Test
    public void whenFindByUsernameIgnoreCase_shouldFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);
        var adminFound = userRepository.findByUsernameIgnoreCase(ADMIN_ROLE_USERNAME);
        var userFound = userRepository.findByUsernameIgnoreCase(USER_ROLE_USERNAME);

        assertThat(adminFound.isPresent()).isTrue();
        assertThat(userFound.isPresent()).isTrue();
        var admin = adminFound.get();
        var user = userFound.get();

        assertThat(admin.getUsername()).isEqualTo(adminRoleUser.getUsername());
        assertThat(admin.getEmail()).isEqualTo(adminRoleUser.getEmail());
        assertThat(admin.getPassword()).isEqualTo(adminRoleUser.getPassword());
        assertThat(admin.getRole()).isEqualTo(adminRoleUser.getRole());

        assertThat(user.getUsername()).isEqualTo(userRoleUser.getUsername());
        assertThat(user.getEmail()).isEqualTo(userRoleUser.getEmail());
        assertThat(user.getPassword()).isEqualTo(userRoleUser.getPassword());
        assertThat(user.getRole()).isEqualTo(userRoleUser.getRole());
    }

    @Test
    public void whenFindByEmailIgnoreCase_shouldFind() {
        userRepository.save(adminRoleUser);
        userRepository.save(userRoleUser);
        var adminFound = userRepository.findByEmailIgnoreCase(ADMIN_ROLE_EMAIL);
        var userFound = userRepository.findByEmailIgnoreCase(USER_ROLE_EMAIL);

        assertThat(adminFound.isPresent()).isTrue();
        assertThat(userFound.isPresent()).isTrue();
        var admin = adminFound.get();
        var user = userFound.get();

        assertThat(admin.getUsername()).isEqualTo(adminRoleUser.getUsername());
        assertThat(admin.getEmail()).isEqualTo(adminRoleUser.getEmail());
        assertThat(admin.getPassword()).isEqualTo(adminRoleUser.getPassword());
        assertThat(admin.getRole()).isEqualTo(adminRoleUser.getRole());

        assertThat(user.getUsername()).isEqualTo(userRoleUser.getUsername());
        assertThat(user.getEmail()).isEqualTo(userRoleUser.getEmail());
        assertThat(user.getPassword()).isEqualTo(userRoleUser.getPassword());
        assertThat(user.getRole()).isEqualTo(userRoleUser.getRole());
    }

}
