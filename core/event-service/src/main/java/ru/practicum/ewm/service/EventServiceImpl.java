package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.AnalyzerGrpcClient;
import ru.practicum.ewm.CollectorGrpcClient;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.event.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.exception.CreationRulesException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.event.*;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.repository.*;
import ru.practicum.ewm.service.integration.RequestIntegrationService;
import ru.practicum.ewm.service.integration.UserIntegrationService;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final RequestIntegrationService requestIntegrationService;
    private final UserIntegrationService userIntegrationService;
    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final CollectorGrpcClient collectorGrpcClient;

    @Transactional
    @Override
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(() -> new NotFoundException(
                "Добавление события. Категория с ID: " + dto.getCategory() + " не найдена."));

        if (!userIntegrationService.checkUserExisting(userId)) {
            throw new NotFoundException("Добавление события. Пользователь с ID: " + userId + " не найден.");
        }

        Location location = locationRepository.save(LocationMapper.dtoToLocation(dto.getLocation()));

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            log.error("Добавление события. " +
                    "Время начала события должно быть не ранее, чем через два часа от текущего момента.");
            throw new ValidationException("Время начала события должно быть не ранее, " +
                    "чем через два часа от текущего момента.");
        }

        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }

        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0);
        }

        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }

        Event newEvent = EventMapper.dtoToEvent(
                dto,
                category,
                LocalDateTime.now(),
                userId,
                location,
                null,
                EventState.PENDING
        );

        newEvent.setCompilations(new ArrayList<>());

        UserShortDto user = userIntegrationService.getUserInfo(userId);

        Event addedEvent = eventRepository.save(newEvent);
        return EventMapper.eventToFullDto(addedEvent, 0L, 0.0, user);
    }

    @Override
    public List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size) {
        UserShortDto user = userIntegrationService.getUserInfo(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorIdOrderByEventDateAsc(userId, pageable);
        if (events == null || events.isEmpty()) {
            log.info("Получение списка событий пользователя. " +
                    "По заданным параметрам (userId: {}, from: {}, size {}) события не найдены.", userId, from, size);
            return new ArrayList<>();
        }

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(events);
        Map<Long, Double> eventsScore = getScoreCount(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event,
                    confirmedRequests,
                    eventScore,
                    user
            );
            result.add(eventShortDto);
        }
        return result;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Получения данных о событии. Событие с ID: " + eventId + " не найдено."));

        if (!event.getInitiatorId().equals(userId)) {
            log.error("Получение данных о событии. " +
                    "Пользователь с ID: {} не является инициатором события с ID: {}", userId, eventId);
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(List.of(event));
        Map<Long, Double> eventsScore = getScoreCount(List.of(eventId));

        Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
        Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);

        UserShortDto user = userIntegrationService.getUserInfo(userId);

        return EventMapper.eventToFullDto(event, confirmedRequests, eventScore, user);
    }

    @Override
    public List<EventShortDto> getShortEventsInfoByIds(List<Long> eventIds) {
        List<Event> events = eventRepository.findAllByIdInOrderByIdAsc(eventIds);

        if (events == null || events.isEmpty()) {
            log.info("Получение событий по списку ID. По указанным в списке ID событий события не найдены.");
            return new ArrayList<>();
        }

        List<Long> usersIds = new ArrayList<>();
        for (Event event : events) {
            usersIds.add(event.getInitiatorId());
        }

        Map<Long, UserShortDto> usersInfoMap = userIntegrationService.getInfoOfUsers(usersIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(events);
        Map<Long, Double> eventsScore = getScoreCount(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event,
                    confirmedRequests,
                    eventScore,
                    usersInfoMap.get(event.getInitiatorId())
            );
            result.add(eventShortDto);
        }
        return result;
    }

    @Transactional
    @Override
    public EventFullDto patchEventById(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event oldEvent = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Обновление данных события. Событие с ID: " + eventId + " не найдено."));

        if (!oldEvent.getInitiatorId().equals(userId)) {
            log.error("Обновление данных события. " +
                    "Пользователь с ID: {} не является инициатором события с ID: {}", userId, eventId);
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        if (oldEvent.getState().equals(EventState.PUBLISHED)) {
            log.error("Обновление данных события. Изменить можно только отмененные события " +
                    "или события в состоянии ожидания модерации.");
            throw new CreationRulesException("Изменить можно только отмененные события " +
                    "или события в состоянии ожидания модерации.");
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);

            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                log.error("Обновление данных события. Время начала события должно быть не ранее, " +
                        "чем через два часа от текущего момента.");
                throw new ValidationException("Время начала события должно быть не ранее, " +
                        "чем через два часа от текущего момента.");
            }

            oldEvent.setEventDate(eventDate);
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(() -> new NotFoundException(
                    "Обновление данных события. Категория с ID: " + dto.getCategory() + " не найдена."));

            oldEvent.setCategory(category);
        }

        if (dto.getLocation() != null) {
            oldEvent.getLocation().setLat(dto.getLocation().getLat());
            oldEvent.getLocation().setLon(dto.getLocation().getLon());
        }

        if (dto.getAnnotation() != null) {
            oldEvent.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            oldEvent.setDescription(dto.getDescription());
        }

        if (dto.getPaid() != null) {
            oldEvent.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            oldEvent.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction().equals(UserStateAction.SEND_TO_REVIEW.toString())) {
                oldEvent.setState(EventState.PENDING);
            }
            if (dto.getStateAction().equals(UserStateAction.CANCEL_REVIEW.toString())) {
                oldEvent.setState(EventState.CANCELED);
            }
        }

        if (dto.getTitle() != null) {
            oldEvent.setTitle(dto.getTitle());
        }

        Event patchedEvent = eventRepository.save(oldEvent);

        Map<Long, Long> confirmedRequestsCount =
                requestIntegrationService.getConfirmedRequestsCount(List.of(patchedEvent));
        Map<Long, Double> eventsScore = getScoreCount(List.of(eventId));

        Double eventScore = eventsScore.getOrDefault(patchedEvent.getId(), 0.0);
        Long confirmedRequests = confirmedRequestsCount.getOrDefault(patchedEvent.getId(), 0L);

        UserShortDto user = userIntegrationService.getUserInfo(userId);

        return EventMapper.eventToFullDto(patchedEvent, confirmedRequests, eventScore, user);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsOfEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Получение запросов на участие в событии. Событие с ID: " + eventId + " не найдено."));
        if (!event.getInitiatorId().equals(userId)) {
            log.error("Получение запросов на участие в событии. " +
                    "Пользователь с ID: {} не является инициатором события с ID: {}", userId, eventId);
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        List<ParticipationRequestDto> requests = requestIntegrationService.getRequests(eventId);
        if (requests == null || requests.isEmpty()) {
            log.info("Получение запросов на участие в событии. По событию с ID: {} запросов не найдено.", eventId);
            return new ArrayList<>();
        }

        return requests;
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult patchRequestsStatusOfEvent(Long userId, Long eventId,
                                                                     EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Обновление данных события. Событие с ID: " + eventId + " не найдено."));

        if (!event.getInitiatorId().equals(userId)) {
            log.error("Обновление статусов заявок. " +
                    "Пользователь с ID: {} не является инициатором события с ID: {}", userId, eventId);
            throw new NotFoundException("Пользователь с ID: " +
                    userId + " не является инициатором события с ID: " + eventId);
        }

        Long participantLimit = event.getParticipantLimit().longValue();
        Long approvedRequestsCount = requestIntegrationService.getRequestsCount(eventId, RequestStatus.CONFIRMED);

        if (participantLimit > 0 && approvedRequestsCount >= participantLimit) {
            log.error("Обновление статусов заявок. " +
                    "Достигнут лимит одобренных заявок ({}), нельзя подтвердить больше.", approvedRequestsCount);
            throw new ConflictException("Достигнут лимит участников события");
        }

        List<ParticipationRequestDto> allRequests = requestIntegrationService.getRequestsByList(dto.getRequestIds());

        if (allRequests.size() != dto.getRequestIds().size()) {
            log.error("Обновление статусов заявок. Некоторые заявки не найдены.");
            throw new NotFoundException("Некоторые заявки не найдены");
        }

        for (ParticipationRequestDto request : allRequests) {
            if (!request.getStatus().equals(RequestStatus.PENDING.name())) {
                log.error("Обновление статусов заявок. Заявка с ID: {} имеет статус {}, а не PENDING",
                        request.getId(), request.getStatus());
                throw new CreationRulesException("Статус можно изменить только у заявок, " +
                        "находящихся в состоянии ожидания: " + RequestStatus.PENDING +
                        ". Текущий статус заявки: " + request.getStatus());
            }
        }

        List<ParticipationRequestDto> requests = allRequests;

        if (dto.getStatus() == RequestStatus.REJECTED) {
            // Отклоняем все заявки
            for (ParticipationRequestDto request : requests) {
                request.setStatus(RequestStatus.REJECTED.name());
            }

            List<ParticipationRequestDto> resultRejectedRequestsDto =
                    requestIntegrationService.updateRequests(requests);

            return new EventRequestStatusUpdateResult(List.of(), resultRejectedRequestsDto);
        }

        if (dto.getRequestIds().size() != requests.size()) {
            if (participantLimit.equals(approvedRequestsCount) && participantLimit > 0) {
                log.error("Обновление статусов заявок на участие в событии. " +
                        "Достигнут лимит одобренных заявок в событии с ID: {}.", eventId);
                throw new CreationRulesException("Достигнут лимит одобренных заявок в событии с ID: " + eventId + ".");
            }
        }

        List<ParticipationRequestDto> approvedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (ParticipationRequestDto request : requests) {
            if (approvedRequestsCount < participantLimit || participantLimit == 0) {
                request.setStatus(RequestStatus.CONFIRMED.name());
                approvedRequests.add(request);
                approvedRequestsCount++;
            } else {
                request.setStatus(RequestStatus.REJECTED.name());
                rejectedRequests.add(request);
            }
        }

        List<ParticipationRequestDto> resultApprovedRequestsDto =
                requestIntegrationService.updateRequests(approvedRequests);

        List<ParticipationRequestDto> resultRejectedRequestsDto =
                requestIntegrationService.updateRequests(rejectedRequests);

        return new EventRequestStatusUpdateResult(resultApprovedRequestsDto, resultRejectedRequestsDto);
    }

    @Override
    public List<EventFullDto> getEventsByAdminRequest(AdminEventRequestParam param) {
        log.info("Уровень Admin. Получение списка событий по параметрам: users={}, states={}, categories={}, " +
                        "rangeStart={}, rangeEnd={}, from={}, size={}",
                param.getUsers(), param.getStates(), param.getCategories(),
                param.getRangeStart(), param.getRangeEnd(), param.getFrom(), param.getSize());

        boolean hasInvalidUsers = param.getUsers() != null && param.getUsers().isEmpty();
        boolean hasInvalidCategories = param.getCategories() != null && param.getCategories().isEmpty();

        if (hasInvalidUsers || hasInvalidCategories) {
            log.info("Уровень Admin. Переданы невалидные ID (0 или отрицательные). Возвращаем пустой список.");
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        List<Event> events = eventRepository.findByAdminRequest(param, pageable);

        if (events == null || events.isEmpty()) {
            log.info("Уровень Admin. Получение списка событий. По заданным параметрам события не найдены.");
            return new ArrayList<>();
        }

        List<Long> usersIds = new ArrayList<>();
        for (Event event : events) {
            usersIds.add(event.getInitiatorId());
        }

        Map<Long, UserShortDto> usersInfoMap = userIntegrationService.getInfoOfUsers(usersIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(events);
        //Map<Long, Long> viewsStats = getScoreCount(events);
        Map<Long, Double> eventsScore = getScoreCount(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );

        List<EventFullDto> result = new ArrayList<>();
        for (Event event : events) {
            Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            EventFullDto eventFullDto = EventMapper.eventToFullDto(
                    event,
                    confirmedRequests,
                    eventScore,
                    usersInfoMap.get(event.getInitiatorId())
            );
            result.add(eventFullDto);
        }

        return result;
    }

    @Transactional
    @Override
    public EventFullDto patchEventByIdByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event oldEvent = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Уровень Admin. Обновление данных события. Событие с ID: " + eventId + " не найдено."));

        log.info("Публикация события. Текущий статус: {}, eventDate: {}",
                oldEvent.getState(), oldEvent.getEventDate());

        if (dto.getStateAction() != null) {
            if (dto.getStateAction().equals(AdminStateAction.PUBLISH_EVENT.toString())
                    && oldEvent.getState().equals(EventState.PENDING)) {
                oldEvent.setState(EventState.PUBLISHED);
                oldEvent.setPublishedOn(LocalDateTime.now());
            } else if (dto.getStateAction().equals(AdminStateAction.REJECT_EVENT.toString())
                    && !oldEvent.getState().equals(EventState.PUBLISHED)) {
                oldEvent.setState(EventState.CANCELED);
            } else {
                log.error("Уровень Admin. Обновление данных события. " +
                        "Опубликовать можно только событие, ожидающее публикации. " +
                        "Отклонить можно только событие, которое не опубликовано.");
                throw new CreationRulesException("Обновление данных события администратором. " +
                        "Опубликовать можно только событие, ожидающее публикации. " +
                        "Отклонить можно только событие, которое не опубликовано.");
            }
        }

        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);

            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                log.error("Уровень Admin. Обновление данных события. " +
                        "Время начала события должно быть не ранее, чем через час от текущего момента.");
                throw new ValidationException("Обновление данных события администратором. " +
                        "Время начала события должно быть не ранее, чем через час от текущего момента.");
            }

            oldEvent.setEventDate(eventDate);
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(() -> new NotFoundException(
                    "Обновление данных события администратором. " +
                            "Категория с ID: " + dto.getCategory() + " не найдена."));

            oldEvent.setCategory(category);
        }

        if (dto.getLocation() != null) {
            if (oldEvent.getLocation() != null) {
                oldEvent.getLocation().setLat(dto.getLocation().getLat());
                oldEvent.getLocation().setLon(dto.getLocation().getLon());
            } else {
                Location newLocation = locationRepository.save(LocationMapper.dtoToLocation(dto.getLocation()));
                oldEvent.setLocation(newLocation);
            }
        }

        if (dto.getAnnotation() != null) {
            oldEvent.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            oldEvent.setDescription(dto.getDescription());
        }

        if (dto.getPaid() != null) {
            oldEvent.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            oldEvent.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getTitle() != null) {
            oldEvent.setTitle(dto.getTitle());
        }

        Event patchedEvent = eventRepository.save(oldEvent);
        log.info("После сохранения: статус = {}, publishedOn = {}",
                patchedEvent.getState(), patchedEvent.getPublishedOn());

        Map<Long, Long> confirmedRequestsCount =
                requestIntegrationService.getConfirmedRequestsCount(List.of(patchedEvent));
        Map<Long, Double> eventsScore = getScoreCount(List.of(patchedEvent.getId()));

        Double eventScore = eventsScore.getOrDefault(patchedEvent.getId(), 0.0);
        Long confirmedRequests = confirmedRequestsCount.getOrDefault(patchedEvent.getId(), 0L);

        UserShortDto user = userIntegrationService.getUserInfo(patchedEvent.getInitiatorId());

        return EventMapper.eventToFullDto(patchedEvent, confirmedRequests, eventScore, user);
    }

    @Override
    public List<EventShortDto> getEventsByPublicRequest(PublicEventRequestParam param) {
        log.info("Публичный поиск событий: {}", param);

        Integer from = param.getFrom() != null ? param.getFrom() : 0;
        Integer size = param.getSize() != null && param.getSize() > 0 ? param.getSize() : 10;

        if (size <= 0) {
            size = 10;
        }
        if (from < 0) {
            from = 0;
        }

        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;

        if (param.getRangeStart() != null && !param.getRangeStart().isBlank()) {
            rangeStart = LocalDateTime.parse(param.getRangeStart(), Constants.FORMATTER);
        }
        if (param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            rangeEnd = LocalDateTime.parse(param.getRangeEnd(), Constants.FORMATTER);
        }

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала диапазона не может быть позже даты окончания");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByPublicRequest(param, pageable);

        if (events == null || events.isEmpty()) {
            log.info("Уровень Public. Получение списка событий. По заданным параметрам " +
                    "(param: {}, from: {}, size {}) события не найдены.", param, from, size);
            return new ArrayList<>();
        }

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(events);

        if (Boolean.TRUE.equals(param.getOnlyAvailable())) {
            Predicate<Event> isAvailable = event -> event.getParticipantLimit() >
                    confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            events = events.stream()
                    .filter(isAvailable)
                    .toList();
        }

        List<Long> usersIds = new ArrayList<>();
        for (Event event : events) {
            usersIds.add(event.getInitiatorId());
        }

        Map<Long, UserShortDto> usersInfoMap = userIntegrationService.getInfoOfUsers(usersIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        Map<Long, Double> eventsScore = getScoreCount(
                events.stream()
                        .map(Event::getId)
                        .toList()
        );

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event,
                    confirmedRequests,
                    eventScore,
                    usersInfoMap.get(event.getInitiatorId())
            );
            result.add(eventShortDto);
        }

        String sort = param.getSort();
        if (sort != null && sort.equalsIgnoreCase("rating")) {
            return result.stream()
                    .sorted(Comparator.comparingDouble(EventShortDto::getRating).reversed())
                    .toList();
        } else {
            return result;
        }
    }

    @Override
    public EventFullDto getEventByIdByPublicRequest(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Публичный запрос на получение данных о событии. Событие с ID: " + eventId + " не найдено."));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            log.error("Уровень Public. Можно получить данные только события со статусом {}.", EventState.PUBLISHED);
            throw new NotFoundException("Публичный запрос на получение данных о событии. " +
                    "Можно получить данные только опубликованного события.");
        }

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(List.of(event));
        Map<Long, Double> eventsScore = getScoreCount(List.of(event.getId()));

        Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
        Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);

        UserShortDto user = userIntegrationService.getUserInfo(event.getInitiatorId());

        return EventMapper.eventToFullDto(event, confirmedRequests, eventScore, user);
    }

    @Override
    public Boolean isEventExist(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public EventFullDto getEventByIdForMicroservice(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(
                "Получения данных о событии для межсервисного взаимодействия. " +
                        "Событие с ID: " + eventId + " не найдено."));

        Map<Long, Long> confirmedRequestsCount = requestIntegrationService.getConfirmedRequestsCount(List.of(event));
        Map<Long, Double> eventsScore = getScoreCount(List.of(event.getId()));

        Double eventScore = eventsScore.getOrDefault(event.getId(), 0.0);
        Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);

        UserShortDto user = userIntegrationService.getUserInfo(event.getInitiatorId());

        return EventMapper.eventToFullDto(event, confirmedRequests, eventScore, user);
    }

    @Override
    public List<EventFullDto> getRecommendationForUser(Long userId) {
        List<RecommendedEventProto> recommendedEvents = analyzerGrpcClient
                .getRecommendationsForUser(userId, 20)
                .toList();

        Map<Long, Double> recommendedEventScoreMap = new HashMap<>();

        recommendedEvents.forEach(recommendedEventProto -> {
            recommendedEventScoreMap.putIfAbsent(recommendedEventProto.getEventId(), recommendedEventProto.getScore());
        });

        List<Event> events = eventRepository.findAllByIdIn(recommendedEventScoreMap.keySet().stream().toList());

        Map<Long, Long> confirmedRequestsCountsMap = requestIntegrationService.getConfirmedRequestsCount(events);

        List<UserShortDto> initiators = userIntegrationService.getInfoOfUsers(
                events.stream()
                        .map(Event::getInitiatorId)
                        .toList()
        );

        Map<Long, UserShortDto> eventInitiatorsMap = initiators.stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        Function.identity()
                        )
                );

        return events.stream()
                .map(event -> {
                    Double eventScore = recommendedEventScoreMap.get(event.getId());
                    Long confirmedRequests = confirmedRequestsCountsMap.getOrDefault(event.getId(), 0L);
                    UserShortDto initiator = eventInitiatorsMap.get(event.getInitiatorId());

                    return EventMapper.eventToFullDto(event, confirmedRequests, eventScore, initiator);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void sendLikeOfEvent(Long userId, Long eventId, ActionTypeProto actionType) {
        List<ParticipationRequestDto> requests = requestIntegrationService.getRequests(eventId);
        boolean isParticipated = requests.stream()
                .anyMatch(request -> request.getRequester().equals(userId) &&
                        request.getStatus().equals(RequestStatus.CONFIRMED.name()));

        if (!isParticipated) {
            log.error("Пользователь с ID: {} не принимал участие в событии с ID: {}.", userId, eventId);
            throw new ValidationException("Только участвовавший в событии пользователь " +
                    "может поставить лайк событию.");
        }

        collectorGrpcClient.collectUserAction(userId, eventId, actionType);
    }

    private Map<Long, Double> getScoreCount(List<Long> eventsIds) {
        if (eventsIds.isEmpty()) return Collections.emptyMap();

        return analyzerGrpcClient.getInteractionsCount(eventsIds)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
    }

}