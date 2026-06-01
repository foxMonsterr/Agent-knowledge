package com.chat.myAgent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由控制器
 *
 * 将根路径重定向到静态页面
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/stream";
    }

    @GetMapping("/chat")
    public String chat() {
        return "redirect:/stream";
    }

    @GetMapping("/stream")
    public String stream() {
        return "forward:/index.html";
    }
}
