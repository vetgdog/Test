package org.example;

public class AudioDurationParser {

    /**
     * 从音频字节数组获取时长
     * @param audioBytes 音频字节数组
     * @return 时长（秒），如果无法解析则返回null
     */
    public static Double getDuration(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length < 12) {
            return null;
        }

        // 检测音频格式
        String format = detectFormat(audioBytes);
        
        switch (format) {
            case "wav":
                return getWavDuration(audioBytes);
            case "mp3":
                return getMp3Duration(audioBytes);
            default:
                return null;
        }
    }

    /**
     * 检测音频格式
     * @param bytes 字节数组
     * @return 格式类型
     */
    private static String detectFormat(byte[] bytes) {
        // WAV 文件头检测 (RIFF...WAVE)
        if (bytes.length >= 12 &&
            bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 && // RIFF
            bytes[8] == 0x57 && bytes[9] == 0x41 && bytes[10] == 0x56 && bytes[11] == 0x45) { // WAVE
            return "wav";
        }

        // MP3 文件头检测
        // ID3v2 标签
        if (bytes.length >= 3 && bytes[0] == 0x49 && bytes[1] == 0x44 && bytes[2] == 0x33) {
            return "mp3";
        }
        
        // MP3 同步帧头 (0xFF, 0xE0-0xFF)
        if (bytes.length >= 2 && 
            (bytes[0] & 0xFF) == 0xFF && 
            ((bytes[1] & 0xFF) & 0xE0) == 0xE0) {
            return "mp3";
        }

        return "unknown";
    }

    /**
     * 获取WAV文件时长
     * @param bytes 字节数组
     * @return 时长（秒）
     */
    private static Double getWavDuration(byte[] bytes) {
        try {
            int offset = 12; // 跳过 RIFF header

            int sampleRate = 0;
            int bytesPerSecond = 0;
            int dataSize = 0;

            // 解析 WAV chunks
            while (offset < bytes.length - 8) {
                String chunkId = new String(bytes, offset, 4);
                int chunkSize = readLittleEndian32(bytes, offset + 4);

                if ("fmt ".equals(chunkId)) {
                    // 格式块
                    sampleRate = readLittleEndian32(bytes, offset + 12);
                    bytesPerSecond = readLittleEndian32(bytes, offset + 16);
                } else if ("data".equals(chunkId)) {
                    // 数据块
                    dataSize = chunkSize;
                    break;
                }

                offset += 8 + chunkSize;
                // 确保偶数对齐
                if (chunkSize % 2 == 1) offset++;
            }

            if (bytesPerSecond > 0) {
                return (double) dataSize / bytesPerSecond;
            } else if (sampleRate > 0) {
                // 备用计算方法
                int channels = readLittleEndian16(bytes, 22);
                int bitsPerSample = readLittleEndian16(bytes, 34);
                long totalSamples = dataSize / (channels * bitsPerSample / 8);
                return (double) totalSamples / sampleRate;
            }

            return null;
        } catch (Exception e) {
            System.err.println("WAV解析错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取MP3文件时长
     * @param bytes 字节数组
     * @return 时长（秒）
     */
    private static Double getMp3Duration(byte[] bytes) {
        try {
            int offset = 0;
            int totalFrames = 0;
            int sampleRate = 0;
            int samplesPerFrame = 0;

            // 跳过ID3v2标签
            if ((bytes[0] & 0xFF) == 0x49 && (bytes[1] & 0xFF) == 0x44 && (bytes[2] & 0xFF) == 0x33) {
                int id3Size = ((bytes[6] & 0x7F) << 21) | ((bytes[7] & 0x7F) << 14) |
                             ((bytes[8] & 0x7F) << 7) | (bytes[9] & 0x7F);
                offset = 10 + id3Size;
            }

            // 查找并解析MP3帧
            while (offset < bytes.length - 4) {
                // 寻找帧同步标识 (0xFF, 0xE0-0xFF)
                if ((bytes[offset] & 0xFF) == 0xFF &&
                    ((bytes[offset + 1] & 0xFF) & 0xE0) == 0xE0) {

                    int header = readBigEndian32(bytes, offset);
                    Mp3FrameInfo frameInfo = parseMp3Header(header);

                    if (frameInfo != null) {
                        if (totalFrames == 0) {
                            sampleRate = frameInfo.sampleRate;
                            samplesPerFrame = frameInfo.samplesPerFrame;
                        }

                        totalFrames++;
                        offset += frameInfo.frameSize;
                    } else {
                        offset++;
                    }
                } else {
                    offset++;
                }
            }

            if (totalFrames > 0 && sampleRate > 0 && samplesPerFrame > 0) {
                return (double) (totalFrames * samplesPerFrame) / sampleRate;
            }

            return null;
        } catch (Exception e) {
            System.err.println("MP3解析错误: " + e.getMessage());
            return null;
        }
    }


    /**
     * MP3帧信息类
     */
    private static class Mp3FrameInfo {
        int sampleRate;
        int samplesPerFrame;
        int frameSize;
        int bitrate;
        
        Mp3FrameInfo(int sampleRate, int samplesPerFrame, int frameSize, int bitrate) {
            this.sampleRate = sampleRate;
            this.samplesPerFrame = samplesPerFrame;
            this.frameSize = frameSize;
            this.bitrate = bitrate;
        }
    }

    /**
     * 解析MP3帧头
     * @param header 32位帧头
     * @return 帧信息
     */
    private static Mp3FrameInfo parseMp3Header(int header) {
        int version = (header >> 19) & 0x03;
        int layer = (header >> 17) & 0x03;
        int bitrate = (header >> 12) & 0x0F;
        int sampleRateIndex = (header >> 10) & 0x03;
        int padding = (header >> 9) & 0x01;

        // MPEG版本
        double[] versions = {2.5, 0, 2, 1}; // 0表示无效
        double mpegVersion = versions[version];
        if (mpegVersion == 0) return null;

        // Layer检查
        int[] layers = {0, 3, 2, 1}; // 0表示无效
        int mpegLayer = layers[layer];
        if (mpegLayer == 0) return null;

        // 采样率表
        int[][] sampleRates = {
            {11025, 12000, 8000},  // MPEG 2.5
            {0, 0, 0},             // 保留
            {22050, 24000, 16000}, // MPEG 2
            {44100, 48000, 32000}  // MPEG 1
        };
        
        int sampleRate = sampleRates[version][sampleRateIndex];
        if (sampleRate == 0) return null;

        // 比特率表 (Layer 3)
        int[][] bitrateTable = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // 保留
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320}, // MPEG 1
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}      // MPEG 2/2.5
        };
        
        int bitrateRow = (version == 3) ? 1 : 2; // MPEG 1 使用行1，其他使用行2
        int bitrateKbps = bitrateTable[bitrateRow][bitrate];
        if (bitrateKbps == 0) return null;

        // 计算帧长度
        int samplesPerFrame = (version == 3) ? 1152 : 576; // MPEG 1: 1152, MPEG 2/2.5: 576
        int frameSize = (144 * bitrateKbps * 1000) / sampleRate + padding;

        return new Mp3FrameInfo(sampleRate, samplesPerFrame, frameSize, bitrateKbps);
    }

    // 辅助方法：读取小端序32位整数
    private static int readLittleEndian32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | 
               ((bytes[offset + 1] & 0xFF) << 8) | 
               ((bytes[offset + 2] & 0xFF) << 16) | 
               ((bytes[offset + 3] & 0xFF) << 24);
    }

    // 辅助方法：读取小端序16位整数
    private static int readLittleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    // 辅助方法：读取大端序32位整数
    private static int readBigEndian32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | 
               ((bytes[offset + 1] & 0xFF) << 16) | 
               ((bytes[offset + 2] & 0xFF) << 8) | 
               (bytes[offset + 3] & 0xFF);
    }
}