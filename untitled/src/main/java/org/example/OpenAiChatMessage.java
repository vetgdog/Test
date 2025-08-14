package org.example;

import lombok.Data;

@Data
public class OpenAiChatMessage {
    private String role;
    private Object content;
}
