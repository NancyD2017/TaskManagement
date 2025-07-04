package com.example.task_management.security;

import com.example.task_management.model.entity.RefreshToken;
import com.example.task_management.model.entity.User;
import com.example.task_management.model.request.LoginRequest;
import com.example.task_management.model.request.RefreshTokenRequest;
import com.example.task_management.model.request.UpsertUserRequest;
import com.example.task_management.model.response.AuthResponse;
import com.example.task_management.model.response.RefreshTokenResponse;
import com.example.task_management.repository.UserRepository;
import com.example.task_management.service.RefreshTokenService;
import com.example.task_management.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;

    private final RefreshTokenService refreshTokenService;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        return AuthResponse.builder()
                .id(userDetails.getId())
                .token(jwtUtils.generateJwtToken(userDetails))
                .refreshToken(refreshToken.getToken())
                .username(userDetails.getUsername())
                .roles(roles)
                .build();
    }

    public User register(UpsertUserRequest createUserRequest) {
        if (userRepository.findByEmail(createUserRequest.getEmail()).isPresent())
            throw new IllegalArgumentException("User with email " + createUserRequest.getEmail() + " already exists!");
        var user = User.builder()
                .username(createUserRequest.getUsername())
                .password(passwordEncoder.encode(createUserRequest.getPassword()))
                .email(createUserRequest.getEmail())
                .build();
        user.setRoles(createUserRequest.getRoles());

        return userRepository.save(user);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByRefreshToken(requestRefreshToken)
                .map(refreshTokenService::checkRefreshToken)
                .map(RefreshToken::getUserId)
                .flatMap(userRepository::findById)
                .map(tokenOwner -> {
                    String token = jwtUtils.generateTokenFromUsername(tokenOwner.getUsername());
                    String refreshToken = refreshTokenService.createRefreshToken(tokenOwner.getId()).getToken();
                    return new RefreshTokenResponse(token, refreshToken);
                }).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

    }

    public void logout() {
        var currentPrincipal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentPrincipal instanceof AppUserDetails userDetails) {
            Long userId = userDetails.getId();

            refreshTokenService.deleteByUserId(userId);
        }
    }
}
