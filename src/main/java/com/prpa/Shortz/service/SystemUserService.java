package com.prpa.Shortz.service;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.repository.ShortzUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemUserService implements UserDetailsService {

    @Autowired
    private ShortzUserRepository shortzUserRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<ShortzUser> user = shortzUserRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        return user.orElseGet(ShortzUser::new);
    }
}
