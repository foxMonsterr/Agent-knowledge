package com.chat.myAgent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 计算器工具
 *
 * 提供数学运算能力，支持基础四则运算和复杂表达式
 */
@Component
public class CalculatorTool {

    /**
     * 计算数学表达式
     * 支持：加减乘除、括号、幂运算等
     */
    @Tool(description = "计算数学表达式，支持加(+)减(-)乘(*)除(/)、括号、取余(%)等运算。例如: 2+3*4, (10+5)/3, 128*256")
    public String calculate(
            @ToolParam(description = "要计算的数学表达式，如 2+3*4 或 (100-20)/4") String expression) {
        try {
            // 安全校验：只允许数字和数学运算符
            if (!expression.matches("[0-9+\\-*/().%\\s]+")) {
                return "错误：表达式包含非法字符，只支持数字和运算符(+-*/%.())";
            }

            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");

            if (engine == null) {
                // 如果没有 JS 引擎，使用简单的方式计算
                return calculateSimple(expression);
            }

            Object result = engine.eval(expression);
            return expression + " = " + result.toString();
        } catch (Exception e) {
            return "计算错误: " + e.getMessage() + "。请检查表达式是否正确。";
        }
    }

    /**
     * 计算百分比
     */
    @Tool(description = "计算百分比。例如：计算200的15%，或者计算30是120的百分之几")
    public String calculatePercentage(
            @ToolParam(description = "计算模式：'of' 表示求某数的百分之几（如200的15%），'is' 表示求占比（如30是120的百分之几）") String mode,
            @ToolParam(description = "第一个数字") double num1,
            @ToolParam(description = "第二个数字") double num2) {
        if ("of".equalsIgnoreCase(mode)) {
            double result = num1 * num2 / 100;
            return num1 + " 的 " + num2 + "% = " + result;
        } else if ("is".equalsIgnoreCase(mode)) {
            if (num2 == 0) return "错误：除数不能为0";
            double result = (num1 / num2) * 100;
            return num1 + " 是 " + num2 + " 的 " + String.format("%.2f", result) + "%";
        } else {
            return "不支持的计算模式，请使用 'of' 或 'is'";
        }
    }

    /**
     * 单位换算
     */
    @Tool(description = "进行常见单位换算，支持：km/m（千米/米）、kg/g（千克/克）、celsius/fahrenheit（摄氏/华氏温度）")
    public String convertUnit(
            @ToolParam(description = "数值") double value,
            @ToolParam(description = "源单位，如 km、m、kg、g、celsius、fahrenheit") String fromUnit,
            @ToolParam(description = "目标单位") String toUnit) {
        try {
            double result;
            String from = fromUnit.toLowerCase();
            String to = toUnit.toLowerCase();

            if (from.equals("km") && to.equals("m")) {
                result = value * 1000;
            } else if (from.equals("m") && to.equals("km")) {
                result = value / 1000;
            } else if (from.equals("kg") && to.equals("g")) {
                result = value * 1000;
            } else if (from.equals("g") && to.equals("kg")) {
                result = value / 1000;
            } else if (from.equals("celsius") && to.equals("fahrenheit")) {
                result = value * 9 / 5 + 32;
            } else if (from.equals("fahrenheit") && to.equals("celsius")) {
                result = (value - 32) * 5 / 9;
            } else {
                return "不支持的单位转换: " + fromUnit + " → " + toUnit;
            }

            return value + " " + fromUnit + " = " + String.format("%.2f", result) + " " + toUnit;
        } catch (Exception e) {
            return "换算错误: " + e.getMessage();
        }
    }

    /**
     * 简单计算（备用方案，当 ScriptEngine 不可用时）
     */
    private String calculateSimple(String expression) {
        try {
            // 只处理简单的二元运算
            expression = expression.replaceAll("\\s+", "");

            if (expression.contains("+")) {
                String[] parts = expression.split("\\+", 2);
                double result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
                return expression + " = " + formatNumber(result);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*", 2);
                double result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
                return expression + " = " + formatNumber(result);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/", 2);
                double divisor = Double.parseDouble(parts[1]);
                if (divisor == 0) return "错误：除数不能为0";
                double result = Double.parseDouble(parts[0]) / divisor;
                return expression + " = " + formatNumber(result);
            } else if (expression.contains("-")) {
                // 注意：要处理负数的情况
                int idx = expression.lastIndexOf('-');
                if (idx == 0) return "表达式不完整";
                String left = expression.substring(0, idx);
                String right = expression.substring(idx + 1);
                double result = Double.parseDouble(left) - Double.parseDouble(right);
                return expression + " = " + formatNumber(result);
            }
            return "无法计算该表达式，请使用简单的二元运算";
        } catch (NumberFormatException e) {
            return "数字格式错误: " + e.getMessage();
        }
    }

    private String formatNumber(double num) {
        if (num == (long) num) {
            return String.valueOf((long) num);
        }
        return String.format("%.6f", num).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
