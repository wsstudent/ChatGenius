<template>
  <!-- 前后端共用登录页面 -->
  <div id="body-bg">
    <!--页面加载-->
    <div
      v-if="fullscreenLoading"
      v-loading.fullscreen.lock="fullscreenLoading"
      element-loading-text="Loading..."
      :element-loading-svg="svg"
      element-loading-background="rgba(0, 0, 0, 0.3)"
      element-loading-svg-view-box="-10, -10, 50, 50"
    >
    </div>

    <!-- 使用 v-show 和 transition 实现平滑过渡 -->
    <transition name="fade">
      <div v-show="contentVisible" class="container">
        <span></span>
        <span></span>
        <span></span>
        <el-form ref="ruleFormRef" :model="ruleForm" status-icon :rules="rules" class="loginForm">
          <h2>登录即可使用</h2>
          <el-form-item prop="username">
            <div>
              <input
                v-model="ruleForm.username"
                type="text"
                placeholder="学号"
                class="custom-input"
              />
            </div>
          </el-form-item>
          <el-form-item prop="password">
            <div>
              <input
                v-model="ruleForm.password"
                type="password"
                placeholder="密码"
                class="custom-input"
              />
            </div>
          </el-form-item>
          <!--          <el-form-item prop="role">-->
          <!--            <div>-->
          <!--              <select v-model="ruleForm.role" id="status" class="custom-select">-->
          <!--                <option value="TEACHER">教师</option>-->
          <!--                <option value="STUDENT">学生</option>-->
          <!--              </select>-->
          <!--            </div>-->
          <!--          </el-form-item>-->

          <div class="inputBox">
            <el-button @click="submitForm(ruleFormRef)">登 录</el-button>
          </div>
        </el-form>
        '/',
      </div>
    </transition>
  </div>
  <!-- 添加 GitHub 水印链接 -->
  <a href="https://github.com/wsstudent" target="_blank" class="github-link">
    <div class="github-watermark">
      <span>Developed by</span>
      <svg height="24" width="24" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
        <path
          d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"
        ></path>
      </svg>
    </div>
  </a>
</template>

<script lang="ts" setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import wsIns from '@/utils/websocket'
import { WsRequestMsgType } from '@/utils/wsType'

// 全屏加载状态
const fullscreenLoading = ref(true)
const resourcesLoaded = ref(false)
// 控制内容显示的新状态
const contentVisible = ref(false)

// 监听页面资源加载情况
onMounted(async () => {
  /**
   * 这里使用 nextTick() 是为了：
   * 避免闪烁：确保加载动画元素（v-loading.fullscreen.lock="fullscreenLoading"）已完全渲染到 DOM 中
   * 顺序保证：先确保 DOM 更新完成，再开始检测背景图片等资源的加载情况
   * 平滑过渡：当背景图片等资源加载完成后，能够平滑切换到实际内容，不会出现渲染不完整或闪烁的情况
   * 如果没有 nextTick()，可能会在 DOM 尚未完全准备好的情况下开始资源检测，导致不理想的用户体验或视觉异常
   */
  await nextTick()

  // 先显示背景图
  document.getElementById('body-bg')?.classList.add('loaded')

  // 预加载背景图
  const backgroundImage = new Image()
  backgroundImage.src = '../../assets/imgs/bg.jpg'

  // 设置背景图加载完成后的回调
  backgroundImage.onload = backgroundImage.onerror = async () => {
    // 背景图加载完成，现在等待容器渲染
    // 等待容器元素渲染
    await nextTick()

    // 短暂延迟确保DOM渲染完成
    setTimeout(() => {
      // 设置容器可见
      contentVisible.value = true

      // 再延迟一段时间后关闭加载动画，确保过渡效果顺畅
      setTimeout(() => {
        fullscreenLoading.value = false
      }, 50)
    }, 100)
  }

  // 设置超时保护
  setTimeout(() => {
    if (fullscreenLoading.value) {
      contentVisible.value = true
      setTimeout(() => {
        fullscreenLoading.value = false
      }, 500)
      console.warn('资源加载超时，强制显示页面')
    }
  }, 5000) // 5秒超时保护
})

// 加载图标的SVG
const svg = `
        <path class="path" d="
          M 30 15
          L 28 17
          M 25.61 25.61
          A 15 15, 0, 0, 1, 15 30
          A 15 15, 0, 1, 1, 27.99 7.5
          L 15 15
        " style="stroke-width: 4px; fill: rgba(0, 0, 0, 0)"/>
      `

// 登录表单验证
const ruleFormRef = ref<FormInstance>()
const checkUsername = (rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('请输入用户名'))
  }
  callback()
}
const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  }
  callback()
}

const ruleForm = reactive({
  username: '',
  password: '',
  // role: ''
})
const rules = reactive<FormRules<typeof ruleForm>>({
  username: [{ validator: checkUsername, trigger: 'blur' }],
  password: [{ validator: validatePass, trigger: 'blur' }],
  // role: [{ required: true, message: '请选择身份', trigger: 'change' }]
})

// 登录接口以及后台验证成功后的路由跳转
// const router = useRouter()
// const submitForm = async (formEl: FormInstance | undefined) => {
//   if (!formEl) return
//   await formEl.validate(async (valid) => {
//     if (valid) {
//       try {
//         const response = await request.post('/login', ruleForm)
//         if (response.code === '200') {
//           localStorage.setItem('xm-user', JSON.stringify(response.data))
//           ElMessage.success('登录成功')
//
//           // 根据角色跳转到不同页面
//           if (ruleForm.role === 'TEACHER') {
//             await router.push('/backend/main')
//           } else {
//             await router.push('/')  // 学生界面
//           }
//         }else {
//           ElMessage.error(response.msg)
//         }
//       } catch (error) {
//         console.error('登录失败:', error)
//         ElMessage.error('登录失败，请重试')
//       }
//     } else {
//       console.log('error submit!')
//     }
//   })
// }

// 登录处理
const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        ElMessage.success('登录验证中，请稍候...')
        // 使用WebSocket发送密码登录请求
        wsIns.send({
          type: WsRequestMsgType.PASSWORD_LOGIN,
          data: {
            username: ruleForm.username,
            password: ruleForm.password,
          },
        })
        // 超时检测代码
        // 保存到全局变量
        window.loginTimeoutId = setTimeout(() => {
          if (!localStorage.getItem('TOKEN')) {
            ElMessage.warning('登录响应超时，请重试')
          }
        }, 5000)
      } catch (error) {
        console.error('登录请求失败:', error)
        ElMessage.error('登录请求失败，请重试')
      }
    }
  })
}
</script>

<style lang="scss" src="./styles.scss" scoped />
