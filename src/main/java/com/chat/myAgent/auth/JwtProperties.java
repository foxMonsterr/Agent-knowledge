package com.chat.myAgent.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "smart-agent.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥（至少256位，即32个字符以上）
     */
    private String secret = "smartagent-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    /**
     * Token 有效期（小时）
     */
    private int expirationHours = 24;
}
