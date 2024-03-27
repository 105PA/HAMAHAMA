package com.baekopa.backend.domain.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SharedMeetingDto {

    private Long id;

    private String topic;

    @JsonFormat(pattern = "YYYY-MM-DD")
    private LocalDate studyAt;

    private Long studyId;

    private String studyName;

    private String studyImage;

    @Builder
    private SharedMeetingDto(Long id, String topic, LocalDate studyAt, Long studyId, String studyName, String studyImage) {
        this.id = id;
        this.topic = topic;
        this.studyAt = studyAt;
        this.studyId = studyId;
        this.studyName = studyName;
        this.studyImage = studyImage;
    }

    public static SharedMeetingDto of(Long id, String topic, LocalDate studyAt, Long studyId, String studyName, String studyImage) {
        return builder().id(id)
                .topic(topic)
                .studyAt(studyAt)
                .studyId(studyId)
                .studyName(studyName)
                .studyImage(studyImage)
                .build();
    }
}