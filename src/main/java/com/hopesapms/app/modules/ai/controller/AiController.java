package com.hopesapms.app.modules.ai.controller;

import com.hopesapms.app.modules.ai.dto.AiRequestDTO;
import com.hopesapms.app.modules.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestBody AiRequestDTO dto) {
        return ResponseEntity.ok(aiService.generateContent(dto));
    }
}
