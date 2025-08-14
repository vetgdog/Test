package org.example;


import okhttp3.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;

public class testnew {
    private static final String groupId = "1945048847600325060"; // TODO: 填写您的 GroupId
    private static final String apiKey = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiJtZXRhWHNpcmUgWCIsIlVzZXJOYW1lIjoibWV0YVhzaXJlIFgiLCJBY2NvdW50IjoiIiwiU3ViamVjdElEIjoiMTk0NTA0ODg0NzYwODcxMzY2OCIsIlBob25lIjoiIiwiR3JvdXBJRCI6IjE5NDUwNDg4NDc2MDAzMjUwNjAiLCJQYWdlTmFtZSI6IiIsIk1haWwiOiJtZXRheHNpcmVAZ21haWwuY29tIiwiQ3JlYXRlVGltZSI6IjIwMjUtMDgtMDQgMTY6NTM6MDkiLCJUb2tlblR5cGUiOjEsImlzcyI6Im1pbmltYXgifQ.DQ3qU9s7FSGLI6MV6E-UUYXeb1BoL88UgviCvdG9g31WIndVzmINfrLkL1f6nx3iNfaDhH17bG5QnzpPFyGqRU2dMyaitzcF2zsYIano8929-_URutfz_2ARNPPGCvWHA5GaDJFLYvBEA_XUb2R7H9N4aFNT1zh9LtbDX2Zp28p7EGZv5gobGxzTtTnyBohy4Bx1N93U1JbxHaJ1wwo13Nu4ExOloC3Lbg1kdfFrQYxGZMyEXo5n0JxdJH23dhlO2Sazw_9-hBOpx0T1E4NsO_Sj1CfrC9BsqKZIyhCkhOtXg20c2uNgiPygo6aiUeWDRHAVRajjietcNDYUsATb2g";
    private static final String fileFormat = "mp3"; // 支持 mp3/pcm/flac


    public static void main(String[] args) throws Exception {
        Long l = System.currentTimeMillis();
        //传入的excel文件
        String excelPath = "C:\\Users\\admin\\Desktop\\工作簿_2.xlsx";
        DoClone(excelPath);
        System.out.println(System.currentTimeMillis() - l);
    }

    public static void DoClone(String excelPath) throws Exception {
        //分别接收excel文件中的Texts，VoiceIds，Emotions
        List<String> Orders = new ArrayList<>();
        List<String> Types = new ArrayList<>();
        List<String> Texts = new ArrayList<>();
        List<String> VoiceIds = new ArrayList<>();
        List<String> Emotions = new ArrayList<>();
        List<String> CharacterNames = new ArrayList<>();

        //读取excel文件
        try (InputStream inp = Files.newInputStream(Paths.get(excelPath));
             Workbook workbook = new XSSFWorkbook(inp)) {

            Sheet sheet = workbook.getSheetAt(0); // 默认读取第一个 sheet
            int rowCount = sheet.getPhysicalNumberOfRows();

            for (int i = 1; i < rowCount; i++) { // 跳过标题行（从第1行开始）
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell orderCell = row.getCell(0);
                Cell typecell = row.getCell(1);
                Cell textCell = row.getCell(2);
                Cell voiceIdCell = row.getCell(3);
                Cell emotionCell = row.getCell(4);
                Cell characterNameCell = row.getCell(5);

                // 安全处理orderCell
                String order = "";
                if (orderCell != null && orderCell.getCellType() == CellType.NUMERIC) {
                    order = String.valueOf((int)orderCell.getNumericCellValue());
                } else if (orderCell != null) {
                    order = orderCell.toString().trim();
                }

                // 其他单元格保持原样
                String type = typecell != null ? typecell.toString().trim() : "";
                String text = textCell != null ? textCell.toString().trim() : "";
                String voiceId = voiceIdCell != null ? voiceIdCell.toString().trim() : "";
                String emotion = emotionCell != null ? emotionCell.toString().trim() : "";
                String characterName = characterNameCell != null ? characterNameCell.toString().trim() : "";

                if (!text.isEmpty() && !voiceId.isEmpty()) {
                    Orders.add(order);
                    Types.add(type);
                    Texts.add(text);
                    VoiceIds.add(voiceId);
                    Emotions.add(emotion);
                    CharacterNames.add(characterName);
                }
            }
        }

        //判断信息数量是否一致
        int length = Orders.size();
        int length1 = Types.size();
        int length2 = Texts.size();
        int length3 = VoiceIds.size();
        int length4 = Emotions.size();
        int length5 = CharacterNames.size();
        if(length1 != length || length2 != length || length3 != length || length4 != length || length5 != length) {
            return;
        }
        for(int i=0;i<length;i++){
            ttsNonStream(Orders.get(i),Types.get(i),Texts.get(i),VoiceIds.get(i),Emotions.get(i),CharacterNames.get(i));
        }

    }

    //发出请求，将文本转换为音频
    public static void ttsNonStream(String order,String type, String text,String voiceId,String emotion,String characterName) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();


        String url = "https://api.minimax.io/v1/t2a_v2?GroupId=" + groupId;

        RequestBody body = RequestBody.create(
                buildRequestBody(text,voiceId,emotion),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("请求失败，状态码：" + response.code());
                if (response.body() != null) {
                    System.err.println("错误信息：" + response.body().string());
                }
                return;
            }

            JSONObject result = new JSONObject(response.body().string());
            String hexAudio = result.getJSONObject("data").getString("audio");
            byte[] audioBytes = hexToBytes(hexAudio);

            // 保存音频文件到 F:\TTSClient 目录





            String filename = order+ '_' + type + "_" + characterName +"_es"  + "."  + fileFormat;
            String filePath = "F:\\TTSClient\\" + filename;

            Files.write(Paths.get(filePath), audioBytes);
            System.out.println("✅ 音频保存成功：" + filePath);
        }
    }

    //构建请求提
    private static String buildRequestBody(String text,String voiceId,String emotion) {
        JSONObject body = new JSONObject();
        body.put("model", "speech-02-turbo");
        body.put("text", text);
        body.put("stream", false);  // 非流式，避免重复内容

        JSONObject voiceSetting = new JSONObject();
        voiceSetting.put("voice_id", voiceId);
        voiceSetting.put("speed", 1.0);
        voiceSetting.put("vol", 1.0);
        voiceSetting.put("pitch", 0);
        voiceSetting.put("emotion",emotion);
        body.put("voice_setting", voiceSetting);

        JSONObject audioSetting = new JSONObject();
        audioSetting.put("sample_rate", 32000);
        audioSetting.put("bitrate", 128000);
        audioSetting.put("format", fileFormat);
        audioSetting.put("channel", 1);
        body.put("audio_setting", audioSetting);
        //Supported values include:
        //'Chinese', 'Chinese,Yue', 'English', 'Arabic', 'Russian', 'Spanish', 'French', 'Portuguese', 'German', 'Turkish', 'Dutch',
        // 'Ukrainian', 'Vietnamese', 'Indonesian', 'Japanese', 'Italian', 'Korean', 'Thai', 'Polish', 'Romanian', 'Greek', 'Czech', 'Finnish', 'Hindi', 'auto'
        body.put("language_boost", "Spanish");

        return body.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < len; i += 2) {
            out.write(Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return out.toByteArray();
    }
}
