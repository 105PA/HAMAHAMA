package com.baekopa.backend.domain.meeting.service;

import com.baekopa.backend.domain.meeting.dto.request.MeetingSummaryRequestDTO;
import com.baekopa.backend.domain.meeting.dto.request.MeetingSummaryUpdateDTO;
import com.baekopa.backend.domain.meeting.dto.request.UpdateMeetingKeywordDTO;
import com.baekopa.backend.domain.meeting.dto.request.UpdateMeetingKeywordListDTO;
import com.baekopa.backend.domain.meeting.dto.response.*;
import com.baekopa.backend.domain.meeting.entity.Meeting;
import com.baekopa.backend.domain.meeting.entity.MeetingKeyword;
import com.baekopa.backend.domain.meeting.entity.MeetingScript;
import com.baekopa.backend.domain.meeting.entity.MeetingSummary;
import com.baekopa.backend.domain.meeting.repository.MeetingKeywordRepository;
import com.baekopa.backend.domain.meeting.repository.MeetingRepository;
import com.baekopa.backend.domain.meeting.repository.MeetingScriptRepository;
import com.baekopa.backend.domain.meeting.repository.MeetingSummaryRepository;
import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.domain.note.entity.Note;
import com.baekopa.backend.domain.note.entity.SubmittedNote;
import com.baekopa.backend.domain.note.repository.SubmittedNoteRepository;
import com.baekopa.backend.domain.notification.entity.NotificationType;
import com.baekopa.backend.domain.notification.service.EmitterService;
import com.baekopa.backend.domain.study.entity.Study;
import com.baekopa.backend.domain.study.repository.StudyMemberRepository;
import com.baekopa.backend.domain.study.repository.StudyRepository;
import com.baekopa.backend.global.response.error.ErrorCode;
import com.baekopa.backend.global.response.error.exception.BusinessException;
import com.baekopa.backend.global.service.S3UploadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final MeetingScriptRepository meetingScriptRepository;
    private final MeetingKeywordRepository meetingKeywordRepository;
    private final S3UploadService s3UploadService;
    private final StudyRepository studyRepository;
    private final SubmittedNoteRepository submittedNoteRepository;
    private final EmitterService emitterService;
    private final StudyMemberRepository studyMemberRepository;

    @Value("${BASE_URL_AI}")
    private String fastUrl;
    private final RestTemplate restTemplate;

    public InStudyMeetingListDTO getMeetingList(Long studyId) {
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_EXIST, ErrorCode.STUDY_NOT_EXIST.getMessage()));

        List<Meeting> meetingList = meetingRepository.findAllByStudyAndDeletedAtIsNullAndRecordFileIsNotNullOrderByStudyAtDesc(study);
        List<MeetingStudyDTO> meetingStudyDTOList = new ArrayList<>();
        List<MeetingKeywordDTO> meetingKeywordDTOList = new ArrayList<>();
        List<MeetingMemberInfoDTO> meetingMemberInfoDTOList = new ArrayList<>();

        for (Meeting meeting : meetingList) {
            //미팅 키워드
            List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);

            for (MeetingKeyword meetingKeyword : meetingKeywordList) {
                MeetingKeywordDTO meetingKeywordDTO = MeetingKeywordDTO.from(meetingKeyword);
                meetingKeywordDTOList.add(meetingKeywordDTO);
            }

            // 미팅 참여자
            List<SubmittedNote> submittedNoteList = submittedNoteRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
            List<Note> noteList = new ArrayList<>();
            for (SubmittedNote submittedNote : submittedNoteList) {
                noteList.add(submittedNote.getNote());
            }
            for (Note note : noteList) {
                meetingMemberInfoDTOList.add(MeetingMemberInfoDTO.from(note.getMember()));
            }

            meetingStudyDTOList.add(MeetingStudyDTO.of(meeting, meetingKeywordDTOList, meetingMemberInfoDTOList));
        }

        return InStudyMeetingListDTO.of(study, meetingStudyDTOList);
    }

    public MeetingResponseDTO getMeetingResultAll(Long meetingId) {
        // 미팅 정보
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));
        // 미팅 전문
        MeetingScript meetingScript = meetingScriptRepository.findByMeetingAndDeletedAtIsNull(meeting)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SCRIPT_NOT_FOUND, ErrorCode.MEETING_SCRIPT_NOT_FOUND.getMessage()));
        // 미팅 요약
        MeetingSummary meetingSummary = meetingSummaryRepository.findByIdAndDeletedAtIsNull(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SUMMARY_NOT_FOUND, ErrorCode.MEETING_SUMMARY_NOT_FOUND.getMessage()));
        // 미팅 키워드
        List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
        List<MeetingKeywordDTO> meetingKeywordDTOList = new ArrayList<>();
        for (MeetingKeyword meetingKeyword : meetingKeywordList) {
            MeetingKeywordDTO meetingKeywordDTO = MeetingKeywordDTO.from(meetingKeyword);
            meetingKeywordDTOList.add(meetingKeywordDTO);
        }

        // 미팅 참여자
        List<SubmittedNote> submittedNoteList = submittedNoteRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
        List<Note> noteList = new ArrayList<>();
        for (SubmittedNote submittedNote : submittedNoteList) {
            noteList.add(submittedNote.getNote());
        }
        List<MeetingMemberInfoDTO> meetingMemberInfoDTOList = new ArrayList<>();
        for (Note note : noteList) {
            meetingMemberInfoDTOList.add(MeetingMemberInfoDTO.from(note.getMember()));
        }

        return MeetingResponseDTO.of(meeting, meetingScript, meetingSummary, meetingKeywordDTOList, meetingMemberInfoDTOList);
    }

    @Transactional
    public MeetingSummaryResponseDTO createSummary(Long studyId, Long meetingId) {
        String summaryUrl = fastUrl + "/studies/summary";

        String originalText = meetingScriptRepository.findByMeetingAndDeletedAtIsNull(meetingRepository.findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage())))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()))
                .getScriptContent();
        originalText = originalText.replace(".", ".\n");

        // Json 변환
        MeetingSummaryRequestDTO meetingSummaryRequestDTO = MeetingSummaryRequestDTO.of(originalText);
        ObjectMapper objectMapper = new ObjectMapper();
        Object jsonText;
        try {
            jsonText = objectMapper.writeValueAsString(meetingSummaryRequestDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // fast api 통신
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(jsonText, headers);
        MeetingSummaryDTO meetingSummaryDTO = restTemplate.postForObject(summaryUrl, requestEntity, MeetingSummaryDTO.class);

        // db 저장
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));
        MeetingSummary meetingSummary = MeetingSummary.of(meeting, meetingSummaryDTO.getSummaryText());
        meetingSummaryRepository.save(meetingSummary);
        MeetingSummaryResponseDTO meetingSummaryResponseDTO = MeetingSummaryResponseDTO.from(meetingSummaryDTO);

        String message = "'" + meeting.getStudy().getTitle() + "' '" + meeting.getTopic() + "' 요약이 완료되었습니다.";
        List<Member> members = getMeetingMember(meetingId);
        for (Member member : members) {
            emitterService.send(member, NotificationType.SUMMARY, message, studyId + "/" + meetingId);
        }

        return meetingSummaryResponseDTO;
    }

    public MeetingSummaryResponseDTO getMeetingSummary(Long meetingId) {
        MeetingSummary meetingSummary = meetingSummaryRepository.findByIdAndDeletedAtIsNull(meetingId).orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SUMMARY_NOT_FOUND, ErrorCode.MEETING_SUMMARY_NOT_FOUND.getMessage()));
        return MeetingSummaryResponseDTO.getMeetingSummary(meetingSummary);
    }

    @Transactional
    public MeetingSummaryResponseDTO updateCreateSummary(Long studyId, Long meetingId) {
        String summaryUrl = fastUrl + "/studies/summary";

        String originalText = meetingScriptRepository.findByMeetingAndDeletedAtIsNull(meetingRepository.findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage())))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()))
                .getScriptContent();
        originalText = originalText.replace(".", ".\n");

        // Json 변환
        MeetingSummaryRequestDTO meetingSummaryRequestDTO = MeetingSummaryRequestDTO.of(originalText);
        ObjectMapper objectMapper = new ObjectMapper();
        Object jsonText;
        try {
            jsonText = objectMapper.writeValueAsString(meetingSummaryRequestDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // fast api 통신
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(jsonText, headers);
        MeetingSummaryDTO meetingSummaryDTO = restTemplate.postForObject(summaryUrl, requestEntity, MeetingSummaryDTO.class);

        // db 저장
        MeetingSummary meetingSummary = meetingSummaryRepository.findByIdAndDeletedAtIsNull(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SUMMARY_NOT_FOUND, ErrorCode.MEETING_SUMMARY_NOT_FOUND.getMessage()));
        meetingSummary.updateMeetingSummary(meetingSummaryDTO.getSummaryText());

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));

        String message = "'" + meeting.getStudy().getTitle() + "' '" + meeting.getTopic() + "' 요약이 완료되었습니다.";
        List<Member> members = getMeetingMember(meetingId);
        for (Member member : members) {
            emitterService.send(member, NotificationType.SUMMARY, message, studyId + "/" + meetingId);
        }

        return MeetingSummaryResponseDTO.from(meetingSummaryDTO);
    }

    @Transactional
    public MeetingSummaryResponseDTO updateMeetingSummary(MeetingSummaryUpdateDTO meetingSummaryUpdateDTO, Long meetingId) {
        MeetingSummary meetingSummary = meetingSummaryRepository.findByIdAndDeletedAtIsNull(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SUMMARY_NOT_FOUND, ErrorCode.MEETING_SUMMARY_NOT_FOUND.getMessage()));
        meetingSummary.updateMeetingSummary(meetingSummaryUpdateDTO.getSummaryText());

        return MeetingSummaryResponseDTO.getMeetingSummary(meetingSummary);
    }

    @Transactional
    public MeetingKeywordListDTO createMeetingKeyword(Long studyId, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));
        //이미 기존의 키워드가 존재 한다면?
        if (meetingKeywordRepository.existsByMeetingAndDeletedAtIsNull(meeting)) {
            List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);

            for (MeetingKeyword meetingKeyword : meetingKeywordList) {
                meetingKeywordRepository.delete(meetingKeyword);
            }
        }

        String keywordUrl = fastUrl + "/studies/keyword";

        String originalText = meetingScriptRepository.findByMeetingAndDeletedAtIsNull(meetingRepository.findById(meetingId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage())))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_SCRIPT_NOT_FOUND, ErrorCode.MEETING_SCRIPT_NOT_FOUND.getMessage()))
                .getScriptContent();

        //originalText = originalText.replace(".", ".\n");

        MeetingSummaryRequestDTO meetingSummaryRequestDTO = MeetingSummaryRequestDTO.of(originalText);
        ObjectMapper objectMapper = new ObjectMapper();
        Object jsonText;

        try {
            jsonText = objectMapper.writeValueAsString(meetingSummaryRequestDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // fast api 통신
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> requestEntity = new HttpEntity<>(jsonText, headers);

        MeetingKeywordResponseDTO MeetingKeywordResponseDTO = restTemplate.postForObject(keywordUrl, requestEntity, MeetingKeywordResponseDTO.class);


        // db 저장

        for (int i = 0; i < MeetingKeywordResponseDTO.getKeyword().size(); i++) {
            meetingKeywordRepository.save(MeetingKeyword.of(meeting, MeetingKeywordResponseDTO.getKeyword().get(i)));
        }

        List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
        List<MeetingKeywordDTO> meetingKeywordDTOList = new ArrayList<>();
        for (MeetingKeyword meetingKeyword : meetingKeywordList) {
            MeetingKeywordDTO meetingKeywordDTO = MeetingKeywordDTO.from(meetingKeyword);
            meetingKeywordDTOList.add(meetingKeywordDTO);
        }

        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_NOT_EXIST, ErrorCode.STUDY_NOT_EXIST.getMessage()));
        List<Member> memberList = studyMemberRepository.findAllByStudyAndDeletedAtIsNull(study).stream()
                .map((o) -> o.getMember()).toList();
        for (Member member : memberList) {
            emitterService.send(member, NotificationType.KEYWORD, study.getTitle() + " " + meeting.getTopic() + "의 키워드 생성이 완료되었습니다.", studyId + "/" + meetingId);
        }

        return MeetingKeywordListDTO.from(meetingKeywordDTOList);
    }

    public MeetingKeywordListDTO getMeetingKeyword(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));
        List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
        List<MeetingKeywordDTO> meetingKeywordDTOList = new ArrayList<>();

        for (MeetingKeyword meetingKeyword : meetingKeywordList) {
            MeetingKeywordDTO meetingKeywordDTO = MeetingKeywordDTO.from(meetingKeyword);
            meetingKeywordDTOList.add(meetingKeywordDTO);
        }
        return MeetingKeywordListDTO.from(meetingKeywordDTOList);
    }

    @Transactional
    public MeetingKeywordListDTO updateMeetingKeyword(Long meetingId, UpdateMeetingKeywordListDTO updateMeetingKeywordListDTO) {
        for (UpdateMeetingKeywordDTO updateMeetingKeywordDTO : updateMeetingKeywordListDTO.getUpdateMeetingKeywordList()) {
            MeetingKeyword meetingKeyword = meetingKeywordRepository.findByIdAndDeletedAtIsNull(updateMeetingKeywordDTO.getGroupKeywordId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_KEYWORD_NOT_FOUND, ErrorCode.MEETING_KEYWORD_NOT_FOUND.getMessage()));

            meetingKeyword.updateMeetingKeyword(updateMeetingKeywordDTO.getKeyword());
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));
        List<MeetingKeyword> meetingKeywordList = meetingKeywordRepository.findAllByMeetingAndDeletedAtIsNull(meeting);
        List<MeetingKeywordDTO> meetingKeywordDTOList = new ArrayList<>();

        for (MeetingKeyword meetingKeyword : meetingKeywordList) {
            MeetingKeywordDTO meetingKeywordDTO = MeetingKeywordDTO.from(meetingKeyword);
            meetingKeywordDTOList.add(meetingKeywordDTO);
        }

        return MeetingKeywordListDTO.from(meetingKeywordDTOList);
    }

    private List<Member> getMeetingMember(Long meetingId) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEETING_NOT_FOUND, ErrorCode.MEETING_NOT_FOUND.getMessage()));

        return submittedNoteRepository.findAllByMeetingAndDeletedAtIsNull(meeting)
                .stream()
                .map((o) -> o.getNote().getMember()).collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void getTomorrowMeetings() {

        // 내일의 날짜로 미팅 조회
        List<Meeting> upcomingMeetings = meetingRepository.findTomorrowMeetings();

        // 조회된 미팅에 대한 처리
        for (Meeting meeting : upcomingMeetings) {
            List<Member> memberList = studyMemberRepository.findAllByStudyAndDeletedAtIsNull(meeting.getStudy()).stream()
                    .map((o) -> (o.getMember())).toList();

            String message = "'" + meeting.getStudy().getTitle() + "' 미팅 일정이 있습니다.";
            for (Member member : memberList) {
                emitterService.send(member, NotificationType.UPCOMING, message, meeting.getStudy().getId() + "");
            }
        }
    }
}
