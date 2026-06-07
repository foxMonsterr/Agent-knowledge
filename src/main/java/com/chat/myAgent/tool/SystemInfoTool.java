package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class SystemInfoTool {

    @Tool(description = "获取安全的运行时信息，包括 Java 版本、操作系统类型、时区和当前时间")
    public String getRuntimeInfo() {
        return "运行时信息：\n"
                + "- Java: " + System.getProperty("java.version", "unknown") + "\n"
                + "- OS: " + System.getProperty("os.name", "unknown") + "\n"
                + "- TimeZone: " + ZoneId.systemDefault() + "\n"
                + "- Now: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "列出当前本地 Agent 工具能力")
    public String listToolCapabilities() {
        return """
                当前可用本地工具：
                - datetime: 日期时间查询与日期计算
                - calculator: 数学表达式、百分比、单位换算
                - translate: 文本翻译与语言检测
                - doc: 读取受限 docs 目录下的 txt/md 文件
                - db: 查询模拟员工和部门数据
                - text: 文本统计、清洗、截断、词频
                - json: JSON 校验、格式化、压缩、字段提取
                - collection: 数字统计、排序、去重、频次统计
                - regex: 正则匹配、替换、邮箱和 URL 提取
                - system: 安全运行时信息和能力说明

                当前未接入：实时天气、联网搜索、外部 HTTP 请求。
                """;
    }

    @Tool(description = "说明当前未支持的能力，例如实时天气、联网搜索、外部 HTTP 请求")
    public String explainUnsupportedCapability(@ToolParam(description = "用户想使用但当前未支持的能力") String capability) {
        String target = capability == null || capability.isBlank() ? "该能力" : capability;
        return target + " 当前未接入。本项目现阶段只启用安全本地工具，不访问外部网络、不调用第三方天气 API，也不执行系统命令。"
                + "如果后续要支持实时天气或联网搜索，需要增加独立工具、白名单、超时、密钥配置和失败降级策略。";
    }
}
