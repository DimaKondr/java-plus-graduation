package ru.practicum.ewm.dto.event;

import lombok.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;

import java.util.List;

@Value
@Builder
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class EventRequestStatusUpdateResult {
    List<ParticipationRequestDto> confirmedRequests;
    List<ParticipationRequestDto> rejectedRequests;
}