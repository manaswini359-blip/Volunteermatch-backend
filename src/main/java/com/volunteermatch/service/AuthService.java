package com.volunteermatch.service;

import com.volunteermatch.dto.AuthRequest;
import com.volunteermatch.dto.AuthResponse;
import com.volunteermatch.dto.RegisterRequest;
import com.volunteermatch.entity.Organization;
import com.volunteermatch.entity.User;
import com.volunteermatch.entity.VolunteerProfile;
import com.volunteermatch.exception.BadRequestException;
import com.volunteermatch.repository.OrganizationRepository;
import com.volunteermatch.repository.UserRepository;
import com.volunteermatch.repository.VolunteerProfileRepository;
import com.volunteermatch.security.JwtTokenProvider;
import com.volunteermatch.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String cleanName = request.getName() != null ? request.getName().trim() : "";

        log.info("Registration attempt received for email: '{}', role: '{}'", cleanEmail, request.getRole());

        if (cleanEmail.isEmpty()) {
            throw new BadRequestException("Email is required");
        }

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            log.warn("Registration rejected: Account with email '{}' already exists in database", cleanEmail);
            throw new BadRequestException("An account with this email already exists: " + cleanEmail);
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Registration rejected: Password and confirm password mismatch for email '{}'", cleanEmail);
            throw new BadRequestException("Passwords do not match");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().trim().toUpperCase());
        } catch (Exception e) {
            log.warn("Registration rejected: Invalid role '{}' for email '{}'", request.getRole(), cleanEmail);
            throw new BadRequestException("Invalid role. Must be VOLUNTEER or ORGANIZATION");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .name(cleanName)
                .email(cleanEmail)
                .password(hashedPassword)
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully in database: ID={}, email='{}', role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        Long profileId = null;
        String profileImage = null;
        String orgName = null;

        if (role == User.Role.VOLUNTEER) {
            VolunteerProfile profile = VolunteerProfile.builder()
                    .user(savedUser)
                    .bio(request.getBio() != null && !request.getBio().isBlank() ? request.getBio().trim() : null)
                    .skills(request.getSkills() != null && !request.getSkills().isBlank() ? request.getSkills().trim() : null)
                    .interests(request.getInterests() != null && !request.getInterests().isBlank() ? request.getInterests().trim() : null)
                    .location(request.getLocation() != null && !request.getLocation().isBlank() ? request.getLocation().trim() : null)
                    .availability(request.getAvailability() != null && !request.getAvailability().isBlank() ? request.getAvailability().trim() : null)
                    .experience(request.getExperience() != null && !request.getExperience().isBlank() ? request.getExperience().trim() : null)
                    .experienceLevel(request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank() ? request.getExperienceLevel().trim() : "Beginner")
                    .profileImage(null)
                    .build();
            VolunteerProfile savedProfile = volunteerProfileRepository.save(profile);
            profileId = savedProfile.getId();
            profileImage = savedProfile.getProfileImage();

            notificationService.sendNotification(savedUser.getId(),
                    "Welcome to VolunteerMatch! Complete your profile with your skills and interests to find matching opportunities.",
                    com.volunteermatch.entity.Notification.NotificationType.SYSTEM);

        } else if (role == User.Role.ORGANIZATION) {
            Organization organization = Organization.builder()
                    .user(savedUser)
                    .organizationName(request.getOrganizationName() != null && !request.getOrganizationName().isBlank() ? request.getOrganizationName().trim() : cleanName)
                    .description(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription().trim() : "Nonprofit / Community Service Organization")
                    .location(request.getLocation() != null && !request.getLocation().isBlank() ? request.getLocation().trim() : "City")
                    .profileImage("https://images.unsplash.com/photo-1582213782179-e0d53f98f2ca?w=150")
                    .build();
            Organization savedOrg = organizationRepository.save(organization);
            profileId = savedOrg.getId();
            profileImage = savedOrg.getProfileImage();
            orgName = savedOrg.getOrganizationName();

            notificationService.sendNotification(savedUser.getId(),
                    "Welcome to VolunteerMatch! Create your first event and let our AI discover suitable volunteers.",
                    com.volunteermatch.entity.Notification.NotificationType.SYSTEM);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, request.getPassword())
            );
        } catch (Exception ex) {
            log.error("AuthenticationManager failed immediately after registration for email '{}': {}", cleanEmail, ex.getMessage());
            throw new BadRequestException("Registration succeeded but initial login failed: " + ex.getMessage());
        }

        String jwt;
        try {
            jwt = tokenProvider.generateToken(authentication);
            log.info("JWT generated successfully for new user '{}'", cleanEmail);
        } catch (Exception ex) {
            log.error("JWT generation failed for new user '{}': {}", cleanEmail, ex.getMessage(), ex);
            throw new BadRequestException("Failed to generate authorization token: " + ex.getMessage());
        }

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .profileId(profileId)
                .profileImage(profileImage)
                .organizationName(orgName)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword();

        log.info("Login attempt received for email: '{}'", cleanEmail);

        if (cleanEmail.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            log.warn("Login failed: Missing email or password");
            throw new BadCredentialsException("Invalid email or password");
        }

        // Diagnostic verification against database
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User NOT found in database for email '{}'", cleanEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        User existingUser = userOpt.get();

        // Compare entered plain-text password with stored BCrypt hash
        boolean passwordMatches = passwordEncoder.matches(rawPassword, existingUser.getPassword());
        if (!passwordMatches) {
            log.warn("Login failed: Password mismatch for user email '{}'. Plain-text password did not match stored BCrypt hash.", cleanEmail);
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("Login password verified successfully for user '{}'. Authenticating with AuthenticationManager...", cleanEmail);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, rawPassword)
            );
        } catch (Exception ex) {
            log.warn("AuthenticationManager rejected credentials for user '{}': {}", cleanEmail, ex.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt;
        try {
            jwt = tokenProvider.generateToken(authentication);
            log.info("JWT successfully generated for user '{}' (role: {})", cleanEmail, existingUser.getRole());
        } catch (Exception ex) {
            log.error("JWT generation failed for user '{}': {}", cleanEmail, ex.getMessage(), ex);
            throw new BadRequestException("Failed to generate authentication token: " + ex.getMessage());
        }

        User user = existingUser;
        Long profileId = null;
        String profileImage = null;
        String orgName = null;

        if (user.getRole() == User.Role.VOLUNTEER) {
            VolunteerProfile profile = volunteerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                profileId = profile.getId();
                profileImage = profile.getProfileImage();
            }
        } else if (user.getRole() == User.Role.ORGANIZATION) {
            Organization org = organizationRepository.findByUserId(user.getId()).orElse(null);
            if (org != null) {
                profileId = org.getId();
                profileImage = org.getProfileImage();
                orgName = org.getOrganizationName();
            }
        }

        log.info("Login process successfully completed for user ID: {}, email: '{}', role: {}",
                user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileId(profileId)
                .profileImage(profileImage)
                .organizationName(orgName)
                .build();
    }
}
