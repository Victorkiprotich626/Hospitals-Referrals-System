package com.example.referrals.common.web;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserFacade {

    public CustomUserDetails requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new IllegalStateException("Authenticated user not available");
    }

    public boolean hasRole(RoleName roleName) {
        return requireUser().getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleName.name()));
    }
}
