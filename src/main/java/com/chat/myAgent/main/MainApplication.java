package com.chat.myAgent.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动程序
 *
 */
@SpringBootApplication(scanBasePackages = "com.chat.myAgent")
@SpringBootConfiguration
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.chat.myAgent.repository",
        "com.chat.myAgent.learn.repository.jpa"
})
@EnableMongoRepositories(basePackages = {
        "com.chat.myAgent.repository.mongo",
        "com.chat.myAgent.learn.repository.mongo",
        "com.chat.myAgent.react.repository"
})
@EntityScan(basePackages = {
        "com.chat.myAgent.model.entity",
        "com.chat.myAgent.learn.model"
})
public class MainApplication
{
    public static void main(String[] args)
    {

        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(MainApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  启动成功   ლ(´ڡ`ლ)ﾞ  \n");
    }
}
