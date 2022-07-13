package com.example.demo.service;

import com.example.demo.config.AppConfig;
import com.example.demo.dto.AuthenticationResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.NotificationEmail;
import com.example.demo.model.User;
import com.example.demo.model.VerificationToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VerificationTokenRepository;
import com.example.demo.security.JwtProvider;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;

    private final AuthenticationManager authenticationManager;

    private final JwtProvider jwtProvider;
    private final AppConfig appConfig;
    public void signup(RegisterRequest registerRequest) {
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setUserName(registerRequest.getUserName());
        user.setCreatedDate(Instant.now());
        user.setEnabled(false);


     String token =  generateVerificationToken(user);
     String link = appConfig.getUrl() +  "/api/auth/accountVerification/" + token;
     mailService.sendMail(new NotificationEmail("Please Activate your account",user.getEmail(),
             "Thank you for signing up to Our Project, please click on the below url to activate your account:"+link));

    }

    private String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpireDate(Instant.now().plusSeconds(600));

        verificationTokenRepository.save(verificationToken);

        return token;
    }


    public void verifyAccount(String token) {
       VerificationToken verificationToken =  verificationTokenRepository.findByToken(token).orElseThrow(() -> new IllegalStateException("Invalid token"));
        fetchUserAndEnable(verificationToken);
    }

    @Transactional
    void fetchUserAndEnable(VerificationToken verificationToken) {
        Long userId   = verificationToken.getUser().getUserId();
        User user  = userRepository.findById(userId).orElseThrow(() -> new IllegalStateException("User not found exception"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public AuthenticationResponse login(LoginRequest loginRequest) {
      Authentication authentication =  authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserName(),loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    String token = jwtProvider.generateToken(authentication);
    return new AuthenticationResponse(token, loginRequest.getUserName());
    }
}
