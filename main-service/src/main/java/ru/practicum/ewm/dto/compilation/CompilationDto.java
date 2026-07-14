package ru.practicum.ewm.dto.compilation;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import ru.practicum.ewm.dto.event.EventShortDto;

import java.util.List;

@Value
@Builder
@Getter
public class CompilationDto {

    Long id;
    List<EventShortDto> events;
    Boolean pinned;
    String title;

}
