package com.chat.myAgent.controller;

import com.chat.myAgent.common.result.R;
import com.chat.myAgent.model.vo.DemoFlowStepVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 演示中心接口
 */
@Tag(name = "演示中心", description = "项目演示流程与入口导航")
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    @Operation(summary = "获取演示流程")
    @GetMapping("/flow")
    public R<List<DemoFlowStepVO>> flow() {
        return R.ok(List.of(
                DemoFlowStepVO.builder().title("登录 / 初始化").description("先登录系统，必要时初始化管理员").route("/login").highlight("JWT + RBAC").build(),
                DemoFlowStepVO.builder().title("首页 / 看板").description("查看首页亮点与运营指标").route("/home").highlight("运营指标与趋势").build(),
                DemoFlowStepVO.builder().title("聊天 / Agent").description("演示普通对话、思考模式和工具调用").route("/chat").highlight("TraceId + 审计").build(),
                DemoFlowStepVO.builder().title("知识库 / RAG").description("展示知识检索与引用追溯").route("/knowledge").highlight("RAG + 来源追踪").build(),
                DemoFlowStepVO.builder().title("规划 / 执行").description("展示任务拆解与多步执行").route("/planning").highlight("Planner + Executor").build(),
                DemoFlowStepVO.builder().title("会话 / 审计").description("查看历史会话和审计日志").route("/audit").highlight("链路可追踪").build(),
                DemoFlowStepVO.builder().title("监控 / 用户管理").description("查看系统状态和管理员后台").route("/monitor").highlight("平台化控制台").build()
        ));
    }
}
