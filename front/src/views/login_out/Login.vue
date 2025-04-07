<template>
  <!-- 前后端共用登录页面 -->
  <div id="body-bg" :class="currentBg">
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

          <div class="inputBox">
            <el-button @click="submitForm(ruleFormRef)">登 录</el-button>
          </div>
        </el-form>
      </div>
    </transition>

    <!-- 添加背景选择器 -->
    <div class="bg-selector">
      <!-- 预设背景选项 -->
      <div
        v-for="bg in backgrounds"
        :key="bg.id"
        :class="['bg-option', { active: currentBg === bg.class }]"
        :style="{ backgroundImage: `url(${bg.thumbnail})` }"
        @click="changeBg(bg.class)"
      ></div>

      <!-- 自定义背景选项 -->
      <div
        v-for="bg in customBackgrounds"
        :key="bg.id"
        :class="['bg-option', { active: currentBg === bg.class }]"
        :style="{ backgroundImage: `url(${bg.thumbnail})` }"
        @click="changeBg(bg.class)"
      ></div>

      <!-- 上传按钮 -->
      <div class="bg-upload" @click="triggerUpload">
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display: none"
        @change="handleFileUpload"
      />
    </div>
  </div>

  <!-- 添加 GitHub 水印链接 -->
  <a href="https://github.com/wsstudent/ChatGenius" target="_blank" class="github-link">
    <div class="github-watermark">
      <span>Powered by</span>
      <svg height="24" width="24" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
        <path
          d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"
        ></path>
      </svg>
    </div>
  </a>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import wsIns from '@/utils/websocket'
import { WsRequestMsgType } from '@/utils/wsType'

// 背景切换功能
const currentBg = ref('bg-1');
const backgrounds = reactive([
  { id: 1, class: 'bg-1', thumbnail: new URL('../../assets/login_bg.jpg', import.meta.url).href },
  { id: 2, class: 'bg-2', thumbnail: new URL('../../assets/bg.jpg', import.meta.url).href },
  { id: 3, class: 'bg-3', thumbnail: new URL('../../assets/login_bg2.jpg', import.meta.url).href }
]);

// 用户自定义背景
const customBackgrounds = ref<{id: number, class: string, url: string, thumbnail: string}[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);

// 触发文件上传对话框
const triggerUpload = () => {
  fileInput.value?.click();
};

// 处理文件上传
const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  // 检查文件类型和大小
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件');
    return;
  }

  if (file.size > 5 * 1024 * 1024) { // 5MB限制
    ElMessage.error('图片大小不能超过5MB');
    return;
  }

  // 将文件转换为Data URL
  const reader = new FileReader();
  reader.onload = (e) => {
    if (!e.target?.result) return;

    // 创建新的背景选项
    const imageUrl = e.target.result as string;
    const newBgId = Date.now(); // 使用时间戳作为ID
    const bgClass = `custom-bg-${newBgId}`;

    // 添加到自定义背景列表
    customBackgrounds.value.push({
      id: newBgId,
      class: bgClass,
      url: imageUrl,
      thumbnail: imageUrl
    });

    // 保存到本地存储
    saveCustomBackgrounds();

    // 添加CSS样式
    addCustomBackgroundStyle(bgClass, imageUrl);

    // 切换到新上传的背景
    changeBg(bgClass);

    ElMessage.success('背景已上传并应用');
  };

  reader.readAsDataURL(file);

  // 重置 input 以便再次选择同一文件
  target.value = '';
};

// 添加自定义背景的CSS样式
const addCustomBackgroundStyle = (className: string, imageUrl: string) => {
  const styleElement = document.createElement('style');
  styleElement.textContent = `
    #body-bg.${className}::before {
      background-image: url('${imageUrl}');
    }
    .app-wrapper.${className} {
      background-image: url('${imageUrl}');
    }
    #body-bg.${className} .container span::before,
    #body-bg.${className} .container span::after {
      background-image: url('${imageUrl}');
    }
  `;
  document.head.appendChild(styleElement);
};

// 保存自定义背景到本地存储
const saveCustomBackgrounds = () => {
  localStorage.setItem('custom-backgrounds', JSON.stringify(customBackgrounds.value));
};

// 加载自定义背景
const loadCustomBackgrounds = () => {
  const saved = localStorage.getItem('custom-backgrounds');
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      customBackgrounds.value = parsed;

      // 为每个自定义背景添加CSS样式
      customBackgrounds.value.forEach(bg => {
        addCustomBackgroundStyle(bg.class, bg.url);
      });
    } catch (e) {
      console.error('加载自定义背景失败', e);
    }
  }
};

// 合并预设背景和自定义背景
const allBackgrounds = computed(() => {
  return [...backgrounds, ...customBackgrounds.value];
});

const changeBg = (bgClass: string) => {
  currentBg.value = bgClass;

  // 更新当前页面背景
  document.getElementById('body-bg')?.classList.forEach(cls => {
    if (cls.startsWith('bg-') || cls.startsWith('custom-bg-')) {
      document.getElementById('body-bg')?.classList.remove(cls);
    }
  });
  document.getElementById('body-bg')?.classList.add(bgClass);

  // 更新全局背景
  document.querySelector('.app-wrapper')?.classList.forEach(cls => {
    if (cls.startsWith('bg-') || cls.startsWith('custom-bg-')) {
      document.querySelector('.app-wrapper')?.classList.remove(cls);
    }
  });
  document.querySelector('.app-wrapper')?.classList.add(bgClass);

  // 保存用户选择到本地存储
  localStorage.setItem('preferred-login-bg', bgClass);
};

// 全屏加载状态
const fullscreenLoading = ref(true)
const resourcesLoaded = ref(false)
// 控制内容显示的新状态
const contentVisible = ref(false)

// 监听页面资源加载情况
onMounted(async () => {
  await nextTick()

  // 先显示背景图
  document.getElementById('body-bg')?.classList.add('loaded')

  // 加载自定义背景
  loadCustomBackgrounds();

  // 加载保存的背景设置
  const savedBg = localStorage.getItem('preferred-login-bg');
  if (savedBg) {
    // 检查是否是有效的背景类名
    const isValid = [...backgrounds, ...customBackgrounds.value]
                    .some(bg => bg.class === savedBg);

    if (isValid) {
      changeBg(savedBg);
    }
  }

  // 预加载背景图
  const backgroundImage = new Image()
  backgroundImage.src = '../../assets/bg.jpg'

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
})

const rules = reactive<FormRules<typeof ruleForm>>({
  username: [{ validator: checkUsername, trigger: 'blur' }],
  password: [{ validator: validatePass, trigger: 'blur' }],
})

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