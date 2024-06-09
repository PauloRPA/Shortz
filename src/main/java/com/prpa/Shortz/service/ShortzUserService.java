package com.prpa.Shortz.service;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.repository.ShortzUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShortzUserService implements UserDetailsService {

    private final ShortzUserRepository shortzUserRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ShortzUserService(ShortzUserRepository shortzUserRepository, PasswordEncoder passwordEncoder) {
        this.shortzUserRepository = shortzUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<ShortzUser> user = shortzUserRepository.findByUsernameIgnoreCase(usernameOrEmail);
        return user.orElseGet(() ->
            shortzUserRepository.findByEmailIgnoreCase(usernameOrEmail)
            .orElseGet(ShortzUser::new)
        );
    }

    public ShortzUserDTO save(ShortzUser newUser) {

        newUser.setUsername(newUser.getUsername().toLowerCase());
        newUser.setEmail(newUser.getEmail().toLowerCase());
        newUser.setRole(Role.USER);
        newUser.setUrlCount(ShortzUser.UNLIMITED_URL_COUNT);
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setEnabled(true);

        ShortzUser userSaved = shortzUserRepository.save(newUser);

        ShortzUserDTO savedDTO = new ShortzUserDTO();
        savedDTO.setUsername(userSaved.getUsername());
        savedDTO.setEmail(userSaved.getEmail());
        savedDTO.setRole(userSaved.getRole());
        savedDTO.setUrlCount(userSaved.getUrlCount());
        savedDTO.setEnabled(userSaved.getEnabled());

        return savedDTO;
    }

    public boolean existsByEmailIgnoreCase(String email) {
        return shortzUserRepository.existsByEmailIgnoreCase(email);
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return shortzUserRepository.existsByUsernameIgnoreCase(username);
    }
}
