package org.kob.backend.consumer.utils;

import com.alibaba.fastjson2.JSONObject;
import org.kob.backend.consumer.WebSocketServer;
import org.kob.backend.pojo.Record;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Thread {
    private final Integer rows;
    private final Integer cols;
    private final Integer inner_walls_count;
    private final int[][] g;
    private final static int[] dx = {-1, 0, 1, 0}, dy =  {0, 1, 0, -1};
    private final Player playerA, playerB;
    private Integer nextStepA = null;
    private Integer nextStepB = null;
    private ReentrantLock lock =  new ReentrantLock();
    private String status = "playing";//相反的是finished
    private String loser = "";//all, A, B

    public Game(Integer rows, Integer cols, Integer inner_walls_count, Integer idA, Integer idB) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
        playerA = new Player(idA, rows - 2, 1, new ArrayList<>());
        playerB = new Player(idB, 1, cols - 2, new ArrayList<>());
    }

    public Player getPlayerA() {
        return playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void  setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public int[][] getG() {
        return g;
    }

    private boolean check_connectivity(int sx, int  sy, int tx, int ty) {
        if(sx == tx && sy == ty) return true;
        g[sx][sy] = 1;

        for(int i = 0; i < 4; i++) {
            int x = sx + dx[i];
            int y = sy + dy[i];
            if(x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if(check_connectivity(x, y, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {

        //一个bool数组, 判断哪里该放墙, 一开始先把所有位置初始化成没有墙
        for(int i = 0; i < this.rows; i++) {
            for(int j = 0; j < this.cols; j++) {
                g[i][j] = 0;
            }
        }

        //给地图两边加上墙
        for(int r = 0; r < this.rows; r++) {
            g[r][0] = g[r][this.cols - 1] = 1;
        }

        //给地图上下加上墙
        for(int c = 0; c < this.cols; c++) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }

        Random random = new Random();
        //随机生成墙
        for(int i = 0; i < this.inner_walls_count / 2; i++) {//地图是中心对称的, 所以要/2
            for(int j = 0; j < 1000; j++){//每一堵墙尝试一千次生成, 一生成成功就转到下一堵墙
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if(g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1) {//如果这个位置已经有墙了就再随机一次
                    continue;
                }

                if(r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) {//不能在双方出生点生成墙
                    continue;
                }

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1;
                break;//找到位置生成墙了, 换下一堵墙

            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }

    public void createMap() {
        for(int i = 0; i < 1000; i++) {
            if(draw()) break;
        }
    }

    private boolean nextStep() {
        //先睡200ms, 因为蛇一秒钟走5格, 那么就是200ms走一格, 但是如果在走一格的期间用户操作了很多下, 那么就只会读入最后一下, 中间的操作就会被覆盖掉
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for(int i = 0; i < 50; i++) {
            try {
                Thread.sleep(100);
                lock.lock();
                //先锁住再判断
                try{
                    if(nextStepA != null &&  nextStepB != null) {
                        playerA.getSteps().add(nextStepA);
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);//吧cellsA的最后一位即蛇头取出来
        if(g[cell.x][cell.y] == 1) return false;

        for(int i = 0; i < n - 1; i++) {//判断下一步会不会碰到自己的身体
            if(cellsA.get(i).x == cell.x && cellsA.get(i).y == cell.y) {
                return false;
            }
        }

        for(int i = 0; i < n - 1; i++) {//判断下一步会不会碰到另一条蛇的身体
            //为什么这里也是n - 1?因为两条蛇是一样长的
            if(cellsB.get(i).x == cell.x && cellsB.get(i).y == cell.y) {
                return false;
            }
        }

        return true;
    }

    private void judge() {//判断两名玩家的下一步操作是否合法
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = check_valid(cellsA, cellsB);
        boolean validB = check_valid(cellsB, cellsA);

        if(!validA || !validB) {
            status = "finished";

            if(!validA && !validB) {
                loser = "all";
            } else if(!validA) {
                loser = "A";
            } else {
                loser = "B";
            }
        }
    }

    private void sendAll(String message) {//向每一个玩家广播信息
        WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }

    private void sendMove() {//向两名玩家广播双方的操作
        lock.lock();
        try {
            JSONObject resp = new JSONObject();
            resp.put("event", "move");
            resp.put("a_direction", nextStepA);
            resp.put("b_direction", nextStepB);
            sendAll(resp.toString());

            //要进行下一步了, 那么要把两名玩家的nextStep清空
            nextStepA = nextStepB = null;
        } finally {
            lock.unlock();
        }

    }

    private String getMapString() {
        StringBuilder res = new StringBuilder();
        for(int i = 0; i < this.rows; i++) {
            for(int j = 0; j < this.cols; j++) {
                res.append(this.g[i][j]);
            }
        }
        return res.toString();
    }

    private void saveToDatabase() {
        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );

        WebSocketServer.recordmapper.insert(record);
    }

    private void sendResult() {//向两个客户端返回游戏结果
        JSONObject resp = new JSONObject();
        resp.put("event", "result");
        resp.put("loser", loser);
        saveToDatabase();
        sendAll(resp.toJSONString());
    }

    @Override
    public void run() {
        for(int i = 0; i < 1000; i++) {//为什么是1000呢? 因为地图有大概200格, 蛇每走三步会变长1格, 所以是200*3=600, 保险起见直接1000
            if(nextStep()) {//是否获取了两条蛇的下一步操作
                judge();

                if(status.equals("playing")) {
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
            } else {
                status = "finished";
                lock.lock();//因为涉及到读入操作, 那么一定要加锁
                try {
                    if(nextStepA == null &&  nextStepB == null) {
                        loser = "all";
                    } else if(nextStepA == null) {
                        loser = "A";
                    } else {
                        loser = "B";
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;//有人输了, 那么直接break
            }
        }
    }
}
