<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useGlobalStore } from '@/stores/global'
import { useUserStore } from '@/stores/user'
import { RoomTypeEnum } from '@/enums'

import UserList from '../UserList/index.vue'
import ChatList from '../ChatList/index.vue'
import SendBar from './SendBar/index.vue'

const isSelect = ref(false)
const globalStore = useGlobalStore()
const userStore = useUserStore()
const currentSession = computed(() => globalStore.currentSession)

// 监听登录状态变化，确保UI及时更新
watch(() => userStore.isSign, (newValue) => {
  if (newValue) {
    isSelect.value = false
  }
}, { immediate: true })

// 页面加载时检查登录状态
onMounted(() => {
  // 如果有token但isSign为false，重新获取用户信息
  if (localStorage.getItem('TOKEN') && !userStore.isSign) {
    userStore.isSign = true
    userStore.getUserDetailAction()
  }
})
</script>

<template>
  <div class="chat-box">
    <div class="chat-wrapper">
      <template v-if="isSelect">
        <ElIcon :size="160" color="var(--font-light)"><IEpChatDotRound /></ElIcon>
      </template>
      <div v-else class="chat">
        <ChatList />
        <SendBar />
      </div>
    </div>
    <UserList v-show="currentSession.type === RoomTypeEnum.Group" />
  </div>
</template>

<style lang="scss" src="./styles.scss" scoped />
