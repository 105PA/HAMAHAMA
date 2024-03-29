package com.baekopa.backend.domain.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RemindQuizResponseDto {

    private Long remindQuizId;
    private String topic; // meeting topic
    private String studyName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime studyAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime openAt;
    private boolean isOpened;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lastModifiedAt;

    @Builder
    private RemindQuizResponseDto(Long remindQuizId, String topic, String studyName, LocalDateTime studyAt, LocalDateTime openAt, boolean isOpened, LocalDateTime lastModifiedAt) {
        this.remindQuizId = remindQuizId;
        this.topic = topic;
        this.studyAt = studyAt;
        this.studyName = studyName;
        this.openAt = openAt;
        this.isOpened = isOpened;
        this.lastModifiedAt = lastModifiedAt;
    }

    public static RemindQuizResponseDto of(Long remindQuizId, String topic, String studyName, LocalDateTime studyAt, LocalDateTime openAt, boolean isOpened, LocalDateTime lastModifiedAt) {

        return builder().remindQuizId(remindQuizId)
                .topic(topic)
                .studyName(studyName)
                .studyAt(studyAt)
                .openAt(openAt)
                .isOpened(isOpened)
                .lastModifiedAt(lastModifiedAt)
                .build();
    }

}