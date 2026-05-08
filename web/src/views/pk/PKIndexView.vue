<template>
  <PlayGround v-if="$store.state.pk.status === 'playing'" />
  <MatchGround v-if="$store.state.pk.status === 'matching'" />
  <ResultBoard v-if="$store.state.pk.loser != 'none'" />

</template>

<script>
import PlayGround from '@/components/PlayGround.vue'
import MatchGround from '@/components/MatchGround.vue'
import ResultBoard from '@/components/ResultBoard.vue'
import { onMounted, onUnmounted } from "vue";
import { useStore } from "vuex";

export default {
  components: {
    PlayGround,
    MatchGround,
    ResultBoard,
  },
  setup() {
    const store = useStore();
    const socketUrl = `ws://127.0.0.1:3000/websocket/${store.state.user.token}/`;

    store.commit("updateLoser", "none");
    store.commit("updateIsRecord", false);
    store.commit("updateRole", null);

    let socket = null;
    onMounted(() => {
      store.commit("updateOpponent", {
        username: "我的对手",
        photo: "https://cdn.acwing.com/media/article/image/2022/08/09/1_1db2488f17-anonymous.png",
      })

      socket = new WebSocket(socketUrl);

      socket.onopen = () => {
        console.log("connected!");
        store.commit("updateSocket", socket);
      }

      socket.onmessage = msg => {
        const data = JSON.parse(msg.data);
        if (data.event === "start-matching") {

          store.commit("updateOpponent", {
            username: data.opponent_username,
            photo: data.opponent_photo,
          });

          setTimeout(() => {
            store.commit("updateStatus", "playing");
          }, 200);

          store.commit("updateGame", data.game);
          store.commit("updateRole", data.role);
        } else if (data.event === "move") {
          const game = store.state.pk.gameObject;
          game.moveQueue.push({
            a_direction: data.a_direction,
            b_direction: data.b_direction
          });
        } else if (data.event === "result") {
          const game = store.state.pk.gameObject;
          const [snake0, snake1] = game.snakes;

          game.moveQueue = []; // 游戏结束，清空未处理的队列

          if(data.loser === "all" || data.loser === "A") {
            snake0.status = 'die';
          }
          if(data.loser === "all" || data.loser === "B") {
            snake1.status = 'die';
          }
          store.commit("updateLoser", data.loser);
        }
      }

      socket.onclose = () => {
        console.log("disconnected!");
        store.commit("updateStatus", "matching");
        store.commit("updateRole", null);
      }
    });

    onUnmounted(() => {
      socket.close();
    })
  }
}

</script>

<style scoped></style>