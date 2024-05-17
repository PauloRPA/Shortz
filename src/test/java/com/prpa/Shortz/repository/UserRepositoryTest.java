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

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShortzUserRepository userRepository;

    private ShortzUser adminRoleUser;
    private ShortzUser userRoleUser;

    @BeforeEach
    public void setup() {
        adminRoleUser = new ShortzUser(1L, "adminName", "adminEmail",
                99, "passwd", Role.ADMIN, true);
        userRoleUser = new ShortzUser(2L, "userName", "userEmail",
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

        var userByName = userRepository.findByUsernameOrEmail("adminName", "notAdminEmail");
        var userByEmail = userRepository.findByUsernameOrEmail("NotAdminName", "adminEmail");

        assertThat(userByName.isPresent()).isTrue();
        assertThat(userByEmail.isPresent()).isTrue();

        assertThat(userByName.get().getUsername()).isEqualTo(adminRoleUser.getUsername());
        assertThat(userByEmail.get().getUsername()).isEqualTo(adminRoleUser.getUsername());
    }

    @Test
    public void whenFindInvalidUserByUsernameOrEmail_shouldReturnEmpty() {
        var userThatShouldNotBeFound = userRepository.findByUsernameOrEmail("Not a human name", "Not an email");

        assertThat(userThatShouldNotBeFound.isPresent()).isFalse();
    }

}
