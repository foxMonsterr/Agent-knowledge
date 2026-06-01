package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.entity.UserEntity;
import com.chat.myAgent.model.vo.UserVO;
import com.chat.myAgent.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员后台接口
 */
@Tag(name = "管理员后台", description = "用户管理、系统配置、后台能力")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public R<List<UserVO>> users() {
        List<UserVO> list = userRepository.findAll().stream().map(this::toVO).toList();
        return R.ok(list);
    }

    @Operation(summary = "启用/禁用用户")
    @PatchMapping("/users/{id}/enabled")
    public R<UserVO> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        return R.ok(toVO(user));
    }

    @Operation(summary = "修改用户角色")
    @PatchMapping("/users/{id}/role")
    public R<UserVO> changeRole(@PathVariable Long id, @RequestParam String role) {
        UserEntity user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        user.setRole(role.toUpperCase());
        userRepository.save(user);
        return R.ok(toVO(user));
    }

    private UserVO toVO(UserEntity user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
