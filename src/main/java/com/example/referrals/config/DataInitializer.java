package com.example.referrals.config;

import com.example.referrals.common.model.RoleName;
import com.example.referrals.user.AppUser;
import com.example.referrals.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.bootstrap.super-admin.first-name}")
    private String superAdminFirstName;
