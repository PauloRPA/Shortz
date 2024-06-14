package com.prpa.Shortz.service;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.dto.ShortzUserDTO;
import com.prpa.Shortz.model.enums.Role;
import com.prpa.Shortz.repository.ShortzUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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

    public Page<ShortzUserDTO> findAll(int page, int pageSize) {
        Page<ShortzUser> users =  shortzUserRepository.findAll(Pageable.ofSize(pageSize).withPage(page));
        return users.map(user -> {
            ShortzUserDTO shortzUserDTO = new ShortzUserDTO();
            shortzUserDTO.setId(UUID.randomUUID());
            shortzUserDTO.setUsername(user.getUsername());
            shortzUserDTO.setEmail(user.getEmail());
            shortzUserDTO.setRole(user.getRole());
            shortzUserDTO.setEnabled(user.getEnabled());
            shortzUserDTO.setUrlCount(user.getUrlCount());
            return shortzUserDTO;
        });
    }

    public boolean existsByEmailIgnoreCase(String email) {
        return shortzUserRepository.existsByEmailIgnoreCase(email);
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        return shortzUserRepository.existsByUsernameIgnoreCase(username);
    }

    public Optional<ShortzUser> findByUsername(String username) {
        return shortzUserRepository.findByUsernameIgnoreCase(username);
    }

    public void update(String id, ShortzUserDTO newInfo) {
        Optional<ShortzUser> userById = shortzUserRepository.findByUsernameIgnoreCase(id);
        if (userById.isEmpty()) return;

        ShortzUser user = userById.get();
        user.setUsername(newInfo.getUsername());
        user.setEmail(newInfo.getEmail());
        user.setUrlCount(newInfo.getUrlCount());
        user.setRole(newInfo.getRole());
        user.setEnabled(newInfo.isEnabled());

        shortzUserRepository.save(user);
    }

    public Optional<ShortzUser> findById(Long id) {
        return shortzUserRepository.findById(id);
    }
}
