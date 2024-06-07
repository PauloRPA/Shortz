package com.prpa.Shortz.config;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.repository.ShortzUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.security.SecureRandom;

@Configuration @EnableWebSecurity
public class SecurityConfig {

    @Value("${default.admin.user}")
    private String USER;

    @Value("${default.admin.password}")
    private String PASSWORD;

    @Autowired
    private ShortzUserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/*").permitAll()
                        .requestMatchers("/user/login").anonymous()
                        .requestMatchers("/user/register").anonymous()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(customizer -> customizer
                        .loginPage("/user/login")
                        .loginProcessingUrl("/user/login")
                        .defaultSuccessUrl("/")
                        .failureUrl("/user/login?error"))
                .csrf(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .logoutUrl("/user/logout"))
                .build();
    }

    @Autowired
    public void configureDelegatingMessageSource(DelegatingMessageSource delegatingMessageSource) {
        var messageSource = new ResourceBundleMessageSource();
        messageSource.addBasenames("bundles.exceptions", "bundles.messages");
        messageSource.setUseCodeAsDefaultMessage(true);

        delegatingMessageSource.setParentMessageSource(messageSource);
    }

    @Bean @Profile("dev")
    public WebSecurityCustomizer ignoreH2() {
        return web -> web.ignoring()
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**"));
    }

    private String generateString(int length, String dictionary) {
        StringBuilder randomPassword = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            randomPassword.append(dictionary.charAt(random.nextInt(0, dictionary.length())));
        }

        return randomPassword.toString();
    }

    @Bean @Transactional
    public CommandLineRunner insertDefaultUserAndPassword(PasswordEncoder encoder){
        return args -> {
            if (userRepository.existsByRole(Role.ADMIN)) { return; }

            final int USER_LENGTH = 8;
            final int PASSWORD_LENGTH = 12;

            final boolean generatedCredentials = USER.equals("generate") || PASSWORD.equals("generate");

            String PASSWD_DICT = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder message = new StringBuilder();

            message.append("""
                    The default admin user and password can be defined using the environment variables:
                    SHORTZ_DEFAULT_USER and SHORTZ_DEFAULT_PASSWORD.
                    """);

            if (USER.equals("generate")) {
                message.append("No default admin user defined. Generating user...\n");
                USER = generateString(USER_LENGTH, PASSWD_DICT);
            }

            if (PASSWORD.equals("generate")) {
                message.append("No default admin password defined. Generating password...\n");
                PASSWORD = generateString(PASSWORD_LENGTH, PASSWD_DICT);
            }

            message.append("\nUser: %s\n".formatted(USER));
            message.append("Password: %s\n".formatted(PASSWORD));

            if (generatedCredentials)
                System.out.println(message);

            ShortzUser dev = new ShortzUser(0L, USER, "dev@dev.com", ShortzUser.UNLIMITED_URL_COUNT, encoder.encode(PASSWORD), Role.ADMIN, true);
            userRepository.save(dev);
        };
    }
}
