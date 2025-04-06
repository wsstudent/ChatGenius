<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import apis from '@/services/apis'
import { ElMessage, ElMessageBox } from 'element-plus'

// 定义用户类型接口
interface UserInfoType {
  id: number | null;
  username: string;
  password?: string;
  name: string;
  avatar: string;
  sex: number;
}

const userStore = useUserStore()
const userList = ref<UserInfoType[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const formData = ref<UserInfoType>({
  id: null,
  username: '',
  password: '',
  name: '',
  avatar: '',
  sex: 1
})
const isEdit = computed(() => !!formData.value.id)
const useDefaultPassword = ref(true)
const defaultPassword = ref('123456')

// 判断当前用户是否为超级管理员
const isAdmin = computed(() => userStore.userInfo.power === 2)

// 获取用户列表
const getUserList = async () => {
  if (!isAdmin.value) return
  loading.value = true
  try {
    const res = await apis.getUserList().send()
    userList.value = res
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 新增/编辑用户
const handleSave = async () => {
  if (!isAdmin.value) return

  // 如果使用默认密码，则清空密码字段，后端会使用默认密码
  if (useDefaultPassword.value && !isEdit.value) {
    formData.value.password = defaultPassword.value
  }

  try {
    if (isEdit.value) {
      await apis.updateUser(formData.value).send()
      ElMessage.success('修改用户成功')
    } else {
      await apis.addUser(formData.value).send()
      ElMessage.success('新增用户成功')
    }
    dialogVisible.value = false
    getUserList()
  } catch (error) {
    ElMessage.error(isEdit.value ? '修改用户失败' : '新增用户失败')
  }
}

// 删除用户
const handleDelete = (id: number) => {
  if (!isAdmin.value) return

  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await apis.deleteUser({ id }).send()
      ElMessage.success('删除用户成功')
      getUserList()
    } catch (error) {
      ElMessage.error('删除用户失败')
    }
  })
}

// 打开新增对话框
const openAddDialog = () => {
  useDefaultPassword.value = true
  formData.value = {
    id: null,
    username: '',
    password: defaultPassword.value,
    name: '',
    avatar: '',
    sex: 1
  }
  dialogVisible.value = true
}

// 打开编辑对话框
const openEditDialog = (row: UserInfoType) => {
  formData.value = { ...row }
  dialogVisible.value = true
}

onMounted(() => {
  if (isAdmin.value) {
    getUserList()
  }
})
</script>

<template>
  <div class="user-management" v-if="isAdmin">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="openAddDialog">新增用户</el-button>
    </div>

    <el-table :data="userList" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="昵称" />
      <el-table-column label="头像" width="100">
        <template #default="scope">
          <el-avatar :src="scope.row.avatar" />
        </template>
      </el-table-column>
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          {{ scope.row.sex === 1 ? '男' : '女' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="openEditDialog(scope.row)">编辑</el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(scope.row.id)"
            :disabled="scope.row.id === 1">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :title="isEdit ? '编辑用户' : '新增用户'"
      v-model="dialogVisible"
      width="500px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="formData.username" />
        </el-form-item>

        <template v-if="!isEdit">
          <el-form-item label="密码设置">
            <el-radio-group v-model="useDefaultPassword">
              <el-radio :label="true">使用默认密码</el-radio>
              <el-radio :label="false">自定义密码</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="默认密码" v-if="useDefaultPassword">
            <el-input v-model="defaultPassword" placeholder="请输入默认密码" />
          </el-form-item>

          <el-form-item label="密码" v-if="!useDefaultPassword">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" />
          </el-form-item>
        </template>

        <el-form-item label="昵称">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="formData.avatar" placeholder="请输入头像URL" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="formData.sex">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.user-management {
  padding: 20px;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
}
</style>