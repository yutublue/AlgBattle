<template>
  <ContentField>我的代码</ContentField>
</template>

<script>
import ContentField from '@/components/ContentField.vue'
import $ from 'jquery'
//import { useStore } from 'vuex'

export default {
  components: {
    ContentField
  },
  setup() {
    //const store = useStore();
    const jwt_token = localStorage.getItem("jwt_token");
    // $.ajax({
    //   url: "http://localhost:3000/user/bot/add/",
    //   type: "POST",
    //   data: {
    //     title: "bot的标题",
    //     description: "bot的描述",
    //     content: "bot的内容",
    //   },
    //   headers: {
    //     Authorization: "Bearer " + jwt_token,
    //   },
    //   success(resp) {
    //     console.log(resp)
    //   },
    //   error(resp) {
    //     console.log(resp)
    //   }
    // })

    // $.ajax({
    //   url: "http://localhost:3000/user/bot/remove/",
    //   type: "POST",
    //   data: {
    //     bot_id: 3
    //   },
    //   headers: {
    //     Authorization: "Bearer " + jwt_token,
    //   },
    //   success(resp) {
    //     console.log(resp)
    //   },
    //   error(resp) {
    //     console.log(resp)
    //   }
    // })

    // $.ajax({
    //   url: "http://localhost:3000/user/bot/update/",
    //   type: "POST",
    //   data: {
    //     bot_id: 2,
    //     title: "bot的标题2",
    //     description: "bot的描述2",
    //     content: "bot的内容2",
    //   },
    //   headers: {
    //     Authorization: "Bearer " + jwt_token,
    //   },
    //   success(resp) {
    //     console.log(resp)
    //   },
    //   error(resp) {
    //     console.log(resp)
    //   }
    // })

    $.ajax({
      url: "http://localhost:3000/user/bot/getlist/",
      type: "get",
      headers: {
        Authorization: "Bearer " + jwt_token,
      },
      success(resp) {
        console.log(resp)
      },
      error(resp) {
        console.log(resp)
      }
    })
  }
}

</script>

<style scoped></style>