package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文档解析工具
 *
 * 读取本地文件内容，支持 txt、md 格式
 * 文件默认存放路径：src/main/resources/docs/
 *
 */
@Component
public class DocParseTool {

    private static final String DOCS_BASE_PATH =
            System.getProperty("smart-agent.docs.path", "src/main/resources/docs/");
    private static final long MAX_FILE_SIZE = 10 * 1024; // 10KB 限制（避免Token超限）

    /**
     * 读取文件内容
     */
    @Tool(description = "读取本地文档文件的内容。支持 .txt 和 .md 格式。文件名示例：sample.md、company-intro.txt")
    public String readFile(
            @ToolParam(description = "文件名，如 sample.md 或 company-intro.txt") String fileName) {
        try {
            // 安全校验：防止路径穿越攻击
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return "错误：文件名不合法，不允许包含路径分隔符";
            }

            // 检查文件扩展名
            if (!fileName.endsWith(".txt") && !fileName.endsWith(".md")) {
                return "错误：仅支持 .txt 和 .md 格式的文件";
            }

            Path filePath = Paths.get(DOCS_BASE_PATH, fileName);

            if (!Files.exists(filePath)) {
                return "错误：文件不存在 - " + fileName + "。可用文件列表：" + listAvailableFiles();
            }

            // 检查文件大小
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                return "错误：文件过大（" + fileSize / 1024 + "KB），超过10KB限制。请使用较小的文件。";
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return "文件 [" + fileName + "] 内容如下：\n\n" + content;
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    /**
     * 列出可用的文档文件
     */
    @Tool(description = "列出 docs 目录下所有可用的文档文件")
    public String listFiles() {
        return listAvailableFiles();
    }

    /**
     * 获取文件信息（大小、行数等）
     */
    @Tool(description = "获取指定文件的基本信息，包括大小和行数")
    public String getFileInfo(
            @ToolParam(description = "文件名") String fileName) {
        try {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return "错误：文件名不合法";
            }

            Path filePath = Paths.get(DOCS_BASE_PATH, fileName);

            if (!Files.exists(filePath)) {
                return "文件不存在: " + fileName;
            }

            long size = Files.size(filePath);
            long lines = Files.lines(filePath, StandardCharsets.UTF_8).count();
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            int charCount = content.length();

            return String.format("文件信息 [%s]:\n- 大小: %d 字节 (%.1f KB)\n- 行数: %d 行\n- 字符数: %d 个字符",
                    fileName, size, size / 1024.0, lines, charCount);
        } catch (IOException e) {
            return "获取文件信息失败: " + e.getMessage();
        }
    }

    /**
     * 列出可用文件
     */
    private String listAvailableFiles() {
        try {
            Path docsDir = Paths.get(DOCS_BASE_PATH);
            if (!Files.exists(docsDir)) {
                return "docs 目录不存在";
            }

            StringBuilder sb = new StringBuilder("可用文件列表：\n");
            Files.list(docsDir)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".txt") || name.endsWith(".md");
                    })
                    .forEach(p -> {
                        try {
                            long size = Files.size(p);
                            sb.append("- ").append(p.getFileName())
                                    .append(" (").append(size / 1024.0).append(" KB)\n");
                        } catch (IOException ignored) {
                        }
                    });

            return sb.toString();
        } catch (IOException e) {
            return "无法列出文件: " + e.getMessage();
        }
    }
}
