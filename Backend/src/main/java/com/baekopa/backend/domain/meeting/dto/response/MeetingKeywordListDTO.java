package com.baekopa.backend.domain.meeting.dto.response;

import com.baekopa.backend.domain.meeting.entity.MeetingKeyword;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MeetingKeywordListDTO {
    private List<MeetingKeywordDTO> keyword;

    @Builder
    public MeetingKeywordListDTO(List<MeetingKeywordDTO> keyword) {
        this.keyword = keyword;
    }


    public static MeetingKeywordListDTO from(List<MeetingKeywordDTO> meetingKeywordList){
        return builder()
                .keyword(meetingKeywordList)
                .build();
    }



}
