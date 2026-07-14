package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.dto.compilation.CreateCompilationDto;
import ru.practicum.ewm.dto.compilation.GetManyCompilationDto;
import ru.practicum.ewm.dto.compilation.UpdateCompilationDto;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(CreateCompilationDto dto);

    CompilationDto updateCompilation(UpdateCompilationDto dto);

    void removeCompilation(Long compId);

    List<CompilationDto> getCompilations(GetManyCompilationDto dto);

    CompilationDto getCompilationById(Long compId);
}
