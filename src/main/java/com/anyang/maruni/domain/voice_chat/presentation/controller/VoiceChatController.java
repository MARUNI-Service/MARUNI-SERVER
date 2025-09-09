package com.anyang.maruni.domain.voice_chat.presentation.controller;

import com.anyang.maruni.domain.voice_chat.application.service.AudioProcessingService;
import com.anyang.maruni.domain.voice_chat.domain.entity.Conversation;
import com.anyang.maruni.domain.voice_chat.presentation.dto.response.ConversationResponse;
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
public class VoiceChatController {

    private final AudioProcessingService audioProcessingService;

    @PostMapping
    public ResponseEntity<ConversationResponse> transcribeAudio(@RequestParam("file") MultipartFile audioFile) {
        Conversation conversation = audioProcessingService.processAudio(audioFile);
        return ResponseEntity.ok(new ConversationResponse(conversation));
    }

    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> transcribeAndSynthesize(@RequestParam("file") MultipartFile audioFile) {
        byte[] ttsAudio = audioProcessingService.processAudioAndSynthesize(audioFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"response.mp3\"")
                .body(ttsAudio);
    }
}
