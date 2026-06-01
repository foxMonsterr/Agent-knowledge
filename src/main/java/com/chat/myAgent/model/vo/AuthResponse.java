package com.chat.myAgent.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    // 认证成功返回的 token
    private String token;

    // 用户信息
    private String username;

    // 用户角色
    private String role;

    // 用户昵称
    private String nickname;

    // token 过期时间
    private Long expiresIn; // 过期时间（秒）
}
