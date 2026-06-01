package com.chat.myAgent.config;

import com.chat.myAgent.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 认证接口：登录、注册、初始化管理员
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 健康检查
                        .requestMatchers("/api/v1/chat/ping").permitAll()

                        // Chat / Agent / Planning / Stream 开发阶段开放
                        .requestMatchers("/api/v1/chat/**").permitAll()
                        .requestMatchers("/api/v1/agent/**").permitAll()
                        .requestMatchers("/api/v1/planning/**").permitAll()
                        .requestMatchers("/api/v1/stream/**").permitAll()

                        // 静态资源与首页
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()

                        // Knife4j / Swagger 文档
                        .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // 知识库流式问答：开发阶段开放
                        .requestMatchers("/api/v1/knowledge/stream").permitAll()

                        // 知识库问答 / 检索 / 状态：登录后可用
                        .requestMatchers("/api/v1/knowledge/ask/**").authenticated()
                        .requestMatchers("/api/v1/knowledge/search").authenticated()
                        .requestMatchers("/api/v1/knowledge/status").authenticated()

                        // 知识库管理：上传/加载 需要 ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/knowledge/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/knowledge/load-directory").hasRole("ADMIN")

                        // 文档列表/删除：USER 和 ADMIN
                        .requestMatchers("/api/v1/knowledge/documents/**").hasAnyRole("ADMIN", "USER")

                        // 监控接口：仅 ADMIN
                        .requestMatchers("/api/v1/monitor/**").hasRole("ADMIN")

                        // Admin 管理接口：仅 ADMIN
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 审计日志：仅 ADMIN
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        // 部署验收：仅 ADMIN
                        .requestMatchers("/api/v1/deploy/**").hasRole("ADMIN")

                        // 运维面板：仅 ADMIN
                        .requestMatchers("/api/v1/ops/**").hasRole("ADMIN")

                        // 发布管理：仅 ADMIN
                        .requestMatchers("/api/v1/release/**").hasRole("ADMIN")

                        // 性能监控：仅 ADMIN
                        .requestMatchers("/api/v1/performance/**").hasRole("ADMIN")

                        // 健康检查：公开
                        .requestMatchers("/api/v1/health").permitAll()

                        // 首页概览：登录用户
                        .requestMatchers("/api/v1/home/**").authenticated()

                        // Demo：登录用户
                        .requestMatchers("/api/v1/demo/**").authenticated()

                        // 权限查询：登录用户
                        .requestMatchers("/api/v1/permission/**").authenticated()

                        // 会话管理：登录用户
                        .requestMatchers("/api/v1/session/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
