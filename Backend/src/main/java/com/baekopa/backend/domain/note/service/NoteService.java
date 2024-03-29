package com.baekopa.backend.domain.note.service;

import com.baekopa.backend.domain.meeting.dto.response.SharedMeetingDto;
import com.baekopa.backend.domain.meeting.entity.Meeting;
import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.domain.member.repository.MemberRepository;
import com.baekopa.backend.domain.note.dto.request.CreateNoteRequestDto;
import com.baekopa.backend.domain.note.dto.request.CreateNoteSummaryRequestDto;
import com.baekopa.backend.domain.note.dto.response.CreateNoteSummaryResponseDto;
import com.baekopa.backend.domain.note.dto.response.NoteResponseDto;
import com.baekopa.backend.domain.note.entity.Note;
import com.baekopa.backend.domain.note.entity.SubmittedNote;
import com.baekopa.backend.domain.note.repository.NoteRepository;
import com.baekopa.backend.domain.note.repository.SubmittedNoteRepository;
import com.baekopa.backend.domain.study.entity.Study;
import com.baekopa.backend.global.response.error.ErrorCode;
import com.baekopa.backend.global.response.error.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final MemberRepository memberRepository;
    private final SubmittedNoteRepository submittedNoteRepository;
    private final RestTemplate restTemplate;

    @Value("${BASE_URL_AI}")
    private String aiUrl;

    public Long createNewNote(CreateNoteRequestDto requestDto) {

        // 작성자
        Member writer = memberRepository.findByIdAndDeletedAtIsNull(requestDto.getWriter())
                .orElseThrow(() -> BusinessException.builder()
                        .errorCode(ErrorCode.MEMBER_ID_NOT_EXIST)
                        .message(ErrorCode.MEMBER_ID_NOT_EXIST.getMessage())
                        .build());

        // 새로운 노트 생성
        Note note = Note.of(requestDto.getTitle(), requestDto.getContent(), null, writer);

        return noteRepository.save(note).getId();

    }

    // 요약 생성
    public String createSummary(Long noteId) throws JsonProcessingException {

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_EXIST, ErrorCode.NOTE_NOT_EXIST.getMessage()));

        // 요약 요청
        String summaryUrl = aiUrl + "/studies/summary";

        // Json 변환
        CreateNoteSummaryRequestDto summaryRequestDto = CreateNoteSummaryRequestDto.from(note.getContent());
        ObjectMapper objectMapper = new ObjectMapper();
        Object data = objectMapper.writeValueAsString(summaryRequestDto);

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity = new HttpEntity<>(data, headers);

        // api 요청
        CreateNoteSummaryResponseDto responseDto = restTemplate.postForObject(summaryUrl, requestEntity, CreateNoteSummaryResponseDto.class);
        note.updateSummary(responseDto.getSummaryText());

        log.info("요약 결과물 : {}", responseDto.getSummaryText());

        return responseDto.getSummaryText();
    }

    public NoteResponseDto getNote(Long noteId, Member member) {

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_EXIST, ErrorCode.NOTE_NOT_EXIST.getMessage()));

        List<SubmittedNote> meetingList = submittedNoteRepository.findMeetingByNote(note);

        // convert entity to dto
        List<SharedMeetingDto> sharedMeetingDtoList = meetingList.stream().map(this::convertToDto).toList();

        return NoteResponseDto.of(noteId, note.getTitle(), note.getContent(), note.getCreatedAt(), note.getModifiedAt(), note.getCreatedBy(), member.getImage(), note.getSummary(), sharedMeetingDtoList);

    }

    public SharedMeetingDto convertToDto(SubmittedNote submittedNote) {

        Meeting meeting = submittedNote.getMeeting();
        Study study = meeting.getStudy();
        return SharedMeetingDto.of(meeting.getId(),
                meeting.getTopic(),
                meeting.getStudyAt(),
                study.getId(),
                study.getTitle(),
                study.getBackgroundImage());

    }
}

