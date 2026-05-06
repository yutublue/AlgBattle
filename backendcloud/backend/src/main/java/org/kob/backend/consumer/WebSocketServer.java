package org.kob.backend.consumer;

import com.alibaba.fastjson2.JSONObject;
import org.kob.backend.consumer.utils.Game;
import org.kob.backend.consumer.utils.JwtAuthentication;
import org.kob.backend.mapper.BotMapper;
import org.kob.backend.mapper.RecordMapper;
import org.kob.backend.mapper.UserMapper;
import org.kob.backend.pojo.Bot;
import org.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {

    final public static ConcurrentHashMap<Integer, WebSocketServer> users = new ConcurrentHashMap<>();

    private User user;
    private Session session = null;

    public static UserMapper usermapper;
    public static RecordMapper recordmapper;
    private static BotMapper  botmapper;

    public static RestTemplate restTemplate;

    public Game game = null;

    private final static String addPlayerUrl = "http://127.0.0.1:3001/player/add/";
    private final static String removePlayerUrl = "http://127.0.0.1:3001/player/remove/";

    @Autowired
    public void setUserMapper(UserMapper usermapper) {
        WebSocketServer.usermapper = usermapper;
    }

    @Autowired
    public void setRecordmapper(RecordMapper recordmapper) {
        WebSocketServer.recordmapper = recordmapper;
    }

    @Autowired
    public void setBotmapper(BotMapper botmapper) {
        WebSocketServer.botmapper = botmapper;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        WebSocketServer.restTemplate = restTemplate;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        this.session = session;
        System.out.println("Connected!");

        Integer userId = JwtAuthentication.getUserId(token);
        this.user = usermapper.selectById(userId);

        if(this.user != null) {
            users.put(userId, this);
        } else {
            this.session.close();
        }

        System.out.println(users);
    }

    @OnClose
    public void onClose() {
        System.out.println("Disconnected!");
        if(this.user != null) {
            users.remove(this.user.getId());
        }
    }

    public static void startGame(Integer aId, Integer aBotId, Integer bId, Integer bBotId) {
        User a = usermapper.selectById(aId);
        User b = usermapper.selectById(bId);

        Bot botA = botmapper.selectById(aBotId);
        Bot botB = botmapper.selectById(bBotId);

        Game game = new Game(
                13,
                14,
                20,
                a.getId(),
                botA,
                b.getId(),
                botB
                );
        game.createMap();
        if(users.get(a.getId()) != null) users.get(a.getId()).game = game;
        if(users.get(b.getId()) != null) users.get(b.getId()).game = game;

        game.start();//新开一个线程

        JSONObject respGame = new JSONObject();
        respGame.put("a_id", game.getPlayerA().getId());
        respGame.put("a_sx",  game.getPlayerA().getSx());
        respGame.put("a_sy", game.getPlayerA().getSy());
        respGame.put("b_id", game.getPlayerB().getId());
        respGame.put("b_sx",  game.getPlayerB().getSx());
        respGame.put("b_sy", game.getPlayerB().getSy());
        respGame.put("map", game.getG());

        JSONObject respA = new JSONObject();
        respA.put("event", "start-matching");
        respA.put("opponent_username", b.getUsername());
        respA.put("opponent_photo", b.getPhoto());
        respA.put("game", respGame);
        if(users.get(a.getId()) != null)
            users.get(a.getId()).sendMessage(respA.toJSONString());

        JSONObject respB = new JSONObject();
        respB.put("event", "start-matching");
        respB.put("opponent_username", a.getUsername());
        respB.put("opponent_photo", a.getPhoto());
        respB.put("game", respGame);
        if(users.get(b.getId()) != null)
            users.get(b.getId()).sendMessage(respB.toJSONString());

    }

    private void startMatching(Integer botId) {
        System.out.println("Starting matching");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();//向MatchingSystem通信
        data.add("user_id", this.user.getId().toString());
        data.add("rating", this.user.getRating().toString());
        data.add("bot_id", botId.toString());
        restTemplate.postForObject(addPlayerUrl, data, String.class);
    }

    private void stopMatching() {
        System.out.println("Stopping matching");
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", this.user.getId().toString());
        restTemplate.postForObject(removePlayerUrl, data, String.class);
    }

    private void move(int direction) {
        if(game.getPlayerA().getId().equals(user.getId())) {
            if(game.getPlayerA().getBotId().equals(-1))//只有真人操作才接收下面这个操作
                game.setNextStepA(direction);
        } else if(game.getPlayerB().getId().equals(user.getId())) {
            if(game.getPlayerB().getBotId().equals(-1))
                game.setNextStepB(direction);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received message!");

        JSONObject data = JSONObject.parseObject(message);
        String event =  data.getString("event");

        if("start-matching".equals(event)) {
            startMatching(data.getInteger("bot_id"));
        } else if("stop-matching".equals(event)) {
            stopMatching();
        } else if("move".equals(event)) {
            move(data.getInteger("direction"));
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) {
        synchronized (this.session) {
            try{
                this.session.getBasicRemote().sendText(message);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
