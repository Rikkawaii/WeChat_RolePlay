import requests
import time


# wechatpadpro提供的消息发送接口


class MessageClient:
    def __init__(self, base_url="http://localhost:8059", key=""):
        self.base_url = base_url
        self.key = key
        self.session = requests.Session()
        # 设置通用请求头[6](@ref)
        self.session.headers.update({
            "Accept": "application/json",
            "Content-Type": "application/json"
        })

    def _request_with_retry(self, endpoint, payload, retries=3):
        """带重试机制的通用请求方法[1,8](@ref)"""
        for attempt in range(retries):
            try:
                response = self.session.post(
                    f"{self.base_url}{endpoint}?key={self.key}",
                    json=payload
                )
                response.raise_for_status()
                return response.json()
            except requests.exceptions.RequestException as e:
                time.sleep(2 ** attempt)  # 指数退避策略
        raise ConnectionError(f"请求失败，已达最大重试次数 {retries}")

    def send_text_message(self, to_user, content, at_list=None):
        """
        发送文本消息接口[1,6](@ref)
        :param to_user: 接收者微信ID
        :param content: 文本内容
        :param at_list: 需要@的微信ID列表
        :return: 接口响应
        """
        endpoint = "/message/SendTextMessage"
        payload = {
            "MsgItem": [{
                "AtWxIDList": at_list or [],
                "ImageContent": "",
                "MsgType": 1,  # 文本消息类型[6](@ref)
                "TextContent": content,
                "ToUserName": to_user
            }]
        }
        return self._request_with_retry(endpoint, payload)

    def send_voice_message(self, to_user, voice_data,
                           format_type=1, duration=0):
        """
        发送语音消息接口[6,8](@ref)
        :param to_user: 接收者微信ID
        :param voice_data: 语音二进制数据
        :param format_type: 音频格式代码（默认0）
        :param duration: 音频时长（秒）
        :return: 接口响应
        """
        endpoint = "/message/SendVoice"
        # 将二进制语音数据编码为base64[8](@ref)
        # encoded_voice = base64.b64encode(voice_data).decode('utf-8')
        payload = {
            "ToUserName": to_user,
            "VoiceData": voice_data,
            "VoiceFormat": format_type,
            "VoiceSecond": duration
        }
        return self._request_with_retry(endpoint, payload)

    def send_image_message(self, to_user, image_data, at_list=None):
        endpoint = "/message/SendImageMessage"
        payload = {
            "MsgItem": [{
                "AtWxIDList": at_list or [],
                "ImageContent": image_data,
                "MsgType": 3,  # 图片消息类型
                "TextContent": "",
                "ToUserName": to_user
            }]
        }
        return self._request_with_retry(endpoint, payload)
