const GAME_OBJECTS = [];

export class GameObject {
    constructor() {
        GAME_OBJECTS.push(this);
        this.timedelta = 0;//这一帧与上一帧之间的时间间隔
        this.has_called_start = false; //当前对象是否执行过
    }

    start() {

    }

    update() {

    }

    on_destroy() {

    }

    destroy() {
        this.on_destroy();

        for (let i in GAME_OBJECTS) {
            const obj = GAME_OBJECTS[i];
            if (obj === this) {
                GAME_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp;//上一次执行的时间

const step = timestamp => {//timestamp是当前执行的时间
    for (let obj of GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            obj.timedelta = timestamp - last_timestamp;
            obj.update();
        }
    }

    requestAnimationFrame(step);
}

requestAnimationFrame(step)