import os
import re

# 修改为你存放音频的目录路径
folder_path = r'F:\123456 - 副本'  # 修改为你的文件夹路径

# 匹配以数字开头，接下来的格式为 _X2_... 的文件名，例如 40_X2_1_*.mp3
pattern = re.compile(r'^(\d+)(_X2_.*\.mp3)$')

for filename in os.listdir(folder_path):
    match = pattern.match(filename)
    if match:
        number = int(match.group(1)) + 1  # 原始数字 + 1
        rest_of_name = match.group(2)
        new_filename = f"{number}{rest_of_name}"

        # 构建完整路径
        old_path = os.path.join(folder_path, filename)
        new_path = os.path.join(folder_path, new_filename)

        print(f"Renaming: {filename} -> {new_filename}")
        os.rename(old_path, new_path)

print("全部重命名完成！")


