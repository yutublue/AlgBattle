package org.kob.backend.service.impl.pk;

import org.kob.backend.consumer.WebSocketServer;
import org.kob.backend.service.pk.StartGameService;
import org.springframework.stereotype.Service;

@Service
public class StarGameServiceImpl implements StartGameService {
    @Override
    public String startGame(Integer aId, Integer bId) {
        System.out.println("Start game: " +  aId + " " + bId);

        WebSocketServer.startGame(aId,bId);

        return "Start game success";
    }
}
