import openpyxl
import requests
import json
import os
import time
from pathlib import Path

class TTSClient:
    def __init__(self):
        self.group_id = "1945048847600325060"  # TODO: 填写您的 GroupId
        self.api_key = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiJtZXRhWHNpcmUgWCIsIlVzZXJOYW1lIjoibWV0YVhzaXJlIFgiLCJBY2NvdW50IjoiIiwiU3ViamVjdElEIjoiMTk0NTA0ODg0NzYwODcxMzY2OCIsIlBob25lIjoiIiwiR3JvdXBJRCI6IjE5NDUwNDg4NDc2MDAzMjUwNjAiLCJQYWdlTmFtZSI6IiIsIk1haWwiOiJtZXRheHNpcmVAZ21haWwuY29tIiwiQ3JlYXRlVGltZSI6IjIwMjUtMDctMTggMTE6MTk6NDIiLCJUb2tlblR5cGUiOjEsImlzcyI6Im1pbmltYXgifQ.jUqG80TnHtTadoLgOuFwZ1KB7UimowIZaBkbxjrrOHMUBc-y1ENelKsRGSgy5YxRtCLasB_ivq3ibsnO6ZVFRatTaC8cgdC0dRb2EnMSASgzoj7yxc0bJZIvkTbs5NbuQbrkW5aAxGnGVWmGCMjt1xkRM6UYglhDXe_4t-OqOk7BcRVLCAvBGqxsbuA6-yXVpXKmQQlI4ieHgjzm8NvTZwlBsMErSC3EQJUY3-RG_Dwje7zWoX0DYDdBdwCBqmnCfY_qmkzhtiLeM4RyjkniDxWKYrxCWOL0abYtzshqUANYj56VueSUED2Vqy2x9Md-oDFej1m5ZkhIkk2ZfsJ9Zw"
        self.file_format = "mp3"  # 支持 mp3/pcm/flac

    def main(self):
        start_time = time.time()
        excel_path = "C:\\Users\\admin\\Desktop\\工作簿_1.xlsx"
        self.do_clone(excel_path)
        print(f"耗时: {time.time() - start_time:.2f}秒")

    def do_clone(self, excel_path):
        # 分别接收excel文件中的Texts，VoiceIds，Emotions
        orders = []
        types = []
        texts = []
        voice_ids = []
        emotions = []
        character_names = []

        # 读取excel文件
        workbook = openpyxl.load_workbook(excel_path)
        sheet = workbook.active  # 默认读取第一个 sheet

        for row in sheet.iter_rows(min_row=2):  # 跳过标题行
            order = str(row[0].value).strip() if row[0].value else ""
            type_ = str(row[1].value).strip() if row[1].value else ""
            text = str(row[2].value).strip() if row[2].value else ""
            voice_id = str(row[3].value).strip() if row[3].value else ""
            emotion = str(row[4].value).strip() if row[4].value else ""
            character_name = str(row[5].value).strip() if row[5].value else ""

            if text and voice_id:
                orders.append(order)
                types.append(type_)
                texts.append(text)
                voice_ids.append(voice_id)
                emotions.append(emotion)
                character_names.append(character_name)

        # 判断信息数量是否一致
        if not (len(orders) == len(types) == len(texts) == len(voice_ids) == len(emotions) == len(character_names)):
            return

        for i in range(len(orders)):
            self.tts_non_stream(orders[i], types[i], texts[i], voice_ids[i], emotions[i], character_names[i])

    def tts_non_stream(self, order, type_, text, voice_id, emotion, character_name):
        url = f"https://api.minimax.io/v1/t2a_v2?GroupId={self.group_id}"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

        data = self.build_request_body(text, voice_id, emotion)

        try:
            response = requests.post(url, headers=headers, json=data, timeout=None)
            response.raise_for_status()

            result = response.json()
            hex_audio = result["data"]["audio"]
            audio_bytes = self.hex_to_bytes(hex_audio)

            # 保存音频文件到 F:\TTSClient 目录
            output_dir = Path("F:/TTSClient")
            output_dir.mkdir(parents=True, exist_ok=True)

            filename = f"{order}_{type_}_{character_name}_es.{self.file_format}"
            file_path = output_dir / filename

            with open(file_path, "wb") as f:
                f.write(audio_bytes)

            print(f"✅ 音频保存成功: {file_path}")

        except requests.exceptions.RequestException as e:
            print(f"请求失败: {e}")
            if hasattr(e, 'response') and e.response is not None:
                print(f"状态码: {e.response.status_code}")
                print(f"错误信息: {e.response.text}")

    def build_request_body(self, text, voice_id, emotion):
        body = {
            "model": "speech-02-turbo",
            "text": text,
            "stream": False,  # 非流式，避免重复内容
            "voice_setting": {
                "voice_id": voice_id,
                "speed": 1.0,
                "vol": 1.0,
                "pitch": 0,
                "emotion": emotion
            },
            "audio_setting": {
                "sample_rate": 32000,
                "bitrate": 128000,
                "format": self.file_format,
                "channel": 1
            },
            "language_boost": "Spanish"
        }
    #     Supported values include:
    # 'Chinese', 'Chinese,Yue', 'English', 'Arabic', 'Russian', 'Spanish', 'French', 'Portuguese', 'German', 'Turkish', 'Dutch',
    #  'Ukrainian', 'Vietnamese', 'Indonesian', 'Japanese', 'Italian', 'Korean', 'Thai', 'Polish', 'Romanian', 'Greek', 'Czech', 'Finnish', 'Hindi', 'auto'
        return body

    def hex_to_bytes(self, hex_str):
        # 检查十六进制字符串长度是否为偶数
        if len(hex_str) % 2 != 0:
            raise ValueError("十六进制字符串长度必须为偶数")

        # 使用更高效的bytes.fromhex方法
        try:
            return bytes.fromhex(hex_str)
        except ValueError as e:
            print(f"十六进制转换错误: {e}")
            raise

if __name__ == "__main__":
    client = TTSClient()
    client.main()
