package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.dto.request.CreateUpdateRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;

import java.util.List;

public interface RequestService {

    //    Создание нового запроса
    ParticipationRequestDto createRequest(CreateUpdateRequestDto dto);

    //    Получение всех запросов определённого пользователя
    List<ParticipationRequestDto> getRequestByUserId(Long userId);

    //    Отмена запроса на событие
    ParticipationRequestDto canceledRequest(Long userId, Long requestId);

    //    Подсчет подтвержденных запросов по списку id событий и статусу
    List<ConfirmedRequestCount> countConfirmedRequests(List<Long> eventIds, RequestStatus requestStatus);

    //    Получение заявок по id события
    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    //    Подсчет количества заявок по id события и статусу
    Long getRequestsCountByIdAndStatus(Long eventId, RequestStatus requestStatus);

    //    Получение списка заявок по списку их id
    List<ParticipationRequestDto> getRequests(List<Long> requestIds);

    //    Получение списка заявок по списку их id
    List<ParticipationRequestDto> updateRequests(List<ParticipationRequestDto> updatedRequests);

}