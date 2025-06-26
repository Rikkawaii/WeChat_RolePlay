import logging
import os
import time
from datetime import datetime
import asyncio

import requests
import websockets
import json

from third.pixiv_downloader import PixivDownloader
from wxpad_client import MessageClient

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)


class SpringBootClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.session = requests.Session()  # 使用会话保持连接
        self.session.headers.update({"Accept": "application/json"})

    def _request_with_retry(self, endpoint, params, method, retries=3):
        """带重试机制的通用请求方法"""
        for attempt in range(retries):
            try:
                if method == "get":
                    response = self.session.get(
                        f"{self.base_url}{endpoint}",
                        params=params
                    )
                else:
                    response = self.session.post(
                        f"{self.base_url}{endpoint}",
                        json=params
                    )
                response.raise_for_status()

                res = response.json()
                if res.get("code") != 0:
                    # todo: 这里逻辑有点问题
                    raise ValueError(res.get("message"))
                return res.get('data')
            except requests.exceptions.RequestException as e:
                logging.error(f"请求失败，尝试第 {attempt + 1} 次: {e}")
                time.sleep(2 ** attempt)  # 指数退避策略
        raise ConnectionError(f"请求失败，已达最大重试次数 {retries}")

    def get_qa_response(self, message: str) -> dict:
        """
        调用Spring接口获取回复
        :param message: 用户消息内容
        :return: 纯文本
        """
        endpoint = "/chat"
        params = {"message": message}

        return self._request_with_retry(endpoint, params, method="get")

    def get_role_play_response(self, sender: str, message: str) -> dict:
        """
        调用Spring接口获取回复
        :param sender: 发送者微信ID
        :param message: 用户消息内容
        :return: 包含text和voiceData的字典v
        """
        endpoint = "/chat/wx/pad"
        params = {
            "message": message,
            "wxid": sender}

        return self._request_with_retry(endpoint, params, method="get")

    def set_role(self, wxid: str, role: str) -> dict:
        """
        调用Spring接口设置角色
        :param wxid:
        :param role: 角色名称
        :return:
        """
        endpoint = "/chat/role"
        params = {
            "wxid": wxid,
            "role": role
        }

        return self._request_with_retry(endpoint, params, method="post")


KEY = "ebde5eed-ddaf-4645-9c4a-4b2005a1a949"
# 消息类型映射
message_type_mapping = {
    1: "文本消息",
    3: "图片消息",
    34: "语音消息",
    47: "动画表情"
}

chat_mode = "Q&A_ASSISTANT"  # 默认为问答助手
chat_role = "Kayoko"  # 默认为鬼方佳代子
# 定义回复模板
MODE_RESPONSE_TEMPLATES = {
    "help": """📢 当前可切换模式:
1. Q&A_ASSISTANT: 问答助手模式(/mode 1)
2. REPEATER: 复读机模式(/mode 2)
3. ROLE_PLAY: 角色扮演模式(/mode 3)
当前模式为: {chat_mode}""",

    "success": "🎉 模式切换成功！当前模式为:{chat_mode}"
}
ROLE_RESPONSE_TEMPLATES = {
    "help": """📢 当前可切换角色:
1. Kayoko: 鬼方佳代子(/role Kayoko)
2. Arona: 阿罗娜(/role Arona)
3. Rikka: 小鸟游六花(/role Rikka)
当前角色为: {chat_role}""",

    "success": "🎉 角色切换成功！当前角色为: {chat_role}",
    "fail": "❗角色不存在, 当前角色为: {chat_role}"
}
# 模式映射
MODE_MAPPING = {
    "1": "Q&A_ASSISTANT",
    "2": "REPEATER",
    "3": "ROLE_PLAY"
}


# 处理模式切换
def handle_mod_change(content: str):
    global chat_mode
    parts = content.split(" ")
    if parts[1] == "help":
        reply_text = MODE_RESPONSE_TEMPLATES["help"].format(chat_mode=chat_mode)
    else:
        # 通过get方法实现优雅回退
        chat_mode = MODE_MAPPING.get(parts[1], "Q&A_ASSISTANT")  # 默认问答助手模式
        reply_text = MODE_RESPONSE_TEMPLATES["success"].format(chat_mode=chat_mode)
    return reply_text


def handle_role_change(content: str):
    global chat_role
    parts = content.split(" ")
    if parts[1] == "help":
        reply_text = ROLE_RESPONSE_TEMPLATES["help"].format(chat_role=chat_role)
    else:
        chat_role = parts[1]
        if chat_role != "Kayoko" and chat_role != "Arona" and chat_role != "Rikka":
            reply_text = ROLE_RESPONSE_TEMPLATES["fail"].format(chat_role=chat_role)
        else:
            reply_text = ROLE_RESPONSE_TEMPLATES["success"].format(chat_role=chat_role)
    return reply_text


async def sync_wechat_messages():
    uri = f"ws://localhost:8059/ws/GetSyncMsg?key={KEY}"  # 替换有效密钥
    try:
        async with websockets.connect(uri, ping_interval=30) as websocket:
            logging.info("✅ 成功连接微信消息同步服务")
            # while循环外创建spring_client和message_client
            spring_client = SpringBootClient()
            message_client = MessageClient(key=KEY)
            pixiv_downloader_client = PixivDownloader()
            while True:
                try:
                    # 接收原始消息数据
                    raw_data = await websocket.recv()
                    # 解析JSON数据
                    msg = json.loads(raw_data)
                    # 提取关键信息
                    sender = msg["from_user_name"]["str"]
                    receiver = msg["to_user_name"]["str"]
                    message_value = msg["msg_type"]
                    content = msg["content"]["str"]
                    timestamp = msg["create_time"]

                    # 格式化时间戳
                    dt = datetime.fromtimestamp(timestamp)
                    formatted_time = dt.strftime("%Y-%m-%d %H:%M:%S")

                    # 映射消息类型
                    message_type = message_type_mapping.get(message_value, "未知消息类型")

                    # 输出结构化信息
                    logging.info(f"""
                    🕒 时间: {formatted_time}
                    👤 发送者: {sender}
                    👥 接收者: {receiver}
                    📢 消息类型: {message_type}
                    📩 内容: {content}
                    """)
                    # 需要回复的用户(这里填入需要回复的用户)
                    need_reply_user = {"wxid_xxxxxxxxx"}
                    # 调用Spring接口
                    if sender in need_reply_user and message_type == "文本消息":
                        # 模式切换
                        if "/mode" in content:
                            reply_text = handle_mod_change(content)
                            message_client.send_text_message(sender, reply_text)
                            logging.info(f"已发送文本回复：{reply_text[:50]}...")
                            continue
                        # 角色切换
                        if "/role" in content:
                            reply_text = handle_role_change(content)
                            if "不存在" in reply_text or "可切换" in reply_text:
                                message_client.send_text_message(sender, reply_text)
                                continue

                            # 角色切换
                            is_success = spring_client.set_role(sender, chat_role)
                            if is_success:
                                # 发送回复通知切换成功
                                message_client.send_text_message(sender, reply_text)
                                logging.info(f"已发送文本回复：{reply_text[:50]}...")
                            else:
                                message_client.send_text_message(sender, "❌ 角色切换失败！！！")
                            continue

                        if chat_mode == "Q&A_ASSISTANT":
                            # 获取图片
                            if "%图片" in content:
                                message_client.send_text_message(sender, "正在搜索图片，请稍候...")
                                pid = content.replace("%图片", "").strip()
                                try:
                                    # 第一步：下载图片
                                    image_path = pixiv_downloader_client.download_artwork(pid)
                                    if image_path:
                                        # 第二步：Base64编码（带超时）
                                        try:
                                            # 动态设置超时（大文件增加时间）
                                            file_size = os.path.getsize(image_path)
                                            timeout = max(15, -1 * (-file_size // (2 * 1024 * 1024)))  # 每2MB增加1秒，最低15秒

                                            image_data = PixivDownloader.file_to_base64(file_path=image_path, timeout=timeout)
                                            message_client.send_image_message(sender, image_data)
                                            logging.info(
                                                f"已发送图片消息：{image_path} (大小: {file_size / 1024:.1f}KB)")
                                        except TimeoutError:
                                            message_client.send_text_message(sender, "图片处理超时！")
                                            logging.warning(
                                                f"Base64编码超时：{image_path} (大小: {file_size / 1024:.1f}KB)")

                                        except MemoryError:
                                            message_client.send_text_message(sender, "图片太大，内存不足处理！")
                                            logging.error(f"内存不足：{image_path} (大小: {file_size / 1024:.1f}KB)")

                                        except Exception as e:
                                            message_client.send_text_message(sender, "图片处理失败，请稍后重试！")
                                            logging.error(f"Base64编码错误：{image_path} - {str(e)}")

                                        # finally:
                                        #     # 可选：清理临时文件
                                        #     if os.path.exists(image_path):
                                        #         os.remove(image_path)
                                    else:
                                        message_client.send_text_message(sender, "图片搜索失败，请检查ID是否正确！")

                                except Exception as e:
                                    message_client.send_text_message(sender, "图片下载过程中发生错误！")
                                    logging.error(f"图片下载失败：{pid} - {str(e)}")
                                continue



                            # 普通问答
                            response = spring_client.get_qa_response(content)
                            reply_text = response
                            message_client.send_text_message(sender, reply_text)
                            logging.info(f"已发送文本回复：{reply_text[:50]}...")
                        elif chat_mode == "REPEATER":
                            message_client.send_text_message(sender, content)
                        else:
                            try:
                                # 获取AI回复
                                response = spring_client.get_role_play_response(sender, content)
                                reply_text = response.get('text', '回复生成失败')
                                image_data = response.get('imageData')
                                voice_data = response.get('voiceData')

                                # 发送文本回复
                                message_client.send_text_message(sender, reply_text)
                                logging.info(f"已发送文本回复：{reply_text[:50]}...")
                                if image_data:
                                    message_client.send_image_message(sender, image_data)
                                    logging.info(f"已发送图片消息：{image_data[:50]}...")
                                # 处理并发送音频
                                if voice_data:
                                    message_client.send_voice_message(sender, voice_data)
                                    logging.info(f"已发送语音消息：{voice_data[:50]}")

                            except Exception as e:
                                message_client.send_text_message(sender, "❗❗❗睡着了~~~")
                                logging.error(f"处理消息失败：{str(e)}")
                    elif sender in need_reply_user:
                        message_client.send_text_message(sender, f"{message_type}暂不支持回复")

                except websockets.exceptions.ConnectionClosedError:
                    print("❌ 连接异常中断，尝试重连...")
                    break
                except KeyError as e:
                    print(f"⚠️ 数据字段缺失: {str(e)}")
                except json.JSONDecodeError:
                    print("⚠️ 非标准JSON格式消息")

    except (ConnectionRefusedError, OSError):
        print("❌ 无法连接到服务器，请检查：\n1. 服务是否运行\n2. 端口是否开放\n3. 密钥是否有效")


if __name__ == '__main__':
    asyncio.get_event_loop().run_until_complete(sync_wechat_messages())
