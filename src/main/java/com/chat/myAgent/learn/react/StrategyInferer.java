package com.chat.myAgent.learn.react;

final class StrategyInferer {

    private StrategyInferer() {}

    static String infer(String message) {
        if (message == null || message.isBlank()) return "auto";
        String lower = message.toLowerCase();
        if (containsAny(lower, "测验", "考试", "题目", "quiz", "做题", "测试", "考题")) return "quiz";
        if (containsAny(lower, "复习", "回顾", "薄弱", "review", "巩固", "温习", "掌握")) return "review";
        if (containsAny(lower, "学习", "了解", "入门", "explore", "搜索", "查找", "查一下", "什么是", "解释", "介绍一下")) return "explore";
        return "auto";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
