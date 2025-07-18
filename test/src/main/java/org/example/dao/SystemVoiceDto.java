package org.example.dao;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


/**
 * 包括所有可用的系统语音。包括语音 ID、语音描述和语音名称。
 */
@Data
public class SystemVoiceDto {

    //音色id
    @JsonProperty("voice_id")
    private String VoiceId;

    //音色名字
    @JsonProperty("voice_name")
    private String VoiceName;

    //音色描述
    @JsonProperty("description")
    private Object Description;

    //音色创建时间
    @JsonProperty("created_time")
    private String createdTime;
}
