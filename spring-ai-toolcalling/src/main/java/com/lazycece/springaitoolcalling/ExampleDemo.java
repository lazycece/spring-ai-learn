package com.lazycece.springaitoolcalling;

import com.lazycece.springaitoolcalling.tools.DateTimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author lazycece
 * @date 2026/6/29
 */
@Component
public class ExampleDemo implements CommandLineRunner {

    @Autowired
    private ChatClient chatClient;

    public void setAlarm(){
        String answer = chatClient.prompt()
                .user("请在 10 分钟后设置一个闹钟提醒我开会")
                .tools(new DateTimeTools())
                .call()
                .content();
        System.out.println(answer);
    }

    @Override
    public void run(String... args) throws Exception {
        setAlarm();
    }
}
