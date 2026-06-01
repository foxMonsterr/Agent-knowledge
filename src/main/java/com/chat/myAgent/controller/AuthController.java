package com.chat.myAgent.controller;

import com.chat.myAgent.auth.JwtProperties;
import com.chat.myAgent.auth.JwtTokenProvider;
import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.dto.LoginRequest;
import com.chat.myAgent.model.dto.RegisterRequest;
import com.chat.myAgent.model.entity.UserEntity;
import com.chat.myAgent.model.vo.AuthResponse;
import com.chat.myAgent.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证接口
 *
 * 提供注册、登录功能
 * 所有接口公开，无需Token
 */
@Slf4j
@Tag(name = "认证管理", description = "用户注册、登录、Token获取")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final Environment environment;

    /**
     * 用户注册
     * POST /api/v1/auth/register
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return R.fail(400, "用户名已存在: " + request.getUsername());
        }

        // 创建用户
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("USER");
        user.setEnabled(true);

        userRepository.save(user);
        log.info("新用户注册: {}, traceId={}", user.getUsername(), TraceContext.getTraceId());

        // 自动登录，返回 Token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        return R.ok("注册成功", AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .nickname(user.getNickname())
                .expiresIn((long) jwtProperties.getExpirationHours() * 3600)
                .build());
    }

    /**
     * 用户登录
     * POST /api/v1/auth/login
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return R.fail(401, "用户名或密码错误");
        }

        if (!user.getEnabled()) {
            return R.fail(403, "账号已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成 Token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        log.info("用户登录: {}, traceId={}", user.getUsername(), TraceContext.getTraceId());

        return R.ok("登录成功", AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .nickname(user.getNickname())
                .expiresIn((long) jwtProperties.getExpirationHours() * 3600)
                .build());
    }

    /**
     * 创建管理员账号（仅供初始化使用）
     * POST /api/v1/auth/init-admin
     *
     * 生产环境应禁用此接口
     */
    @Operation(summary = "初始化管理员账号")
    @PostMapping("/init-admin")
    public R<AuthResponse> initAdmin() {
        boolean enabled = environment != null && Boolean.parseBoolean(environment.getProperty("smart-agent.admin.init-enabled", "false"));
        boolean devProfile = false;
        if (environment != null && environment.getActiveProfiles() != null) {
            for (String profile : environment.getActiveProfiles()) {
                if ("dev".equalsIgnoreCase(profile)) {
                    devProfile = true;
                    break;
                }
            }
        }
        if (!(enabled || devProfile)) {
            return R.fail(403, "该接口仅允许开发环境使用");
        }
        if (userRepository.existsByUsername("admin")) {
            return R.fail(400, "管理员账号已存在");
        }

        String initPassword = environment != null ? environment.getProperty("smart-agent.admin.init-password", "") : "";
        if (initPassword.isBlank()) {
            return R.fail(500, "未配置管理员初始化密码");
        }

        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(initPassword));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setEnabled(true);

        userRepository.save(admin);

        String token = jwtTokenProvider.generateToken(admin.getUsername(), admin.getRole());

        return R.ok("管理员账号创建成功", AuthResponse.builder()
                .token(token)
                .username(admin.getUsername())
                .role(admin.getRole())
                .nickname(admin.getNickname())
                .expiresIn((long) jwtProperties.getExpirationHours() * 3600)
                .build());
    }
}
