package com.baekopa.backend.domain.meeting.controller;

import com.baekopa.backend.domain.meeting.dto.response.MeetingScriptDTO;
import com.baekopa.backend.domain.meeting.service.MeetingScriptService;
import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.global.response.success.ApiResponse;
import com.baekopa.backend.global.response.success.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MeetingScriptController {

    private final MeetingScriptService meetingScriptService;

    @Operation(summary = "스터디 내용 STT 추출", description = "STT 텍스트 파일을 저장합니다.")
    @PostMapping("/studies/{studyId}/meetings/{meetingId}/record")
    public CompletableFuture<ApiResponse<Map<String, Long>>> saveMeetingScript(@PathVariable(value="studyId") Long studyId,
                                                            @PathVariable(value="meetingId") Long meetingId,
                                                            @RequestParam("file") MultipartFile file) {
        meetingScriptService.saveS3(file, meetingId);
        CompletableFuture<Map<String, Long>> futureResult = meetingScriptService.saveMeetingScriptAsync(studyId, meetingId, file);
        return futureResult.thenApply(result -> ApiResponse.of(SuccessCode.MEETING_SCRIPT_CREATE_SUCCESS, result));
    }

    @Operation(summary = "스터디 전문 조회", description = "미팅 전문을 조회합니다")
    @GetMapping("/studies/{study-id}/meetings/{meeting-id}/entire")
    public ApiResponse<MeetingScriptDTO> getMeetingScript(@PathVariable(value = "meeting-id") Long meetingId, @AuthenticationPrincipal Member member) {
        return ApiResponse.of(SuccessCode.MEETING_SCRIPT_GET_SUCCESS, meetingScriptService.getMeetingScript(meetingId));
    }

    @Operation(summary = "스터디 전문 수정", description = "수정한 미팅 전문을 저장합니다.")
    @PutMapping("/studies/{study-id}/meetings/{meeting-id}/entire")
    public ApiResponse<MeetingScriptDTO> updateMeetingScript(@RequestBody MeetingScriptDTO meetingScriptDTO, @PathVariable(value = "meeting-id") Long meetingId, @AuthenticationPrincipal Member member) {
        return ApiResponse.of(SuccessCode.MEETING_SCRIPT_UPDATE_SUCCESS, meetingScriptService.updateMeetingScript(meetingScriptDTO));
    }

}
