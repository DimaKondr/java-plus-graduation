package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.request.ParticipationRequest;
import ru.practicum.ewm.model.request.RequestStatus;

import java.time.LocalDateTime;

@UtilityClass
public class RequestMapper {

    //Преобразование в сущность
    public ParticipationRequest toEntity(LocalDateTime nowData, Event event, User requester, RequestStatus status) {
        return ParticipationRequest.builder()
                .created(nowData)
                .event(event)
                .requester(requester)
                .status(status)
                .build();
    }

    //Преобразование в dto
    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest req) {
        return ParticipationRequestDto.builder()
                .id(req.getId())
                .created(req.getCreated().format(Constants.FORMATTER))
                .event(req.getEvent().getId())
                .requester(req.getRequester().getId())
                .status(req.getStatus().toString())
                .build();
    }

}