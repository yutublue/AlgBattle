package org.kob.backend.service.impl.pk;

import org.kob.backend.consumer.WebSocketServer;
import org.kob.backend.consumer.utils.Game;
import org.kob.backend.service.pk.ReceiveBotMoveService;
import org.springframework.stereotype.Service;

@Service
public class ReceiveBotMoveServiceImpl implements ReceiveBotMoveService {
    @Override
    public String receiveBotMove(Integer userId, Integer direction) {
        System.out.println("Received bot move " + userId + " " + direction + " ");

        if(WebSocketServer.users.get(userId) != null){
            Game game = WebSocketServer.users.get(userId).game;
            if(game != null){
                if(game.getPlayerA().getId().equals(userId)) {
                    game.setNextStepA(direction);
                } else if(game.getPlayerB().getId().equals(userId)) {
                    game.setNextStepB(direction);
                }
            }
        }

        return "receive Bot Move success";
    }
}
