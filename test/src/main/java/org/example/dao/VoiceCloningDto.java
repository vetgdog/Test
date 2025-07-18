package org.example.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 包括帐户下的所有克隆语音。包括语音 ID、语音描述和创建时间。
 */
@Data
public class VoiceCloningDto {
    //音色id
    @JsonProperty("voice_id")
    private String VoiceId;
    //音色描述
    @JsonProperty("description")
    private Object Description;
    //音色创建时间
    @JsonProperty("created_time")
    private String createdTime;
}
