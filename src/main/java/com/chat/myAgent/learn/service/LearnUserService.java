package com.chat.myAgent.learn.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LearnUserService {

    public String currentUserId() {
        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return "anonymous";
        }
        return auth.getName();
    }
}
