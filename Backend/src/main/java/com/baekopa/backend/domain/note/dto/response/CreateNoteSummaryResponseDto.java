package com.baekopa.backend.domain.note.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNoteSummaryResponseDto {

    private String originalText;
    private String summaryText;

}
