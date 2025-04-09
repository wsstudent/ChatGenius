<script setup lang="ts" name="SelectUser">
import { ref, watch, computed, nextTick } from 'vue'
import Avatar from '@/components/Avatar/index.vue'

import { useGlobalStore } from '@/stores/global'
import { useUserInfo } from '@/hooks/useCached'
import { useContactStore } from '@/stores/contacts'


const globalStore = useGlobalStore()  // 获取全局存储
const contactStore = useContactStore()  // 获取联系人存储
const contactsList = computed(() => contactStore.contactsList)  // 获取联系人列表
const selectedUid = computed(() => globalStore.createGroupModalInfo.selectedUid)  // 获取选中的联系人



const selected = ref<number[]>([])  // 选中的联系人列表


/**
 * 选择用户组件
 * @param {string} searchQuery - 搜索框的值
 * @param {number[]} selected - 选中的联系人列表
 * @param {function} toggleSelection - 切换选中状态的方法
 * @param {function} isSelected - 判断是否选中的方法
 * @param {function} isDisabled - 判断是否禁用的方法
 * @param {function} getUserName - 获取用户名的方法
 * @param {function} getUserAvatar - 获取用户头像的方法
 */

// 为每个联系人提前获取用户信息

const userInfoMap = computed(() => {
  const map: Record<number, any> = {}
  if (contactsList.value) {
    contactsList.value.forEach(contact => {
      const userInfo = useUserInfo(contact.uid)
      map[contact.uid] = userInfo.value
    })
  }
  return map
})

// 搜索框的过滤逻辑
const searchQuery = ref('')  // 搜索框的值
const filteredContacts = computed(() => {
  if (!contactsList.value) return []

  return contactsList.value.filter(item => {
    const userInfo = useUserInfo(item.uid)
    return !searchQuery.value ||
      (userInfo.value && userInfo.value.name &&
        userInfo.value.name.toLowerCase().includes(searchQuery.value.toLowerCase()))
  })
})

// 确保联系人加载
watch(
  contactsList,
  () => {
    if (!contactsList.value || contactsList.value.length === 0) {
      contactStore.getContactList()
    }
  },
  { immediate: true }
)

// 监听对话框显示状态，根据模式设置初始选择
// 在 SelectUser.vue 中简化监听器
watch(
  () => globalStore.createGroupModalInfo.show,
  (newShow) => {
    if (newShow) {
      // 无论哪种模式都初始化为空选择
      selected.value = []

      // 初始化后通知父组件（空选择）
      nextTick(() => {
        emit('checked', selected.value)
      })
    }
  },
  { immediate: true }
)

// 判断用户是否禁用选择（已在群组中的成员）
const isDisabled = (uid: number) => {
  return globalStore.createGroupModalInfo.isInvite &&
    globalStore.createGroupModalInfo.selectedUid.includes(uid)
}

// 切换用户选择状态（选中/取消选中）
const emit = defineEmits(['checked'])
const toggleSelection = (uid: number) => {
  // 如果用户已禁用，不允许选择
  if (isDisabled(uid)) return

  const index = selected.value.indexOf(uid)
  if (index === -1) {
    // 如果不在已选列表中，添加到列表
    selected.value.push(uid)
  } else {
    // 如果已在列表中，从列表移除
    selected.value.splice(index, 1)
  }
  // 通知父组件选择状态变化
  // 这里的emit正常工作，因为它只在用户交互时触发一次
  emit('checked', selected.value)
}

// 判断用户是否已被选中
const isSelected = (uid: number) => {
  return selected.value.includes(uid)
}


// 获取用户名的安全方法
const getUserName = (uid: number): string => {
  const userInfo = userInfoMap.value[uid]
  return userInfo?.name || `用户${uid}`
}

// 获取用户头像的安全方法
const getUserAvatar = (uid: number): string => {
  const userInfo = userInfoMap.value[uid]
  return userInfo?.avatar || ''
}

</script>

<template>
 <div class="select-user-container">
      <!-- 搜索框 -->
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索好友"
          clearable
        >
            <template #prepend>
              <el-icon><i-ep-search /></el-icon>
            </template>
        </el-input>
      </div>

      <!-- 联系人列表 -->
      <div class="contacts-list">
        <div v-if="filteredContacts.length === 0" class="no-results">
          没有找到匹配的好友
        </div>
        <div
          v-for="contact in filteredContacts"
          :key="contact.uid"
          class="contact-item"
          :class="{
          'selected': isSelected(contact.uid),
          'disabled': isDisabled(contact.uid)
        }"
          @click="!isDisabled(contact.uid) && toggleSelection(contact.uid)"
        >
          <div class="contact-avatar">
            <!-- 同时传递 uid 和 src，增加渲染成功率 -->
            <Avatar
              :uid="contact.uid"
              :src="getUserAvatar(contact.uid)"
              :size="40"
            />
            <div v-if="isSelected(contact.uid)" class="check-mark">
              <el-icon><i-ep-check /></el-icon>
            </div>
          </div>
          <div class="contact-info">
            <div class="contact-name">{{ getUserName(contact.uid) }}</div>
          </div>
        </div>
      </div>

 </div>
</template>

<style lang="scss" src="./styles.scss" scoped />
