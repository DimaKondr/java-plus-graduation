package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.request.CreateUpdateRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.EventState;
import ru.practicum.ewm.model.request.ParticipationRequest;
import ru.practicum.ewm.model.request.RequestStatus;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public ParticipationRequestDto createRequest(CreateUpdateRequestDto dto) {
        //Дата создания
        LocalDateTime now = LocalDateTime.now();
        //Получение сущностей для создания связей через JPA
        Event event = findEvent(dto.getEventId());
        User requester = findUser(dto.getUserId());

        //Проверка, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            log.error("Не удается создать запрос на неопубликованное событие с id={}", event.getId());
            throw new ConflictException("Событие еще не опубликовано");
        }
        //Проверка, что инициатор не пытается участвовать в своем событии
        if (event.getInitiator().getId().equals(requester.getId())) {
            log.error("Инициатор не может участвовать в собственном мероприятии. eventId={}, userId={}",
                    event.getId(), requester.getId());
            throw new ConflictException("Инициатор не может участвовать в собственном мероприятии");
        }

        //Проверка, что пользователь уже не создавал запрос
        Optional<ParticipationRequest> existingRequest =
                requestRepository.findByRequester_IdAndEvent_Id(requester.getId(), event.getId());

        if (existingRequest.isPresent()) {
            log.error("Запрос пользователя {} на событие {} уже существует",
                    requester.getId(), event.getId());
            throw new ConflictException(String.format("Запрос пользователя c id=%d на событие c id=%d уже существует",
                    requester.getId(), event.getId()));
        }

        // Проверка лимита участников
        Long approvedRequestsCount = requestRepository.countByEvent_IdAndStatus(
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

        ParticipationRequest request = RequestMapper.toEntity(now, event, requester, initialStatus);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создан запрос с id={}, статус={}", saved.getId(), initialStatus);
        return RequestMapper.toParticipationRequestDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        log.info("Получение запросов пользователя с id={}", userId);

        findUser(userId); // проверка существования

        return requestRepository.findAllByUserId(userId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    @Override
    public ParticipationRequestDto canceledRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);

        findUser(userId); // проверка существования

        ParticipationRequest request = findParticipationRequest(requestId);

        // Проверка, что запрос принадлежит пользователю
        if (!request.getRequester().getId().equals(userId)) {
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

    //Получение пользователя
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User with id " + userId + " not found")
        );
    }

    //Получение события
    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Event with id " + eventId + " not found")
        );
    }

    //Получение запроса
    private ParticipationRequest findParticipationRequest(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Request with id " + requestId + " not found")
        );
    }
}
