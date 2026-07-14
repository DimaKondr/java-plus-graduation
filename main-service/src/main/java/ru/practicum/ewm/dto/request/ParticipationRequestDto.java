package ru.practicum.ewm.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
@Builder
public class ParticipationRequestDto {

    Long id;
    String created;
    Long event;
    Long requester;
    String status;

}
