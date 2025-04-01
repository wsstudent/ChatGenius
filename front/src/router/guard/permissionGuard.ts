import type { Router } from 'vue-router'

import { useUserStore } from '@/stores/user'

// 白名单，未登录用户可以访问
const whiteList: Array<string | RegExp> = ['/login']

const whiteListTest = (path: string) => {
  return whiteList.some((o) => {
    if (o instanceof RegExp) {
      return o.test(path)
    } else {
      return o === path
    }
  })
}

const createPermissionGuard = (router: Router) => {
  // 路由守卫
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const token = localStorage.getItem('TOKEN')

    // 检测token存在但未登录状态时，恢复登录状态
    if (token && !userStore.isSign) {
      userStore.isSign = true
      try {
        await userStore.getUserDetailAction() // 使用await确保获取到用户信息
        console.log('恢复用户登录状态成功')
      } catch (error) {
        console.error('恢复用户登录状态失败:', error)
        localStorage.removeItem('TOKEN')
        localStorage.removeItem('USER_INFO')
        userStore.isSign = false
        return next({ path: '/login', replace: true })
      }
    }

    // 登录状态控制
    if (whiteListTest(to.path) || userStore.isSign) {
      if (userStore.isSign && to.path === '/login') {
        return next({ path: '/' }) // 已登录用户不允许访问登录页
      }
      return next()
    } else {
      return next({ path: '/login', replace: true })
    }
  })
}



export default createPermissionGuard
