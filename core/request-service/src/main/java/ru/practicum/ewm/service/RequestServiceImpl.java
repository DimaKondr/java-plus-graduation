package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.request.CreateUpdateRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.dto.event.EventState;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.service.integration.EventIntegrationService;
import ru.practicum.ewm.service.integration.UserIntegrationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserIntegrationService userIntegrationService;
    private final EventIntegrationService eventIntegrationService;

    @Transactional
    @Override
    public ParticipationRequestDto createRequest(CreateUpdateRequestDto dto) {
        //Дата создания
        LocalDateTime now = LocalDateTime.now();
        Long requesterId = checkUserExisting(dto.getUserId());
        EventFullDto event = eventIntegrationService.getEventInfo(dto.getEventId());

        //Проверка, что событие опубликовано
        if (!event.getState().equals(EventState.PUBLISHED.name())) {
            log.error("Не удается создать запрос на неопубликованное событие с id={}", event.getId());
            throw new ConflictException("Событие еще не опубликовано");
        }

        //Проверка, что инициатор не пытается участвовать в своем событии
        if (event.getInitiator().getId().equals(requesterId)) {
            log.error("Инициатор не может участвовать в собственном мероприятии. eventId={}, userId={}",
                    event.getId(), requesterId);
            throw new ConflictException("Инициатор не может участвовать в собственном мероприятии");
        }

        //Проверка, что пользователь уже не создавал запрос
        Optional<ParticipationRequest> existingRequest =
                requestRepository.findByRequesterIdAndEventId(requesterId, event.getId());

        if (existingRequest.isPresent()) {
            log.error("Запрос пользователя {} на событие {} уже существует",
                    requesterId, event.getId());
            throw new ConflictException(String.format("Запрос пользователя c id=%d на событие c id=%d уже существует",
                    requesterId, event.getId()));
        }

        // Проверка лимита участников
        Long approvedRequestsCount = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && approvedRequestsCount >= event.getParticipantLimit()) {
            log.error("Достигнут лимит участников для event {}. Limit: {}, CONFIRMED: {}",
                    event.getId(), event.getParticipantLimit(), approvedRequestsCount);
            throw new ConflictException(String.format("Достигнут лимит участников. Limit=%d, Approved=%d",
                    event.getParticipantLimit(), approvedRequestsCount));
        }

        //Определение статуса запроса
        RequestStatus initialStatus;

        if (event.getParticipantLimit() == 0) {
            initialStatus = RequestStatus.CONFIRMED;
        } else if (!event.getRequestModeration()) {
            initialStatus = RequestStatus.CONFIRMED;
        } else {
            initialStatus = RequestStatus.PENDING;
        }

        ParticipationRequest request = RequestMapper.toEntity(now, event.getId(), requesterId, initialStatus);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создан запрос с id={}, статус={}", saved.getId(), initialStatus);
        return RequestMapper.toParticipationRequestDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        log.info("Получение запросов пользователя с id={}", userId);

        checkUserExisting(userId); // проверка существования

        return requestRepository.findAllByUserId(userId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    @Override
    public ParticipationRequestDto canceledRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);

        checkUserExisting(userId); // проверка существования

        ParticipationRequest request = findParticipationRequest(requestId);

        // Проверка, что запрос принадлежит пользователю
        if (!request.getRequesterId().equals(userId)) {
            log.error("Запрос с id={} не принадлежит пользователю с id={}", requestId, userId);
            throw new NotFoundException("Запрос не найден или не принадлежит пользователю");
        }

        // Только PENDING запросы можно отменить
        if (request.getStatus() != RequestStatus.PENDING) {
            log.error("Нельзя отменить запрос со статусом: {}", request.getStatus());
            throw new ConflictException("Можно отменить только запросы в статусе PENDING");
        }
        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest canceled = requestRepository.save(request);
        log.info("Запрос с id={} отменен", requestId);
        return RequestMapper.toParticipationRequestDto(canceled);
    }

    @Override
    public List<ConfirmedRequestCount> countConfirmedRequests(List<Long> eventIds, RequestStatus requestStatus) {
        // Нужно считать только CONFIRMED запросы
        if (requestStatus.equals(RequestStatus.PENDING)) {
            log.error("Нужно считать только подтвержденные запросы, а поступил статус: {}", requestStatus);
            throw new ConflictException("Нужно считать только CONFIRMED запросы");
        }
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, requestStatus);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    public Long getRequestsCountByIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return requestRepository.getRequestsCountByIdAndStatus(eventId, requestStatus);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(List<Long> requestIds) {
        return requestRepository.findAllById(requestIds).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    @Override
    public List<ParticipationRequestDto> updateRequests(List<ParticipationRequestDto> updatedRequestsDto) {
        List<ParticipationRequest> updatedRequests = updatedRequestsDto.stream()
                .map(RequestMapper::toUpdatedEntity)
                .toList();

        return requestRepository.saveAll(updatedRequests).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    //Проверка существования пользователя
    private Long checkUserExisting(Long userId) {
        if (!userIntegrationService.checkUserExisting(userId)) {
            throw new NotFoundException("User with id " + userId + " not found.");
        }
        return userId;
    }

    //Получение запроса
    private ParticipationRequest findParticipationRequest(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Request with id " + requestId + " not found")
        );
    }

}