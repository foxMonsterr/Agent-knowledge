package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限信息接口
 */
@Tag(name = "权限管理", description = "获取当前用户角色和菜单权限")
@RestController
@RequestMapping("/api/v1/permission")
public class PermissionController {

    @Operation(summary = "获取当前用户权限")
    @GetMapping("/current")
    public R<PermissionVO> current(Authentication authentication) {
        String role = authentication != null && authentication.getAuthorities() != null
                ? authentication.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_GUEST")
                : "ROLE_GUEST";

        List<String> menus;
        List<String> actions;
        if ("ROLE_ADMIN".equals(role)) {
            menus = List.of("home", "chat", "notes", "knowledge", "quiz", "flashcards", "graph", "stats", "session",
                    "demo", "deploy", "release", "dashboard", "agent", "planning", "stream", "monitor", "audit", "docs", "admin");
            actions = List.of("knowledge:upload", "knowledge:delete", "monitor:view", "session:delete", "user:manage", "audit:view", "deploy:view");
        } else {
            menus = List.of("home", "chat", "notes", "knowledge", "quiz", "flashcards", "graph", "stats", "session", "docs");
            actions = List.of("session:view", "deploy:view");
        }

        return R.ok(PermissionVO.builder()
                .role(role)
                .menus(menus)
                .actions(actions)
                .build());
    }
}
