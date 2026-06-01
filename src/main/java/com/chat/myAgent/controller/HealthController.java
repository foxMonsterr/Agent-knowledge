package com.chat.myAgent.controller;

import com.chat.myAgent.common.context.TraceContext;
import com.chat.myAgent.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口
 */
@Tag(name = "健康检查", description = "应用状态与追踪信息")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("traceId", TraceContext.getTraceId());
        data.put("service", "smart-agent");
        return R.ok(data);
    }
}
