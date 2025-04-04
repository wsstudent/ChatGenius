<template>
  <ElDialog
    class="setting-box-modal"
    v-model="value"
    :width="client === 'PC' ? 580 : '85%'"
    :close-on-click-modal="false"
    center
  >
    <div class="setting-box">
      <div class="setting-avatar-box">
        <ElAvatar
          size="large"
          class="setting-avatar"
          :src="userInfo?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
        />
        <!-- 添加头像上传覆盖层 -->
        <div class="avatar-upload-mask" @click="triggerAvatarUpload">
          <el-icon><Upload /></el-icon>
          <span>更换头像</span>
        </div>
        <!-- 隐藏的文件上传输入框 -->
        <input
          ref="avatarInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleAvatarChange"
        />
        <el-icon
          size="20"
          color="var(--font-main)"
          class="setting-avatar-sex"
          v-if="userInfo.sex && [SexEnum.MAN, SexEnum.REMALE].includes(userInfo.sex)"
          :style="{
            backgroundColor: `var(${
              userInfo.sex === SexEnum.MAN ? '--avatar-sex-bg-man' : '--avatar-sex-bg-female'
            })`,
          }"
        >
          <Female v-if="userInfo.sex === SexEnum.MAN" />
          <Male v-if="userInfo.sex === SexEnum.REMALE" />
        </el-icon>
      </div>

      <div class="setting-name">
        <div class="name-edit-wrapper" v-show="editName.isEdit === false">
          <span class="user-name">
            <el-tooltip effect="dark" :content="currentBadge?.describe" placement="top">
              <img class="setting-badge" :src="currentBadge?.img" v-show="currentBadge" />
            </el-tooltip>
            {{ userInfo.name || '-' }}
          </span>
          <el-button
            class="name-edit-icon"
            size="small"
            :icon="EditPen"
            circle
            @click="onEditName"
          />
        </div>

        <div class="name-edit-input-wrapper" v-show="editName.isEdit">
          <el-input
            v-model="editName.tempName"
            size="small"
            maxlength="12"
            show-word-limit
            placeholder="请输入昵称"
          />
          <div class="name-edit-action">
            <el-button
              class="action-btn"
              size="small"
              circle
              :icon="CloseBold"
              @click="onCancelEditName"
            />
            <el-button
              class="action-btn"
              size="small"
              type="primary"
              :loading="editName.saving"
              circle
              :icon="Select"
              @click="onSaveUserName"
            />
          </div>
        </div>
      </div>

      <ul class="badge-list">
        <li class="badge-item" v-for="badge of badgeList" :key="badge.id">
          <div class="badge-info">
            <el-badge class="badge-item-dot" :hidden="badge.wearing !== IsYetEnum.YES" dot>
              <div class="badge-img">
                <img :src="badge.img" alt="" />
              </div>
            </el-badge>
            <div class="badge-text">
              <div class="badge-name">{{ badge.name || '' }}</div>
              <div class="badge-des">{{ badge.describe || '' }}</div>
            </div>
          </div>

          <div class="badge-action" v-if="badge.obtain === IsYetEnum.YES">
            <el-button
              v-if="badge.wearing !== IsYetEnum.YES"
              type="primary"
              @click="toggleWarningBadge(badge)"
              >佩戴</el-button
            >
            <el-button v-else @click="toggleWarningBadge(badge)">卸下</el-button>
          </div>
        </li>
      </ul>
    </div>
  </ElDialog>
</template>


<script setup lang="ts">
import { computed, reactive, ref, watchEffect } from 'vue'
import { useRequest } from 'alova'
import { ElMessage } from 'element-plus'
import { Select, CloseBold, EditPen, Upload, Female, Male } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useCachedStore } from '@/stores/cached'
import { SexEnum, IsYetEnum } from '@/enums'
import type { BadgeType } from '@/services/types'
import apis from '@/services/apis'
import { judgeClient } from '@/utils/detectDevice'

const client = judgeClient()

const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const value = computed({
  get() {
    return props.modelValue
  },
  set(value) {
    emit('update:modelValue', value)
  },
})

const editName = reactive({
  isEdit: false,
  tempName: '',
  saving: false,
})

const userStore = useUserStore()
const cachedStore = useCachedStore()

const userInfo = computed(() => userStore.userInfo)
const { send: handlerGetBadgeList, data: badgeList } = useRequest(apis.getBadgeList, {
  initialData: [],
  immediate: false,
})

watchEffect(() => {
  if (value.value) {
    handlerGetBadgeList()
    userStore.getUserDetailAction()
  }
})

const currentBadge = computed(() =>
  badgeList.value.find((item) => item.obtain === IsYetEnum.YES && item.wearing === IsYetEnum.YES),
)

// 佩戴卸下徽章
const toggleWarningBadge = async (badge: BadgeType) => {
  if (!badge?.id) return
  await apis.setUserBadge(badge.id).send()
  handlerGetBadgeList()
  badge.img && (userInfo.value.badge = badge.img)
  updateCurrentUserCache('wearingItemId', badge.id) // 更新缓存里面的用户徽章
}

/** ------------------头像上传相关-----------------------------**/
const avatarInputRef = ref<HTMLInputElement | null>(null)

const triggerAvatarUpload = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = async (event: Event) => {
  const files = (event.target as HTMLInputElement).files
  if (!files || files.length === 0) return

  const file = files[0]

  // 检查文件类型
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    ElMessage.error('请上传图片格式文件')
    return
  }

  // 检查文件大小，限制为2MB
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  const fileName = file.name

  try {
    ElMessage.info('头像上传中...')

    // 获取上传URL，注意scene应该是0（对应后端的AVATAR枚举）
    const uploadData = await apis.getUploadUrl({
      fileName,
      scene: 0  // 使用正确的场景值，0代表头像
    }).send()

    if (uploadData?.uploadUrl) {
      // 使用PUT方法上传文件到OSS
      const response = await fetch(uploadData.uploadUrl, {
        method: 'PUT',  // 修改为PUT方法
        body: file
      })

      if (!response.ok) {
        throw new Error('头像上传失败')
      }

      // 调用更新用户头像API
      await apis.modifyUserAvatar(uploadData.downloadUrl).send()

      // 更新本地状态和缓存
      userStore.userInfo.avatar = uploadData.downloadUrl
      updateCurrentUserCache('avatar', uploadData.downloadUrl)
      ElMessage.success('头像修改成功')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请稍后重试')
  } finally {
    // 清空文件选择器
    if (avatarInputRef.value) {
      avatarInputRef.value.value = ''
    }
  }
}

// 更新缓存函数
const updateCurrentUserCache = (key: 'name' | 'wearingItemId' | 'avatar', value: any) => {
  const currentUser = userStore.userInfo.uid && cachedStore.userCachedList[userStore.userInfo.uid]
  if (currentUser) {
    currentUser[key] = value // 更新缓存里面的用户信息
  }
}

// 修改用户名相关
const onEditName = () => {
  editName.isEdit = true
  editName.tempName = userInfo.value.name || ''
}

const onCancelEditName = () => {
  editName.isEdit = false
  editName.tempName = ''
  editName.saving = false
}

const onSaveUserName = async () => {
  if (!editName.tempName || editName.tempName.trim() === '') {
    ElMessage.warning('用户名不能为空哦~')
    return
  }
  if (editName.tempName === userInfo.value.name) {
    ElMessage.warning('用户名和当前一样哦~')
    return
  }

  editName.saving = true

  try {
    await apis.modifyUserName(editName.tempName).send()
    userStore.userInfo.name = editName.tempName
    updateCurrentUserCache('name', editName.tempName)
    ElMessage.success('用户名修改成功')
    onCancelEditName()
  } catch (error) {
    console.error('修改用户名失败:', error)
  } finally {
    editName.saving = false
  }
}
</script>

<style lang="scss" src="./styles.scss" scoped />