import Router from '@/router'
import { useWsLoginStore, LoginStatus } from '@/stores/ws'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { useGroupStore } from '@/stores/group'
import { useGlobalStore } from '@/stores/global'
import { useEmojiStore } from '@/stores/emoji'
import { WsResponseMessageType } from './wsType'
import type {
  LoginSuccessResType,
  LoginInitResType,
  WsReqMsgContentType,
  OnStatusChangeType,
} from './wsType'
import type { MessageType, MarkItemType, RevokedMsgType } from '@/services/types'
import { OnlineEnum, ChangeTypeEnum, RoomTypeEnum } from '@/enums'
import { computedToken } from '@/services/request'
import { worker } from './initWorker'
import shakeTitle from '@/utils/shakeTitle'
import notify from '@/utils/notification'
import { ElMessage } from 'element-plus'

class WS {
  #tasks: WsReqMsgContentType[] = []
  // 重连🔐
  #connectReady = false

  constructor() {
    this.initConnect()
    // 收到消息
    worker.addEventListener('message', this.onWorkerMsg)

    // 后台重试次数达到上限之后，tab 获取焦点再重试
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && !this.#connectReady) {
        this.initConnect()
      }

      // 获得焦点停止消息闪烁
      if (!document.hidden) {
        shakeTitle.clear()
      }
    })
  }

  initConnect = () => {
    const token = localStorage.getItem('TOKEN')
    // 如果token 是 null, 而且 localStorage 的用户信息有值，需要清空用户信息
    if (token === null && localStorage.getItem('USER_INFO')) {
      localStorage.removeItem('USER_INFO')
    }
    // 初始化 ws
    worker.postMessage(`{"type":"initWS","value":${token ? `"${token}"` : null}}`)
  }

  onWorkerMsg = (e: MessageEvent<any>) => {
    const params: { type: string; value: unknown } = JSON.parse(e.data)
    switch (params.type) {
      case 'message': {
        this.onMessage(params.value as string)
        break
      }
      case 'open': {
        this.#dealTasks()
        break
      }
      case 'close':
      case 'error': {
        this.#onClose()
        break
      }
    }
  }

  // 重置一些属性
  #onClose = () => {
    this.#connectReady = false
  }

  #dealTasks = () => {
    this.#connectReady = true
    // 先探测登录态
    // this.#detectionLoginStatus()

    setTimeout(() => {
      const userStore = useUserStore()
      const loginStore = useWsLoginStore()
      if (userStore.isSign) {
        // 处理堆积的任务
        this.#tasks.forEach((task) => {
          this.send(task)
        })
        // 清空缓存的消息
        this.#tasks = []
      } else {
        // 如果没登录，而且已经请求了登录二维码，就要更新登录二维码。
        loginStore.loginQrCode && loginStore.getLoginQrCode()
      }
    }, 500)
  }

  #send(msg: WsReqMsgContentType) {
    worker.postMessage(
      `{"type":"message","value":${typeof msg === 'string' ? msg : JSON.stringify(msg)}}`,
    )
  }

  send = (params: WsReqMsgContentType) => {
    if (this.#connectReady) {
      this.#send(params)
    } else {
      // 放到队列
      this.#tasks.push(params)
    }
  }

  // 收到消息回调
  onMessage = (value: string) => {
    // FIXME 可能需要 try catch,
    const params: { type: WsResponseMessageType; data: unknown } = JSON.parse(value)
    const loginStore = useWsLoginStore()
    const userStore = useUserStore()
    const chatStore = useChatStore()
    const groupStore = useGroupStore()
    const globalStore = useGlobalStore()
    const emojiStore = useEmojiStore()
    switch (params.type) {

      // 获取登录二维码
      case WsResponseMessageType.LoginQrCode: {
        const data = params.data as LoginInitResType
        loginStore.loginQrCode = data.loginUrl
        break
      }

      // 等待授权
      case WsResponseMessageType.WaitingAuthorize: {
        loginStore.loginStatus = LoginStatus.Waiting
        break
      }

      // 登录成功
      case WsResponseMessageType.LoginSuccess: {
        // console.log('收到登录成功消息:', JSON.stringify(params.data))

        // 设置登录状态
        loginStore.loginStatus = LoginStatus.Success
        userStore.isSign = true

        // 提取数据并处理
        const data = params.data as LoginSuccessResType

        // 确保token存在
        if (!data.token) {
          console.error('登录成功但token为空')
          ElMessage.error('登录成功但未获取到令牌，请重试')
          return
        }

        // 保存用户信息和token
        const { token, ...userInfo } = data
        userStore.userInfo = { ...userStore.userInfo, ...userInfo }
        localStorage.setItem('USER_INFO', JSON.stringify(userInfo))
        localStorage.setItem('TOKEN', token)

        // 清除登录超时检测
        if (window.loginTimeoutId) {
          clearTimeout(window.loginTimeoutId)
          window.loginTimeoutId = null
        }

        // 显示登录成功提示
        ElMessage.success('登录成功')

        // 路由跳转
        setTimeout(() => {
          // 如果当前在登录页，跳转到首页
          if (Router.currentRoute.value.path === '/login') {
            Router.push('/')
          }
        }, 100)

        break
      }

      // 登录失败（添加这个新的消息类型处理）
      case WsResponseMessageType.LoginError: {
        const data = params.data as { msg: string }
        console.log('登录失败:', data.msg)
        // 显示错误消息
        ElMessage.error(data.msg || '登录失败，请检查用户名和密码')

        break
      }

      // 也需要添加相应的枚举值
      // WsResponseMessageType.LoginError = 1000

      // 收到消息
      case WsResponseMessageType.ReceiveMessage: {
        chatStore.pushMsg(params.data as MessageType)
        break
      }
      // 用户下线
      case WsResponseMessageType.OnOffLine: {
        const data = params.data as OnStatusChangeType
        groupStore.countInfo.onlineNum = data.onlineNum
        // groupStore.countInfo.totalNum = data.totalNum
        groupStore.batchUpdateUserStatus(data.changeList)
        break
      }
      // 用户 token 过期
      case WsResponseMessageType.TokenExpired: {
        userStore.isSign = false
        userStore.userInfo = {}
        localStorage.removeItem('USER_INFO')
        localStorage.removeItem('TOKEN')
        loginStore.loginStatus = LoginStatus.Init
        break
      }
      // 小黑子的发言在禁用后，要删除他的发言
      case WsResponseMessageType.InValidUser: {
        const data = params.data as { uid: number }
        // 消息列表删掉小黑子发言
        chatStore.filterUser(data.uid)
        // 群成员列表删掉小黑子
        groupStore.filterUser(data.uid)
        break
      }
      // 点赞、倒赞消息通知
      case WsResponseMessageType.WSMsgMarkItem: {
        const data = params.data as { markList: MarkItemType[] }
        chatStore.updateMarkCount(data.markList)
        break
      }
      // 消息撤回通知
      case WsResponseMessageType.WSMsgRecall: {
        const { data } = params as { data: RevokedMsgType }
        chatStore.updateRecallStatus(data)
        break
      }
      // 新好友申请
      case WsResponseMessageType.RequestNewFriend: {
        const data = params.data as { uid: number; unreadCount: number }
        globalStore.unReadMark.newFriendUnreadCount += data.unreadCount
        notify({
          name: '新好友',
          text: '您有一个新好友, 快来看看~',
          onClick: () => {
            Router.push('/contact')
          },
        })
        break
      }
      // 新好友申请
      case WsResponseMessageType.NewFriendSession: {
        // changeType 1 加入群组，2： 移除群组
        const data = params.data as {
          roomId: number
          uid: number
          changeType: ChangeTypeEnum
          activeStatus: OnlineEnum
          lastOptTime: number
        }
        if (
          data.roomId === globalStore.currentSession.roomId &&
          globalStore.currentSession.type === RoomTypeEnum.Group
        ) {
          if (data.changeType === ChangeTypeEnum.REMOVE) {
            // 移除群成员
            groupStore.filterUser(data.uid)
            // TODO 添加一条退出群聊的消息
          } else {
            // TODO 添加群成员
            // TODO 添加一条入群的消息
          }
        }
        break
      }
      default: {
        console.log('接收到未处理类型的消息:', params)
        break
      }
    }
  }
}

export default new WS()
