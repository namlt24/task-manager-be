package com.taskmanager.auth.service;

import com.taskmanager.auth.dto.AuthResponse;
import com.taskmanager.auth.dto.ForgotPasswordRequest;
import com.taskmanager.auth.dto.LoginRequest;
import com.taskmanager.auth.dto.RefreshRequest;
import com.taskmanager.auth.dto.RegisterRequest;
import com.taskmanager.auth.dto.ResetPasswordRequest;
import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ConflictException;
import com.taskmanager.email.EmailService;
import com.taskmanager.security.AppUserDetails;
import com.taskmanager.security.JwtService;
import com.taskmanager.user.entity.Role;
import com.taskmanager.user.entity.RoleName;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.mapper.UserMapper;
import com.taskmanager.user.repository.RoleRepository;
import com.taskmanager.user.repository.UserRepository;
import com.taskmanager.workspace.service.WorkspaceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordResetStore passwordResetStore;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final WorkspaceService workspaceService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenStore refreshTokenStore,
                       PasswordResetStore passwordResetStore,
                       EmailService emailService,
                       UserMapper userMapper,
                       WorkspaceService workspaceService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
        this.passwordResetStore = passwordResetStore;
        this.emailService = emailService;
        this.userMapper = userMapper;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER missing"));

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.addRole(userRole);
        user = userRepository.save(user);

        // Every new user gets a personal workspace they own.
        workspaceService.provision(user.getId(), "Cá nhân");

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        return issueTokens(principal.getDomainUser());
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Provided token is not a refresh token");
        }
        Long userId = jwtService.extractUserId(claims);
        if (!refreshTokenStore.isValid(userId, request.refreshToken())) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));
        return issueTokens(user);
    }

    public void logout(Long userId) {
        refreshTokenStore.revoke(userId);
    }

    /**
     * Issues a 6-digit reset code, stores it (1-minute TTL) and emails it to the user.
     * Returns silently when the email is unknown to avoid leaking which accounts exist.
     */
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String code = generateCode();
            passwordResetStore.save(email, code);
            emailService.sendPasswordResetCode(
                    user.getEmail(), user.getFullName(), code, PasswordResetStore.CODE_TTL.toMinutes());
        });
    }

    /**
     * Verifies the reset code and sets the new password. The code is single-use (deleted on success)
     * and existing sessions are revoked so a leaked password can no longer be used.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().toLowerCase();
        String storedCode = passwordResetStore.get(email);
        if (storedCode == null) {
            throw new BadRequestException("Mã xác nhận không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại mã.");
        }
        if (!storedCode.equals(request.code())) {
            throw new BadRequestException("Mã xác nhận không đúng.");
        }
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        passwordResetStore.delete(email);
        refreshTokenStore.revoke(user.getId());
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private AuthResponse issueTokens(User user) {
        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        refreshTokenStore.store(user.getId(), refreshToken, jwtService.getRefreshTokenExpirationMs());
        return AuthResponse.of(accessToken, refreshToken, jwtService.getAccessTokenExpirationMs(),
                userMapper.toDto(user));
    }
}
