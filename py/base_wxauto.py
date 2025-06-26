from wxauto import WeChat
import time
import requests
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)


class SpringBootClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()  # 使用会话保持连接
        self.session.headers.update({"Accept": "application/json"})

    def get_response(self, message: str, retries=3) -> dict:
        """
        调用Spring接口获取回复
        :param message: 用户消息内容
        :param retries: 失败重试次数
        :return: 包含text和audioPath的字典
        """
        endpoint = "/chat/wx/auto"
        params = {"message": message}

        for attempt in range(retries):
            try:
                response = self.session.get(
                    f"{self.base_url}{endpoint}",
                    params=params
                )
                response.raise_for_status()
                return response.json()
            except requests.exceptions.RequestException as e:
                time.sleep(2 ** attempt)  # 指数退避等待
        raise ConnectionError(f"接口请求失败，已达最大重试次数 {retries}")


# 按装订区域中的绿色按钮以运行脚本。
if __name__ == '__main__':
    # 初始化服务客户端
    spring_client = SpringBootClient()
    # 获取微信窗口对象
    wx = WeChat()
    target_chat = "佳代子Agent"  # 指定监听和回复的聊天对象
    try:
        # 激活目标聊天窗口（确保微信客户端已登录且窗口可见）
        # wx.ChatWith(target_chat)
        wx.AddListenChat(target_chat)
        logging.info(f"已连接到微信聊天：{target_chat}")

        # 消息处理循环
        while True:
            try:
                # 获取监听到的新消息
                msgs = wx.GetListenMessage()

                # 遍历所有聊天窗口（理论上此时只有target_name）
                for chat in msgs:
                    # 获取当前聊天窗口的消息列表
                    message_list = msgs.get(chat)
                    for msg in message_list:
                        # 过滤出目标聊天的新文本消息
                        if msg.type == 'friend' and msg.sender == target_chat:
                            logging.info(f"收到新消息：{msg.content}")

                            # 调用Spring接口
                            try:
                                # 获取AI回复
                                response = spring_client.get_response(msg.content)
                                reply_text = response.get('text', '回复生成失败')
                                audio_path = response.get('audioPath')

                                # 发送文本回复
                                wx.SendMsg(reply_text, target_chat)
                                logging.info(f"已发送文本回复：{reply_text[:50]}...")

                                # 处理并发送音频
                                if audio_path:
                                    wx.SendFiles(audio_path, target_chat)
                                    logging.info(f"已发送音频文件：{audio_path}")

                            except Exception as e:
                                logging.error(f"处理消息失败：{str(e)}")
                                wx.SendMsg("服务器发呆中", target_chat)

                time.sleep(3)  # 降低CPU占用

            except KeyboardInterrupt:
                logging.info("用户主动终止程序")
                break

            except Exception as e:
                logging.error(f"运行时错误：{str(e)}")
                time.sleep(10)

    finally:
        # 清理资源
        spring_client.session.close()
        logging.info("程序已正常退出")



