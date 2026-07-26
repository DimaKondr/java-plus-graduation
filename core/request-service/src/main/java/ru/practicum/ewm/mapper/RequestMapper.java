package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.dto.request.RequestStatus;

import java.time.LocalDateTime;

@UtilityClass
public class RequestMapper {

    //Преобразование в сущность
    public ParticipationRequest toEntity(LocalDateTime nowData, Long eventId, Long requesterId, RequestStatus status) {
        return ParticipationRequest.builder()
                .created(nowData)
                .eventId(eventId)
                .requesterId(requesterId)
                .status(status)
                .build();
    }

    //Преобразование в dto
    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest req) {
        return ParticipationRequestDto.builder()
                .id(req.getId())
                .created(req.getCreated().format(Constants.FORMATTER))
                .event(req.getEventId())
                .requester(req.getRequesterId())
                .status(req.getStatus().toString())
                .build();
    }

    //Обновление сущности
    public ParticipationRequest toUpdatedEntity(ParticipationRequestDto dto) {
        return ParticipationRequest.builder()
                .id(dto.getId())
                .created(LocalDateTime.parse(dto.getCreated(),Constants.FORMATTER))
                .eventId(dto.getEvent())
                .requesterId(dto.getRequester())
                .status(RequestStatus.valueOf(dto.getStatus()))
                .build();
    }

}