package com.baekopa.backend.domain.note.service;

import com.baekopa.backend.domain.meeting.entity.Meeting;
import com.baekopa.backend.domain.meeting.repository.MeetingRepository;
import com.baekopa.backend.domain.note.dto.request.MeetingUniquificationDTO;
import com.baekopa.backend.domain.note.dto.request.SubmittedNoteListDTO;
import com.baekopa.backend.domain.note.repository.SubmittedNoteRepository;
import com.baekopa.backend.global.response.error.ErrorCode;
import com.baekopa.backend.global.response.error.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmittedNoteSummaryService {

    private final MeetingRepository meetingRepository;
    private final SubmittedNoteRepository submittedNoteRepository;
    private final RestTemplate restTemplate;

    @Value("${BASE_URL_AI}")
    private String fastUrl;

    @Scheduled(fixedRate = 5 * 60 * 1000, zone = "Asia/Seoul")
    @Transactional
    public void createSubmittedNoteSummary() {

        // 요약 해야하는 미팅 조회
        // study at이 now와 30분이내로 차이가 나고, now보다 크고, 삭제되지 않았고, 요약이 아직 안된 meeting
        List<Meeting> meetingList = meetingRepository.findUpcomingMeetings();

        String uniquificationUrl = fastUrl + "/studies/uniquification";

        //// 요약 해야하는 제출된 노트 조회
        for (Meeting meeting : meetingList) {

            List<String> submittedNoteList = submittedNoteRepository.findAllByMeetingAndDeletedAtIsNull(meeting)
                    .stream().map((submittedNote) -> submittedNote.getNote().getSummary()).toList();

            SubmittedNoteListDTO submittedNoteListDTO = SubmittedNoteListDTO.of(submittedNoteList);
            String requestBody = convertObjectToJson(submittedNoteListDTO);

            // fast api 통신
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            MeetingUniquificationDTO meetingUniquificationDTO = restTemplate.postForObject(uniquificationUrl, requestEntity, MeetingUniquificationDTO.class);
            meeting.updateNoteSummary(meetingUniquificationDTO.getUniquification());
        }

    }

    private String convertObjectToJson(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.JSON_PARSE_ERROR, ErrorCode.JSON_PARSE_ERROR.getMessage());
        }
    }

}
