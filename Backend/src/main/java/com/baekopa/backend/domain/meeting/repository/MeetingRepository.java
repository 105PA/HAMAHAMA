package com.baekopa.backend.domain.meeting.repository;

import com.baekopa.backend.domain.meeting.entity.Meeting;
import com.baekopa.backend.domain.study.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByStudyAndDeletedAtIsNull(Study study);

    Optional<Meeting> findById(Long meetingId);

    List<Meeting> findAllByStudyAndDeletedAtIsNullAndStudyAtGreaterThanEqualOrderByStudyAtAsc(Study study, LocalDateTime current);

    Optional<Meeting> findTopByStudyAndDeletedAtIsNullAndStudyAtGreaterThanEqualOrderByStudyAtAsc(Study study, LocalDateTime currentDate);

    Optional<Meeting> findTopByStudyAndDeletedAtIsNullAndStudyAtLessThanEqualOrderByStudyAtDesc(Study study, LocalDateTime currentDate);

    @Query(value = "SELECT * FROM Meeting WHERE abs(TIMESTAMPDIFF(MINUTE, study_at, now())) <= 30 AND deleted_at is NULL AND study_at >= now() AND note_summary is NULL", nativeQuery = true)
    List<Meeting> findUpcomingMeetings();

}
