import pybase64
import json
import logging
import os
import re
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from urllib3.util.retry import Retry
from PIL import Image
from requests.adapters import HTTPAdapter

# 日志配置
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)


languages = {
    "zh_tw": ["的插畫", "的漫畫"],
    "zh": ["的插画", "的动图", "的漫画"],
    "ja": ["のイラスト", "のマンガ"],
    "ko": ["의 일러스트", "의 만화"]
}


class PixivConfig:
    """配置文件处理器"""
    _CONFIG_PATH = "pixiv_config.json"

    @classmethod
    def load_config(cls):
        """加载配置文件"""
        default_config = {
            "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "PHPSESSID": "",
            "root_dir": "D:/新建文件夹/pixiv"
        }

        try:
            with open(cls._CONFIG_PATH, 'r', encoding='utf-8') as f:
                config = json.load(f)

            # 合并默认配置
            return {**default_config, **config}
        except FileNotFoundError:
            logging.warning("未找到配置文件，正在创建默认配置...")
            with open(cls._CONFIG_PATH, 'w', encoding='utf-8') as f:
                json.dump(default_config, f, indent=2)
            return default_config


class PixivDownloader:
    def __init__(self):
        self.config = PixivConfig.load_config()
        self.headers = {
            'referer': 'https://www.pixiv.net/',
            'user-agent': self.config['user_agent'],
            'cookie': f'PHPSESSID={self.config["PHPSESSID"]}'
        }
        self.root_dir = self.config['root_dir']
        self.session = self._create_session()
        self.artist_name_cache = {}

    def _create_session(self):
        """创建带重试机制的会话"""
        s = requests.Session()
        retry = Retry(
            total=5,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=['HEAD', 'GET', 'OPTIONS']
        )
        adapter = HTTPAdapter(max_retries=retry)
        s.mount('http://', adapter)
        s.mount('https://', adapter)
        return s

    def _sanitize_filename(self, name):
        """清理非法文件名字符"""
        return re.sub(r'[\\/*?:"<>|]', "_", name).strip()

    def _get_artist_name(self, artwork_id):
        """通过作品ID获取作者名称（带缓存）"""
        if artwork_id in self.artist_name_cache:
            return self.artist_name_cache[artwork_id]

        url = f"https://www.pixiv.net/artworks/{artwork_id}"
        try:
            resp = self.session.get(url, headers=self.headers, timeout=10)
            resp.raise_for_status()
            resp_text = resp.text
            # 获取浏览器语言
            lang = re.findall(r' lang="(.*?)"', resp_text)
            if lang:
                lang = lang[0]
            else:
                return "Unknown_Artist"
            if lang in languages:
                # 返回画师名字
                for l in languages[lang]:
                    name = re.search(f'- (.*?){l}', resp_text)
                    if name:
                        self.artist_name_cache[artwork_id] = name.group(1)
                        return name.group(1)
            else:
                logging.info("不支持该网站的语言，仅支持简体中文、繁体中文、韩语及日语。")
            logging.warning("未找到对应画师~")
            return "Unknown_Artist"
        except Exception as e:
            logging.error(f"获取作者名称失败: {str(e)}")
            return "Unknown_Artist"

    def download_artwork(self, artwork_id):
        """下载单个作品（支持多图页/动图）"""
        try:
            # 创建作品目录
            artist = self._get_artist_name(artwork_id)
            # 获取作品元数据
            meta_url = f"https://www.pixiv.net/ajax/illust/{artwork_id}"
            resp = self.session.get(meta_url, headers=self.headers)
            meta = resp.json()

            if meta.get("error"):
                logging.error(f"作品不存在或无权访问: {artwork_id}")
                return False

            # 获取图片总数
            page_count = meta["body"]["pageCount"]
            # 动态生成保存路径
            base_dir = os.path.join(self.root_dir, artist)
            if page_count > 1:
                save_dir = os.path.join(base_dir, artwork_id)  # 多图：/作者名/作品ID/
            else:
                save_dir = base_dir  # 单图：/作者名/
            os.makedirs(save_dir, exist_ok=True)
            logging.info(f"开始下载{artwork_id}共{page_count}张图片 → {save_dir}")

            # 判断作品类型
            if meta["body"]["illustType"] == 2:  # 动图
                return self._download_ugoira(artwork_id, save_dir, artist)
            else:  # 普通图片
                return self._download_images(artwork_id, save_dir, artist)

        except Exception as e:
            logging.error(f"下载失败: {str(e)}")
            return False

    def _download_images(self, artwork_id, save_dir, artist):
        """下载普通图片"""
        pages_url = f"https://www.pixiv.net/ajax/illust/{artwork_id}/pages"
        resp = self.session.get(pages_url, headers=self.headers)
        pages = resp.json()["body"]

        with ThreadPoolExecutor(max_workers=4) as executor:
            futures = []
            for idx, page in enumerate(pages):
                url = page["urls"]["original"]
                ext = os.path.splitext(url)[1]
                filename = f"{artwork_id}_p{idx}{ext}"
                save_path = os.path.join(save_dir, filename)
                futures.append(executor.submit(self._download_file , url, save_path))

            for future in as_completed(futures):
                if not future.result():
                    return None
        return save_path

    def _download_ugoira(self, artwork_id, save_dir, artist):
        """下载并合成動画"""
        meta_url = f"https://www.pixiv.net/ajax/illust/{artwork_id}/ugoira_meta"
        resp = self.session.get(meta_url, headers=self.headers)
        meta = resp.json()

        if meta["error"]:
            logging.error("无法获取动图元数据")
            return False

        # 下载ZIP文件
        zip_url = meta["body"]["originalSrc"]
        zip_path = os.path.join(save_dir, f"{artwork_id}.zip")
        if not self._download_file(zip_url, zip_path):
            return False

        # 解压并合成GIF
        try:
            delays = [frame["delay"] for frame in meta["body"]["frames"]]
            self._convert_ugoira(zip_path, save_dir, delays, artist, artwork_id)
            os.remove(zip_path)  # 清理临时文件
            return True
        except Exception as e:
            logging.error(f"动图合成失败: {str(e)}")
            return False

    def _download_file(self, url, save_path):
        """通用文件下载方法"""
        if os.path.exists(save_path):
            logging.info(f"文件已存在，跳过下载: {save_path}")
            return True

        try:
            with self.session.get(url, headers=self.headers, stream=True) as r:
                r.raise_for_status()
                with open(save_path, 'wb') as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        f.write(chunk)
            logging.info(f"成功下载: {save_path}")
            return True
        except Exception as e:
            logging.error(f"下载失败 - {url}: {str(e)}")
            return False

    def _convert_ugoira(self, zip_path, save_dir, delays, artist, artwork_id):
        """将ugoira转换为GIF"""
        with zipfile.ZipFile(zip_path, 'r') as zf:

            image_files = [f for f in zf.namelist() if f.endswith(('.png', '.jpg', '.jpeg'))]
            images = [Image.open(zf.open(image_file)).convert('RGBA') for image_file in image_files]

            # frames = []
            # for filename in sorted(zf.namelist()):
            #     if filename.lower().endswith(('.jpg', '.jpeg', '.png')):
            #         with zf.open(filename) as f:
            #             img = Image.open(f).convert("RGBA")
            #             frames.append(img)

        gif_path = os.path.join(save_dir, f"{artwork_id}.gif")
        images[0].save(
            gif_path,
            save_all=True,
            append_images=images[1:],
            duration=delays,
            loop=0,
        )
        logging.info(f"动图合成成功: {gif_path}")

    # 下载对应画师对应tag的图片
    def download_artist(self, artist_id, tag="", limit=50):
        limit = min(limit, 100)
        """下载画师全部作品"""
        try:
            # 获取作品列表
            if tag:
                # 这里的limit固定为100(最佳，因为貌似再多一点会出现错误)，这里可以拿到对应tag的100个pid（不足100会按实际返回）
                api_url = f"https://www.pixiv.net/ajax/user/{artist_id}/illustmanga/tag?tag={tag}&offset=0&limit={limit}"
            else:
                api_url = f"https://www.pixiv.net/ajax/user/{artist_id}/profile/all"

            resp = self.session.get(api_url, headers=self.headers)
            data = resp.json()

            if data.get('error'):
                logging.error(f"获取画师作品失败: {data.get('message', '未知错误')}")
                return False
            if tag:
                artwork_ids = [work["id"] for work in data["body"]["works"]]
            else:
                artwork_ids = list(data["body"]["illusts"].keys())[:limit]

            if not artwork_ids:
                logging.warning("该画师没有公开的插画作品")
                return False

            logging.info(f"找到 {len(artwork_ids)} 个作品，开始下载...")

            # 创建画师目录
            artist_name = self._get_artist_name(artwork_ids[0])
            save_dir = os.path.join("artists", f"{artist_name}_{artist_id}")
            os.makedirs(save_dir, exist_ok=True)

            for artwork_id in artwork_ids:
                self.artist_name_cache[artwork_id] = artist_name

            # 批量下载
            success_count = 0
            with ThreadPoolExecutor(max_workers=4) as executor:
                futures = {executor.submit(self.download_artwork, aid): aid for aid in artwork_ids}
                for future in as_completed(futures):
                    aid = futures[future]
                    try:
                        if future.result():
                            success_count += 1
                            logging.info(f"已下载 {success_count}/{len(artwork_ids)} 张图片")
                    except Exception as e:
                        logging.error(f"作品 {aid} 下载失败: {str(e)}")

            logging.info(f"下载完成 ({success_count}/{len(artwork_ids)})")
            return True

        except Exception as e:
            logging.error(f"画师作品下载失败: {str(e)}")
            return False

    # @staticmethod
    # def file_to_base64(file_path):
    #     """将文件内容编码为Base64字符串"""
    #     with open(file_path, 'rb') as file:  # 二进制模式读取
    #         file_content = file.read()
    #         return base64.b64encode(file_content).decode('utf-8')  # 转为字符串

    @staticmethod
    def file_to_base64(file_path, timeout=15):
        """
        将文件内容编码为Base64字符串（带超时机制）
        :param file_path: 文件路径
        :param timeout: 超时时间（秒）
        :return: Base64编码字符串
        :raises: TimeoutError 当操作超时时抛出
        """

        def _read_file():
            with open(file_path, 'rb') as file:
                return file.read()

        try:
            with ThreadPoolExecutor(max_workers=1) as executor:
                future = executor.submit(_read_file)
                file_content = future.result(timeout=timeout)
                return pybase64.b64encode(file_content).decode('utf-8')  # 关键改动
        except TimeoutError:
            logging.error(f"Base64编码超时：{file_path} (超过{timeout}秒)")
            raise
        except Exception as e:
            logging.error(f"文件读取失败：{file_path} - {str(e)}")
            raise


# 使用示例 ==============================================
if __name__ == "__main__":
    # 首次使用前需要在同级目录创建 pixiv_config.json 文件
    # 内容格式：
    # {
    #     "user_agent": "你的浏览器User-Agent",
    #     "PHPSESSID": "你的Pixiv登录cookie"
    # }

    downloader = PixivDownloader()

    # 示例1：下载单个作品（动图）
    # downloader.download_artwork("127487466")  # 替换为实际作品ID
    #
    # # 示例2：下载整个画师作品
    # downloader.download_artist("69374642", "調月リオ")  # 替换为实际画师ID
    downloader.download_artist("463194", limit=50)  # 替换为实际画师ID
    # downloader.download_artist("107270690", "ブルーアーカイブ")  # 替换为实际画师ID

