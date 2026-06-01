package com.chat.myAgent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 权限视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {
    private String role;
    private List<String> menus;
    private List<String> actions;
}
