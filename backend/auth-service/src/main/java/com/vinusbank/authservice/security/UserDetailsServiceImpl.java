package com.vinusbank.authservice.security;

import com.vinusbank.authservice.entity.User;
import com.vinusbank.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService
 * WHY WE NEED THIS: When a user tries to log in, Spring Security needs a way to fetch the user's password hash from the database.
 * HOW IT WORKS: This overridden service tells Spring EXACTLY how to find our users (by querying our UserRepository).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Triggered automatically by Spring Security during authenticationManager.authenticate()
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Query our physical MySQL database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Use our adapter to convert MySQL row -> Spring Security readable object
        return UserDetailsImpl.build(user);
    }
}
