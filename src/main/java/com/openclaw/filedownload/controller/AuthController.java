package com.openclaw.filedownload.controller;

import com.openclaw.filedownload.dto.AuthDTO.*;
import com.openclaw.filedownload.entity.User;
import com.openclaw.filedownload.repository.UserRepository;
import com.openclaw.filedownload.service.CustomUserDetailsService;
import com.openclaw.filedownload.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(
                new MessageResponse("用户名已存在", false)
            );
        }
        
        // 检查邮箱是否存在
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(
                new MessageResponse("邮箱已被注册", false)
            );
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse("注册成功", true));
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(
                new MessageResponse("用户名或密码错误", false)
            );
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);
        
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        
        AuthResponse response = new AuthResponse(
            token,
            user.getUsername(),
            user.getEmail(),
            jwtUtil.getExpirationMs() / 1000
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        org.springframework.security.core.userdetails.UserDetails userDetails = 
            (org.springframework.security.core.userdetails.UserDetails) 
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(response);
    }
}
