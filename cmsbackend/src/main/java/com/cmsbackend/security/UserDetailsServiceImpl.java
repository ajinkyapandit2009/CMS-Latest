
package com.cmsbackend.security;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.cmsbackend.repositories.UserRepository;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String correctedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(correctedEmail)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "User not found with email: " + correctedEmail
                    ));
    }
}
