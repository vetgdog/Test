package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class AudioToByteArray {

    /**
     * 读取本地音频文件并转换为 byte[] 数组
     * @param filePath 音频文件路径
     * @return 音频的字节数组
     * @throws IOException 文件读取异常
     */
    public static byte[] readAudioFileToByteArray(String filePath) throws IOException {
        File audioFile = new File(filePath);

        // 检查文件是否存在
        if (!audioFile.exists()) {
            throw new IOException("文件未找到: " + filePath);
        }

        // 创建输入流读取文件
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] byteArray = new byte[(int) audioFile.length()];
            int bytesRead = fis.read(byteArray);

            if (bytesRead != byteArray.length) {
                throw new IOException("读取文件不完整");
            }

            return byteArray;
        }
    }

    // 测试代码
    public static void main(String[] args) {
        String audioPath = "F:\\test\\1add1.wav"; // 替换成你的音频文件路径
        byte[] audioBytes = new byte[0];
        try {
            audioBytes = readAudioFileToByteArray(audioPath);
            System.out.println("音频文件已成功读取为 byte 数组，长度：" + audioBytes.length);
            System.out.println(Arrays.toString(audioBytes));
        } catch (IOException e) {
            System.err.println("读取音频文件失败: " + e.getMessage());
        }
        double time = AudioDurationParser.getDuration(audioBytes);
        long timeLong = Math.round(time*1000);
        System.out.println("音频时长为:" + timeLong);
    }
}
