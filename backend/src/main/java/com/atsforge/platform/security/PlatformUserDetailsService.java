package com.atsforge.platform.security;

import com.atsforge.platform.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlatformUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public PlatformUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByEmailIgnoreCase(username).map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}

