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
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const token = localStorage.getItem('TOKEN')

    // 检测token存在但未登录状态时，恢复登录状态
    if (token && !userStore.isSign) {
      userStore.isSign = true
      try {
        await userStore.getUserDetailAction()
        console.log('恢复用户登录状态成功')
      } catch (error) {
        console.error('恢复用户登录状态失败:', error)
        localStorage.removeItem('TOKEN')
        localStorage.removeItem('USER_INFO')
        userStore.isSign = false
        return next({ path: '/login', replace: true })
      }
    }

    // 1. 先处理白名单路由
    if (whiteListTest(to.path)) {
      // 已登录用户不允许访问登录页
      if (userStore.isSign && to.path === '/login') {
        return next({ path: '/', replace: true })
      }
      return next()
    }

    // 2. 未登录用户重定向到登录页
    if (!userStore.isSign) {
      return next({ path: '/login', replace: true })
    }

    // 3. 检查管理员权限
    if (to.meta.requiresAdmin && userStore.userInfo.power !== 2) {
      // console.log('需要管理员权限，重定向到首页')
      return next({ path: '/', replace: true })
    }

    // 4. 其他情况允许访问
    return next()
  })
}

export default createPermissionGuard