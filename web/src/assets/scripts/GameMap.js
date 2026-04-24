import { GameObject } from './GameObject.js';
import { Snake } from './Snake.js';
import { Wall } from './Wall.js';

export class GameMap extends GameObject {
    constructor(ctx, parent) {//ctx是画布, parent是画布的父元素, 用来动态修改画布的长宽
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.L = 0;//L是一个单位的长度, 一个地图的是13×13个单位

        this.rows = 13;
        this.cols = 14;

        this.innner_walls_count = 20;//内部障碍物数量
        this.walls = [];

        this.snakes = [
            new Snake({ id: 0, color: "#4876EC", r: this.rows - 2, c: 1 }, this),
            new Snake({ id: 1, color: "#F94848", r: 1, c: this.cols - 2 }, this)
        ];
    }

    //判断地图是否连通
    check_connectivity(g, sx, sy, tx, ty) {//用flood fill算法判断
        if (sx == tx && sy == ty) {
            return true;
        }

        g[sx][sy] = true;

        let dx = [-1, 0, 1, 0], dy = [0, 1, 0, -1];
        for (let i = 0; i < 4; i++) {
            let x = sx + dx[i], y = sy + dy[i];
            if (!g[x][y] && this.check_connectivity(g, x, y, tx, ty)) {
                return true;
            }
        }

        return false;
    }

    create_walls() {
        const g = [];//一个bool数组, 判断哪里该放墙

        for (let r = 0; r < this.rows; r++) {
            g[r] = [];
            for (let c = 0; c < this.cols; c++) {
                g[r][c] = false;//把地图上每一块都加进bool数组里面， false就是不放墙
            }
        }

        //先给地图左右两边加上墙
        for (let r = 0; r < this.rows; r++) {
            g[r][0] = true;
            g[r][this.cols - 1] = true;
        }

        //再给地图上下两边加上墙
        for (let c = 0; c < this.cols; c++) {
            g[0][c] = true;
            g[this.rows - 1][c] = true;
        }

        //再给地图内部加上墙
        for (let i = 0; i < this.innner_walls_count; i++) {
            for (let j = 0; j < 1000; j++) {
                let r = parseInt(Math.random() * this.rows);
                let c = parseInt(Math.random() * this.cols);

                if (g[r][c] || g[this.rows - 1 - r][this.cols - 1 - c]) {
                    continue;
                }

                if (r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) {
                    continue;//不能在出生点即左下角和右上角有墙
                }

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = true;
                break;
            }
        }

        const copy_g = JSON.parse(JSON.stringify(g));//创建一个地图副本, 然后在地图副本里面看看是否连通, 防止对实际地图产生影响
        if (!this.check_connectivity(copy_g, this.rows - 2, 1, 1, this.cols - 2)) {
            return false;
        }

        //bool数组判断完毕之后就把墙加进去
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }

        return true;
    }

    add_listening_events() {
        this.ctx.canvas.focus();

        const [snake0, snake1] = this.snakes;
        this.ctx.canvas.addEventListener("keydown", e => {
            if (e.key === 'w') snake0.set_direction(0);
            else if (e.key === 'd') snake0.set_direction(1);
            else if (e.key === 's') snake0.set_direction(2);
            else if (e.key === 'a') snake0.set_direction(3);
            else if (e.key === 'ArrowUp') snake1.set_direction(0);
            else if (e.key === 'ArrowRight') snake1.set_direction(1);
            else if (e.key === 'ArrowDown') snake1.set_direction(2);
            else if (e.key === 'ArrowLeft') snake1.set_direction(3);
        });
    }

    start() {
        for (let i = 0; i < 1000; i++) {
            if (this.create_walls()) {
                break;
            }
        }
        this.add_listening_events();
    }

    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    //检查两条蛇的状态, 如果状态为静止或者方向没有指令那么就算没准备好
    check_ready() {
        for (const snake of this.snakes) {
            if (snake.status !== "idle")
                return false;
            if (snake.direction === -1)
                return false;
        }
        return true;
    }

    next_step() {

        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    check_valid(snakebody) {
        for (const wall of this.walls) {
            if (wall.r === snakebody.r && wall.c === snakebody.c) {
                return false;
            }
        }

        for (const snake of this.snakes) {
            let k = snake.snakebodys.length;
            if (!snake.check_tail_increasing()) {//如果当前判断回合蛇尾会前进, 即不会变长, 那么就不判断蛇尾
                k--;
            }

            for (let i = 0; i < k; i++) {
                if (snake.snakebodys[i].r === snakebody.r && snake.snakebodys[i].c === snakebody.c) {
                    return false;
                }
            }
        }

        return true;
    }

    update() {
        this.update_size();

        if (this.check_ready()) {
            this.next_step();
        }
        this.render();
    }

    render() {//渲染函数
        const color_even = '#AAD751';//偶数格子的颜色
        const color_odd = '#A2D149';//奇数格子的颜色

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if ((r + c) % 2 == 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L);
            }
        }
    }
}