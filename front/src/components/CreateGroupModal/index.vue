<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRequest } from 'alova'
import apis from '@/services/apis'
import { RoomTypeEnum } from '@/enums'
import { ElMessage } from 'element-plus'
import { useGlobalStore } from '@/stores/global'
import { useGroupStore } from '@/stores/group'
import { judgeClient } from '@/utils/detectDevice'
import SelectUser from './SelectUser.vue'

const client = judgeClient()
const globalStore = useGlobalStore()
const groupStore = useGroupStore()
const activeStep = ref(0)
const selectUser = ref<number[]>([])
const groupName = ref('')
const groupAvatar = ref('')

// 修复类型错误
interface CreateGroupParams {
  uidList: number[]
  name?: string
  avatar?: string
}

const { send, loading } = useRequest(apis.createGroup, { immediate: false })
const { send: invite, loading: inviteLoading } = useRequest(apis.inviteGroupMember, {
  immediate: false,
})

const show = computed(() => globalStore.createGroupModalInfo.show)
const isInvite = computed(() => globalStore.createGroupModalInfo.isInvite)
const actionText = computed(() => isInvite.value ? '邀请' : '创建')

// 关闭对话框并重置所有状态
const close = () => {
  // 批量更新状态，确保状态的一致性
  nextTick(() => {
    // 确保在下一次DOM更新循环后再设置下一组状态
    globalStore.createGroupModalInfo = {
      show: false,
      isInvite: false,
      selectedUid: []
    }
  })

  // 重置本地状态
  activeStep.value = 0
  selectUser.value = []
  groupName.value = ''
  groupAvatar.value = ''
}

// 验证选择并前进到下一步
const nextStep = () => {
  // 至少要选择一个用户才能进入下一步
  if (selectUser.value.length === 0) {
    ElMessage.warning('请至少选择一位好友')
    return
  }
  activeStep.value = 1
}

// 返回上一步（选择用户步骤）
const prevStep = () => {
  activeStep.value = 0
}

// 接收子组件传递的选中用户列表
const onChecked = (checked: number[]) => {
  // 直接更新本地状态
  selectUser.value = checked
}

// 添加状态跟踪
const uploading = ref(false)
const avatarFile = ref<File | null>(null)

// 修改文件处理函数
const handleFileChange = async (file: any) => {
  if (!file || !file.raw) return

  const rawFile = file.raw as File

  // 检查文件类型
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(rawFile.type)) {
    ElMessage.error('请上传图片格式文件')
    return
  }

  // 检查文件大小，限制为2MB
  if (rawFile.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }

  // 保存文件引用
  avatarFile.value = rawFile

  // 创建临时URL用于预览
  groupAvatar.value = window.URL.createObjectURL(rawFile)
}

// 添加上传头像函数
const uploadAvatar = async (): Promise<string | null> => {
  if (!avatarFile.value) return null

  try {
    uploading.value = true

    // 获取上传URL，场景为3（对应群组头像）
    const uploadData = await apis.getUploadUrl({
      fileName: avatarFile.value.name,
      scene: 3  // 3代表群组头像
    }).send()

    if (uploadData?.uploadUrl) {
      // 使用PUT方法上传文件到OSS
      const response = await fetch(uploadData.uploadUrl, {
        method: 'PUT',
        body: avatarFile.value
      })

      if (!response.ok) {
        throw new Error('头像上传失败')
      }

      return uploadData.downloadUrl
    }

    return null
  } catch (error) {
    console.error('群组头像上传失败:', error)
    ElMessage.error('群组头像上传失败，请稍后重试')
    return null
  } finally {
    uploading.value = false
  }
}

// 修改发送函数
const onSend = async () => {
  if (selectUser.value.length === 0) return

  try {
    if (isInvite.value) {
      // 邀请流程不变
      await invite({
        roomId: globalStore.currentSession.roomId,
        uidList: selectUser.value,
      })
      ElMessage.success('邀请成功')
      await groupStore.getGroupUserList(true)
    } else {
      // 构建基础参数
      const params: CreateGroupParams = {
        uidList: selectUser.value
      }

      // 设置群名称
      if (groupName.value.trim()) {
        params.name = groupName.value.trim()
      }

      // 如果有选择头像，先上传
      if (avatarFile.value) {
        const avatarUrl = await uploadAvatar()
        if (avatarUrl) {
          params.avatar = avatarUrl
        }
      }

      // 调用创建群组API
      const { id } = await send(params)
      ElMessage.success('群聊创建成功')

      globalStore.currentSession.roomId = id
      globalStore.currentSession.type = RoomTypeEnum.Group
    }
    close()
  } catch (error) {
    ElMessage.error('操作失败，请重试')
  }
}

</script>

<template>
  <ElDialog
    class="create-group-modal"
    :model-value="show"
    :width="client === 'PC' ? 650 : '80%'"
    :close-on-click-modal="false"
    center
    @close="close"
  >
    <template #header>
      <div class="dialog-title">{{ isInvite ? '邀请好友' : '创建群组' }}</div>
    </template>

    <!-- 第一步：选择用户 -->
    <div v-if="activeStep === 0" class="step-content">
      <SelectUser @checked="onChecked" />
      <div class="selected-count">
        已选择 <span class="highlight">{{ selectUser.length }}</span> 位好友
      </div>
    </div>

    <!-- 第二步：群组信息或邀请确认 -->
    <div v-else class="step-content">

      <!-- 邀请确认 -->
      <template v-if="isInvite">
        <div class="confirm-invite">
          <!-- 简化邀请确认界面 -->
          <p class="invite-summary">您即将邀请 {{ selectUser.length }} 位好友加入群聊</p>
        </div>
      </template>

      <!-- 创建群组信息 -->
      <template v-else>

        <div class="group-info-form">
          <el-form label-position="top">
            <el-form-item>
              <template #label>
                群组名称 <span class="optional-hint">(选填，不填写将使用默认名称：创建者的名字 + "的群组")</span>
              </template>
              <el-input
                v-model="groupName"
                placeholder="请输入群组名称"
                maxlength="20"
                show-word-limit
              />
            </el-form-item>
            <el-form-item>
              <template #label>
                群组头像 <span class="optional-hint">(选填)</span>
              </template>
              <div class="avatar-uploader">
                <el-upload
                  class="uploader"
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleFileChange"
                >
                  <div class="upload-area">
                    <img v-if="groupAvatar" :src="groupAvatar" class="preview" />
                    <el-icon v-else class="upload-icon"><i-ep-plus /></el-icon>
                    <span>点击上传</span>
                  </div>
                </el-upload>
              </div>

            </el-form-item>
          </el-form>
        </div>

      </template>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="activeStep === 0 ? close() : prevStep()">
          {{ activeStep === 0 ? '取消' : '返回' }}
        </el-button>
        <el-button
          type="primary"
          @click="activeStep === 0 ? nextStep() : onSend()"
          :loading="loading || inviteLoading"
          :disabled="activeStep === 0 ? selectUser.length === 0 : false"
        >
          {{ activeStep === 0 ? '下一步' : actionText }}
        </el-button>
      </span>
    </template>
  </ElDialog>
</template>

<style lang="scss" src="./styles.scss" scoped />
