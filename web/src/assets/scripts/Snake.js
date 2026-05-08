import { GameObject } from './GameObject.js';
import { SnakeBody } from './SnakeBody.js';

export class Snake extends GameObject {
    constructor(info, gamemap) {//info是蛇的信息
        super();

        this.id = info.id;
        this.color = info.color;
        this.gamemap = gamemap;

        this.snakebodys = [new SnakeBody(info.r, info.c)];//存放蛇的身体, snakebodys[0]放蛇头
        this.next_body = null;//下一步的目标位置

        this.speed = 5;//蛇每秒钟走5个格子
        this.direction = -1;//-1表示没有指令, 0123表示上右下左
        this.status = "idle";//表示蛇的状态, idle表示静止, move表示移动, die表示死亡

        this.dr = [-1, 0, 1, 0];//行偏移量
        this.dc = [0, 1, 0, -1];//列偏移量

        this.step = 0;
        this.eps = 1e-2;

        this.eye_direction = 0;//眼睛朝上
        if (this.id === 1) {
            this.eye_direction = 2;//眼睛朝下
        }

        this.eye_dx = [//眼睛的行偏移量
            [-1, 1],
            [1, 1],
            [1, -1],
            [-1, -1]
        ];
        this.eye_dy = [//眼睛的列偏移量
            [-1, -1],
            [-1, 1],
            [1, 1],
            [1, -1]
        ]
    }

    start() {

    }

    check_tail_increasing() {//检测当前回合蛇的长度是否增加
        if (this.step <= 10) return true;
        if (this.step % 3 === 0) return true;
        return false;
    }

    set_direction(d) {
        this.direction = d;
    }

    next_step() {//将蛇的状态变为走下一步
        const d = this.direction;
        this.next_body = new SnakeBody(this.snakebodys[0].r + this.dr[d], this.snakebodys[0].c + this.dc[d]);
        this.eye_direction = d;
        this.direction = -1;
        this.status = "move";
        this.step++;

        const k = this.snakebodys.length;
        for (let i = k; i > 0; i--) {
            this.snakebodys[i] = JSON.parse(JSON.stringify(this.snakebodys[i - 1]));
        }
        
    }

    update_move() {

        const dx = this.next_body.x - this.snakebodys[0].x;
        const dy = this.next_body.y - this.snakebodys[0].y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < this.eps) {

            this.snakebodys[0] = this.next_body;
            this.next_body = null;
            this.status = "idle";

            if (!this.check_tail_increasing()) {
                this.snakebodys.pop();//蛇尾出队
            }
        } else {

            let move_distance = this.speed * this.timedelta / 1000;
            if (move_distance > distance) move_distance = distance; // 防越界震荡
            this.snakebodys[0].x += move_distance * dx / distance;
            this.snakebodys[0].y += move_distance * dy / distance;

            if (!this.check_tail_increasing()) {//如果当前回合蛇长度不变, 那么蛇尾就要动
                const k = this.snakebodys.length;
                const tail = this.snakebodys[k - 1];
                const target = this.snakebodys[k - 2];
                const tail_dx = target.x - tail.x;
                const tail_dy = target.y - tail.y;
                tail.x += move_distance * tail_dx / distance;
                tail.y += move_distance * tail_dy / distance;
            }
        }
    }

    update() {

        if (this.status === 'move') {
            this.update_move();

        }
        this.render();
    }

    render() {
        const L = this.gamemap.L;
        const ctx = this.gamemap.ctx;

        ctx.fillStyle = this.color;

        if(this.status === 'die') {
            ctx.fillStyle = 'white';
        }

        for (const snakebody of this.snakebodys) {
            ctx.beginPath();
            ctx.arc(snakebody.x * L, snakebody.y * L, L / 2 * 0.8, 0, Math.PI * 2);//画圆弧
            ctx.fill();
        }

        //填充两节身体之间的空隙
        for (let i = 1; i < this.snakebodys.length; i++) {
            const a = this.snakebodys[i - 1];
            const b = this.snakebodys[i];
            if (Math.abs(a.x - b.x) < this.eps && Math.abs(a.y - b.y) < this.eps)
                continue;
            if (Math.abs(a.x - b.x) < this.eps) {
                ctx.fillRect((a.x - 0.4) * L, Math.min(a.y, b.y) * L, L * 0.8, Math.abs(a.y - b.y) * L);
            } else {
                ctx.fillRect(Math.min(a.x, b.x) * L, (a.y - 0.4) * L, Math.abs(a.x - b.x) * L, L * 0.8);
            }
        }

        ctx.fillStyle = 'black';
        for(let i = 0; i < 2; i++) {
            const eye_x = (this.snakebodys[0].x + this.eye_dx[this.eye_direction][i] * 0.15) * L;
            const eye_y = (this.snakebodys[0].y + this.eye_dy[this.eye_direction][i] * 0.15) * L;
            ctx.beginPath();
            ctx.arc(eye_x, eye_y, L * 0.05, 0, Math.PI * 2);
            ctx.fill();
        }
    }
}