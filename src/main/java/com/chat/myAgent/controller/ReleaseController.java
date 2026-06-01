package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发布说明接口
 */
@Tag(name = "发布说明", description = "最终验收、发布清单与演示材料")
@RestController
@RequestMapping("/api/v1/release")
public class ReleaseController {

    @Operation(summary = "发布清单")
    @GetMapping("/checklist")
    public R<Map<String, Object>> checklist() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", List.of(
                "后端服务启动并通过健康检查",
                "前端页面可正常访问",
                "登录/注册/初始化管理员可用",
                "聊天、Agent、RAG、规划、审计、监控可用",
                "Docker Compose 已配置",
                "README 与演示流程已整理"
        ));
        data.put("status", "READY_TO_RELEASE");
        return R.ok(data);
    }

    @Operation(summary = "发布摘要")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "Smart Agent Release Summary");
        data.put("highlights", List.of(
                "多 Agent 协同与统一审计",
                "RAG 知识库与工具调用",
                "JWT + RBAC 权限控制",
                "监控、部署、自检和演示中心"
        ));
        data.put("status", "APPROVED");
        return R.ok(data);
    }
}
