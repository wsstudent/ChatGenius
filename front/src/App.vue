<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted } from 'vue'
// 全局导入 toast 样式
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
// element dark 模式
import 'element-plus/theme-chalk/dark/css-vars.css'

// 初始化背景
onMounted(() => {
  // 加载保存的背景设置
  const savedBg = localStorage.getItem('preferred-login-bg');
  if (savedBg) {
    document.querySelector('.app-wrapper')?.classList.add(savedBg);

    // 如果是自定义背景，需要加载CSS样式
    if (savedBg.startsWith('custom-bg-')) {
      const customBackgrounds = localStorage.getItem('custom-backgrounds');
      if (customBackgrounds) {
        try {
          const backgrounds = JSON.parse(customBackgrounds);
          const bg = backgrounds.find((b: any) => b.class === savedBg);
          if (bg) {
            const styleElement = document.createElement('style');
            styleElement.textContent = `
              .app-wrapper.${savedBg} {
                background-image: url('${bg.url}');
              }
            `;
            document.head.appendChild(styleElement);
          }
        } catch (e) {
          console.error('加载自定义背景失败', e);
        }
      }
    }
  }
})
</script>

<template>
  <div class="app-wrapper">
    <RouterView />
  </div>
</template>

<style scoped>
.app-wrapper {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-image: url('@/assets/login_bg.jpg');
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  transition: background-image 0.5s ease;
}

/* 预设背景 */
.app-wrapper.bg-1 {
  background-image: url('@/assets/login_bg.jpg');
}

.app-wrapper.bg-2 {
  background-image: url('@/assets/bg.jpg');
}

.app-wrapper.bg-3 {
  background-image: url('@/assets/login_bg2.jpg');
}
</style>