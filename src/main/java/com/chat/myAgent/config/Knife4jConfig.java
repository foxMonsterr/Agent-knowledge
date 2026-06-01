package com.chat.myAgent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 * 作用：
 * 1. 统一生成接口文档
 * 2. 便于 Apifox / Knife4j / 前端联调
 * 访问地址：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartAgent API 文档")
                        .description("基于 Spring AI 的多功能自主决策 Agent 助手 - 接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SmartAgent")
                                .url("https://github.com/foxMonsterr/myAgent")))

                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("输入 JWT Token（不需要加 Bearer 前缀")
                        ));
    }
}
