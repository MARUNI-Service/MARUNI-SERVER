package com.anyang.maruni.domain.stt.domain.entity;

import com.anyang.maruni.domain.User.domain.entity.User;
import com.anyang.maruni.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Conversation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저 정보 (연관관계)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String originalAudioUrl;   // S3 URL 또는 local 저장 경로
    private String sttText;            // Whisper 변환 텍스트
    private String gptResponse;        // ChatGPT 응답 텍스트
}
