
export default ({
    state: {
        status: "matching", //matching表示匹配界面, playing表示对战界面
        socket: null,
        opponent_username: "",
        opponent_photo: "",
        gamemap: null,
        a_id: 0,
        a_sx: 0,
        a_sy: 0,
        b_id: 0,
        b_sx: 0,
        b_sy: 0,
        gameObject: null,
        loser: "none",//none, all, A, B
        role: null,//当前玩家在游戏中的角色: "A" 或 "B"
    },
    getters: {
    },
    mutations: {
        updateSocket(state, socket) {
            state.socket = socket;
        },
        updateOpponent(state, opponent) {
            state.opponent_username = opponent.username;
            state.opponent_photo = opponent.photo;
        },
        updateStatus(state, status) {
            state.status = status;
        },
        updateGame(state, gamemap) {
            state.gamemap = gamemap.map;
            state.a_id = gamemap.a_id;
            state.a_sx = gamemap.a_sx;
            state.a_sy = gamemap.a_sy;
            state.b_id = gamemap.b_id;
            state.b_sx = gamemap.b_sx;
            state.b_sy = gamemap.b_sy;
        },
        updateGameObject(state, gameObject) {
            state.gameObject = gameObject;
        },
        updateLoser(state, loser) {
            state.loser = loser;
        },
        updateRole(state, role) {
            state.role = role;
        }
    },
    actions: {

    },
    modules: {
    }
})