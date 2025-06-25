package com.anyang.maruni.domain.stt.presentation.controller;

import com.anyang.maruni.domain.stt.application.service.SttService;
import com.anyang.maruni.domain.stt.domain.entity.Conversation;
import com.anyang.maruni.domain.stt.presentation.dto.response.ConversationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stt")
public class SttController {

    private final SttService sttService;

    @PostMapping
    public ResponseEntity<ConversationResponse> transcribeAudio(@RequestParam("file") MultipartFile audioFile) {

        Conversation conversation = sttService.processAudio(audioFile);

        ConversationResponse response = new ConversationResponse(conversation);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> transcribeAndSynthesize(@RequestParam("file") MultipartFile audioFile) {

        byte[] ttsAudio = sttService.processAudioAndSynthesize(audioFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"response.mp3\"")
                .body(ttsAudio);
    }
}
