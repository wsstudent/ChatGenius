<script setup lang="ts">
import { computed, ref } from 'vue'
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

const close = () => {
  globalStore.createGroupModalInfo.show = false
  globalStore.createGroupModalInfo.isInvite = false
  globalStore.createGroupModalInfo.selectedUid = []
  activeStep.value = 0
  selectUser.value = []
  groupName.value = ''
  groupAvatar.value = ''
}

const nextStep = () => {
  if (selectUser.value.length === 0) {
    ElMessage.warning('请至少选择一位好友')
    return
  }
  activeStep.value = 1
}

const prevStep = () => {
  activeStep.value = 0
}

const onSend = async () => {
  if (selectUser.value.length === 0) return

  if (!isInvite.value && !groupName.value.trim()) {
    ElMessage.warning('请输入群组名称')
    return
  }

  try {
    if (isInvite.value) {
      await invite({
        roomId: globalStore.currentSession.roomId,
        uidList: selectUser.value,
      })
      ElMessage.success('邀请成功')
      groupStore.getGroupUserList(true)
    } else {
      // 修复类型错误
      const params: CreateGroupParams = {
        uidList: selectUser.value
      }

      if (groupName.value.trim()) {
        params.name = groupName.value.trim()
      }

      if (groupAvatar.value) {
        params.avatar = groupAvatar.value
      }

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

const onChecked = (checked: number[]) => {
  selectUser.value = checked
}

// 修复URL类型错误
const handleFileChange = (file: any) => {
  if (file && file.raw) {
    groupAvatar.value = window.URL.createObjectURL(file.raw)
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
      <template v-if="isInvite">
        <div class="confirm-invite">
          <!-- 简化邀请确认界面 -->
          <p class="invite-summary">您即将邀请 {{ selectUser.length }} 位好友加入群聊</p>
        </div>
      </template>
      <template v-else>
        <div class="group-info-form">
          <el-form label-position="top">
            <el-form-item label="群组名称" required>
              <el-input
                v-model="groupName"
                placeholder="请输入群组名称"
                maxlength="20"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="群组头像">
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
