package org.kob.botrunningsystem.service.impl.utils;

import org.joor.Reflect;
import org.kob.botrunningsystem.utils.BotInterface;
import org.kob.botrunningsystem.utils.CodeSecurityScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class Consumer {
    private static RestTemplate restTemplate;
    private static final String receiveBotMoveUrl = "http://127.0.0.1:3000/pk/receive/bot/move/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

    public static void consume(Integer userId, String botCode, String input) {
        // 1. 安全扫描，拦截危险代码
        String violation = CodeSecurityScanner.scan(botCode);
        if (violation != null) {
            System.out.println("[Security] 拦截用户 " + userId + " 的Bot: " + violation);
            sendMove(userId, 0);
            return;
        }

        // 2. 在新线程中编译执行，join 控制超时
        Thread botThread = new Thread(() -> {
            try {
                UUID uuid = UUID.randomUUID();
                String uid = uuid.toString().substring(0, 8);

                BotInterface bi = Reflect.compile(
                        "org.kob.botrunningsystem.utils.Bot" + uid,
                        addUid(botCode, uid)
                ).create().get();

                Integer direction = bi.nextMove(input);
                sendMove(userId, direction);
            } catch (Exception e) {
                System.out.println("[BotError] 用户 " + userId + " 的Bot执行异常: " + e.getMessage());
                sendMove(userId, 0);
            }
        });

        botThread.start();
        try {
            botThread.join(2000);
            if (botThread.isAlive()) {
                System.out.println("[Timeout] 用户 " + userId + " 的Bot超时(>2s)，兜底判负");
                sendMove(userId, 0);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String addUid(String code, String uid) {
        int k = code.indexOf(" implements org.kob.botrunningsystem.utils.BotInterface");
        return code.substring(0, k) + uid + code.substring(k);
    }

    private static void sendMove(Integer userId, Integer direction) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", userId.toString());
        data.add("direction", direction.toString());
        restTemplate.postForLocation(receiveBotMoveUrl, data, String.class);
    }
}
