package com.example.referrals.security;

import com.example.referrals.user.AppUser;
import com.example.referrals.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findAuthenticationUserByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(
            user.getId(),
            user.getHospital() != null ? user.getHospital().getId() : null,
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getDoctorProfile() != null ? user.getDoctorProfile().getId() : null,
            user.getFullName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.isEnabled(),
            !user.isAccountLocked(),
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toSet())
        );
    }
}
