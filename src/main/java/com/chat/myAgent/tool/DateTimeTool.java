package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * 时间日期工具
 *
 * 提供时间相关的查询和计算能力
 * 大模型会根据用户问题自主决定是否调用这些方法
 */
@Component
public class DateTimeTool {

    /**
     * 获取当前日期和时间
     */
    @Tool(description = "获取当前的日期和时间，返回格式为 yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取当前是星期几
     */
    @Tool(description = "获取今天是星期几")
    public String getCurrentDayOfWeek() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return "今天是" + dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
    }

    /**
     * 计算两个日期之间相差的天数
     */
    @Tool(description = "计算两个日期之间相差的天数。日期格式为 yyyy-MM-dd")
    public String daysBetween(
            @ToolParam(description = "起始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            long days = ChronoUnit.DAYS.between(start, end);
            return startDate + " 到 " + endDate + " 相差 " + Math.abs(days) + " 天";
        } catch (Exception e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式，例如 2026-05-01";
        }
    }

    /**
     * 计算从今天起多少天后的日期
     */
    @Tool(description = "计算从今天起，经过指定天数后的日期")
    public String addDays(
            @ToolParam(description = "要增加的天数，可以为负数表示之前的日期") int days) {
        LocalDate target = LocalDate.now().plusDays(days);
        String direction = days >= 0 ? "后" : "前";
        return Math.abs(days) + "天" + direction + "的日期是: " +
                target.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) +
                "（" + target.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE) + "）";
    }

    /**
     * 获取指定日期是星期几
     */
    @Tool(description = "查询指定日期是星期几。日期格式为 yyyy-MM-dd")
    public String getDayOfWeek(
            @ToolParam(description = "要查询的日期，格式 yyyy-MM-dd") String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            return date + " 是" + dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE);
        } catch (Exception e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        }
    }
}
