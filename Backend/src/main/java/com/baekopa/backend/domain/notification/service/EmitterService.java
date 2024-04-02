package com.baekopa.backend.domain.notification.service;

import com.baekopa.backend.domain.member.entity.Member;
import com.baekopa.backend.domain.notification.dto.response.NotificationResponseDto;
import com.baekopa.backend.domain.notification.entity.Notification;
import com.baekopa.backend.domain.notification.entity.NotificationStatus;
import com.baekopa.backend.domain.notification.entity.NotificationType;
import com.baekopa.backend.domain.notification.repository.EmitterRepository;
import com.baekopa.backend.domain.notification.repository.NotificationRepository;
import com.baekopa.backend.global.response.error.ErrorCode;
import com.baekopa.backend.global.response.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmitterService {

    private static final Long DEFAULT_TIMEOUT = 1000L * 60 * 70;

    private final EmitterRepository emitterRepository;
    private final NotificationRepository notificationRepository;

    // 알림 구독
    public SseEmitter subscribe(Member member) {

        String key = createKeyByEmailAndDomain(member);
        String emitterId = createIdByKeyAndTime(key);

        // cache에 emitter 저장
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.saveEmitter(emitterId, emitter);

        // emitter 전처리 작업
        preProcessEmitter(member, emitter, emitterId, key);

        // 미수신 event 전송
        Map<String, NotificationResponseDto> events = emitterRepository.findAllEventStartWithKey(emitterId);
        events.entrySet().stream()
                .filter(entry -> member.getLastCheckedEventId() < Long.parseLong(entry.getKey()))
                .forEach(entry -> {
                    String eventId = entry.getKey();
                    NotificationResponseDto responseDto = entry.getValue();
                    sendNotification(emitter, emitterId, eventId, NotificationStatus.NEW.name(), responseDto);
                });

        return emitter;
    }

    // 알림 전송
    @Transactional
    public void send(Member receiver, NotificationType notificationType, String notificationContent, String relatedContentId) {

        String key = createKeyByEmailAndDomain(receiver);
        String eventId = createIdByKeyAndTime(key);

        // DB에 notification 저장
        Notification notification = Notification.of(
                receiver,
                notificationType,
                notificationContent,
                eventId,
                relatedContentId
        );
        notificationRepository.save(notification);

        // cache에 event 저장
        NotificationResponseDto responseDto = NotificationResponseDto.of(notification);
        emitterRepository.saveEvent(eventId, responseDto);

        // 수신자 event 전송
        Map<String, SseEmitter> emitters = emitterRepository.findAllEmitterStartWithKey(key);
        emitters.entrySet().stream()
                .forEach(entry -> {
                    String emitterId = entry.getKey();
                    SseEmitter emitter = entry.getValue();
                    sendNotification(emitter, emitterId, eventId, NotificationStatus.NEW.name(), responseDto);
                });

    }

    private void preProcessEmitter(Member member, SseEmitter emitter, String emitterId, String key) {

        emitter.onCompletion(() -> emitterRepository.deleteEmitterById(emitterId));
        emitter.onTimeout(() -> {
            emitterRepository.deleteEmitterById(emitterId);
            emitter.complete();
        });

        // 멤버 이벤트 시간 확인
        Long lastCheckedEventId = member.getLastCheckedEventId();

        // 마지막 이벤트 시간 확인
        boolean isAnyEventLater = emitterRepository.findAllEventStartWithKey(key).entrySet().stream()
                .anyMatch(entry -> lastCheckedEventId < Long.parseLong(entry.getKey().split("_")[2]));

        sendNotification(emitter, emitterId, emitterId, NotificationStatus.CONNECT.name(), isAnyEventLater);
    }

    private void sendNotification(SseEmitter emitter, String emitterId, String eventId, String notificationName, Object data) {

        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name(notificationName)
//                    .reconnectTime(1000L) // 재연결 시도
                    .data(data));

        } catch (IllegalStateException | IOException exception) {
            // 클라이언트와의 연결이 끊긴 경우, emitter를 만료시킨다.
            emitter.complete();
            emitterRepository.deleteEmitterById(emitterId);
        }
    }

    private String createKeyByEmailAndDomain(Member member) {

        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(member.getEmail()).append("_").append(member.getProvider().name()).toString();
    }

    private String createIdByKeyAndTime(String key) {

        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(key).append("_").append(System.currentTimeMillis()).toString();
    }
}
