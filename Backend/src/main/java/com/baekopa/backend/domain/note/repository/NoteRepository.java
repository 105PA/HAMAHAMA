package com.baekopa.backend.domain.note.repository;

import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.domain.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByIdAndDeletedAtIsNull(Long noteId);

    List<Note> findAllByMemberAndDeletedAtIsNull(Member member);

    List<Note> findTop5ByMemberAndDeletedAtIsNullOrderByModifiedAtDesc(Member member);

    @Modifying
    @Query(value = "DELETE FROM note WHERE MONTH(deleted_at) = :thresholdDate", nativeQuery = true)
    int deleteSoftDeletedBeforeDate(Timestamp thresholdDate);
}
