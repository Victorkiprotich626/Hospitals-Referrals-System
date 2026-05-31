package com.example.referrals.common.web;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserFacade {
