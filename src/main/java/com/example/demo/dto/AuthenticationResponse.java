package com.example.demo.dto;

import lombok.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticationResponse {
    private String authenticationToken;

    private String username;



}
