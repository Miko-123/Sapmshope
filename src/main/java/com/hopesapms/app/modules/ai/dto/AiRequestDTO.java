package com.hopesapms.app.modules.ai.dto;

import lombok.Data;

@Data
public class AiRequestDTO {
    private String prompt;
    private String contextType;
}
