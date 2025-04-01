import { ref } from 'vue'
import apis from '@/services/apis'
import { defineStore } from 'pinia'
import type { UserInfoType } from '@/services/types'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<Partial<UserInfoType>>({})
  // 初始化时根据localStorage中是否有TOKEN来设置登录状态
  const isSign = ref(!!localStorage.getItem('TOKEN'))

  // 从本地存储获取缓存的用户信息
  let localUserInfo = {}
  try {
    localUserInfo = JSON.parse(localStorage.getItem('USER_INFO') || '{}')
  } catch (error) {
    localUserInfo = {}
  }

  // 从本地存储恢复用户信息
  if (!Object.keys(userInfo.value).length && Object.keys(localUserInfo).length) {
    userInfo.value = localUserInfo
  }

  // 获取用户详情的方法
  function getUserDetailAction() {
    apis
      .getUserDetail()
      .send()
      .then((data) => {
        userInfo.value = { ...userInfo.value, ...data }
      })
      .catch(() => {
        // 请求失败时清除缓存
        localStorage.removeItem('TOKEN')
        localStorage.removeItem('USER_INFO')
        // 清除登录状态
        isSign.value = false
      })
  }

  return { userInfo, isSign, getUserDetailAction }
})