import re
import os
from collections import Counter

def extract_first_numbers(directory):
    # 创建存储序号的列表
    numbers_list = []

    # 正则表达式匹配规则：提取最后一个下划线前的数字
    pattern = re.compile(r'^(\d+)_')

    # 遍历目录中的所有文件
    for filename in os.listdir(directory):
        if filename.endswith('.mp3'):
            match = pattern.search(filename)
            if match:
                number = match.group(1)
                numbers_list.append(number)

    return numbers_list

# 使用示例（请替换为实际文件目录）
if __name__ == '__main__':
    target_directory1 = r'F:\TTSClient\新\es\18_Ariel'  # 修改为您的MP3文件所在目录
    target_directory2 = r'F:\TTSClient\新\es\17_Cleopatra'  # 修改为您的MP3文件所在目录
    result1 = extract_first_numbers(target_directory1)
    result2 = extract_first_numbers(target_directory2)

    # 找出缺失的元素
    missing_elements = [x for x in result1 if x not in result2]
    print('result2中缺失的元素：', missing_elements)

    # 统计数量差异
    count1 = Counter(result1)
    count2 = Counter(result2)

    inconsistent_elements = {}
    for num in set(result1 + result2):
        if count1[num] != count2.get(num, 0):
            inconsistent_elements[num] = {
                'result1_count': count1[num],
                'result2_count': count2.get(num, 0)
            }

    print('数量不一致的元素：', inconsistent_elements)
