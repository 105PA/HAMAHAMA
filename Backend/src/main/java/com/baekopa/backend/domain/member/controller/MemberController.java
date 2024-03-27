package com.baekopa.backend.domain.member.controller;

import com.baekopa.backend.domain.meeting.dto.response.StudyMeetingListDto;
import com.baekopa.backend.domain.member.dto.request.MyInfoReqeustDto;
import com.baekopa.backend.domain.member.dto.response.MyInfoResponseDto;
import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.domain.member.service.MemberService;
import com.baekopa.backend.global.response.success.ApiResponse;
import com.baekopa.backend.global.response.success.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "사용자의 정보를 조회합니다.")
    @GetMapping()
    public ApiResponse<MyInfoResponseDto> getMyInfo(@AuthenticationPrincipal Member member) {

        log.info(" 내 정보 조회 : {}", member.getEmail());

        return ApiResponse.of(SuccessCode.MEMBER_FIND_SUCCESS, memberService.getMyInfo(member));
    }

    @Operation(summary = "내 정보 수정", description = "사용자의 프로필 사진, 이름을 수정합니다.")
    @PutMapping()
    public ApiResponse<Map<String, String>> updateMyInfo(@AuthenticationPrincipal Member member, @ModelAttribute MyInfoReqeustDto myInfoReqeustDto) throws IOException {

        log.info(" 내 정보 수정 : {}", member.getEmail());
        return ApiResponse.of(SuccessCode.UPDATE_SUCCESS, memberService.updateMyInfo(member, myInfoReqeustDto));
    }

    @Operation(summary = "내 스터디 미팅 조회", description = "내가 속한 스터디의 앞으로의 일정들을 조회합니다. 노트를 내보낼때 사용됩니다.")
    @GetMapping("meetings")
    public ApiResponse<List<StudyMeetingListDto>> getStudyMeetings(@AuthenticationPrincipal Member member) {

        log.info("내 스터디 미팅 조회 : {}", member.getName());

        return ApiResponse.of(SuccessCode.STUDY_MEETING_GET_SUCCESS, memberService.getStudyMeetings(member));
    }

    // TODO: 일정 로직 작성
//    @Operation(summary = "내 일정 조회", description = "나의 주간 일정을 조회합니다. 마이페이지 대시보드에서 사용합니다.")
//    @GetMapping("/study-timeline")
//    public ApiResponse<List<WeekMeetingListDto>> getMyMeetings(@AuthenticationPrincipal Member member, @RequestBody RequestWeekDto requestDto) {
//
//        log.info("요청 주간 : {} ~ {}", requestDto.getStartDate(), requestDto.getEndDate());
//
//        return ApiResponse.of(SuccessCode.MEETING_GET_SUCCESS, memberService.getMyMeetings(member, requestDto));
//
//    }

    // TODO: 속한 스터디 목록
    @Operation(summary = "내가 속한 스터디 목록 및 미팅 조회", description = "사용자가 속한 스터디 목록 및 미팅 조회")
    @GetMapping("/studies")
    public ApiResponse<?> getMyStudies(@AuthenticationPrincipal Member member) {

        log.info("내가 속한 스터디 목록 조회 : {}", member.getName());

        //return ApiResponse.of(SuccessCode.MY_STUDY_GET_SUCCESS, memberService.getMyStudies(member));
        return null;

    }

    // TODO: 작성한 노트 목록
    @Operation(summary = "내가 작성한 노트 목록 조회", description = "사용자가 작성한 노트 목록 조회")
    @GetMapping("/notes")
    public ApiResponse<?> getMyNotes(@AuthenticationPrincipal Member member) {

        log.info("내가 작성한 노트 목록 조회 : {}", member.getName());
        return ApiResponse.of(SuccessCode.NOTE_GET_SUCCESS, memberService.getMyNotes(member));

    }

}
