package ru.practicum.ewm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventState;
import ru.practicum.ewm.dto.request.CreateUpdateRequestDto;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.dto.request.RequestStatus;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.repository.RequestRepository;
import ru.practicum.ewm.service.RequestServiceImpl;
import ru.practicum.ewm.service.integration.EventIntegrationService;
import ru.practicum.ewm.service.integration.UserIntegrationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestServiceImpl Unit Tests")
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventIntegrationService eventIntegrationService;

    @Mock
    private UserIntegrationService userIntegrationService;

    @InjectMocks
    private RequestServiceImpl requestService;

    private UserShortDto testUser;
    private UserShortDto eventInitiator;
    private EventFullDto testEvent;
    private ParticipationRequest testRequest;
    private CreateUpdateRequestDto createDto;

    @BeforeEach
    void setUp() {
        eventInitiator = new UserShortDto(
                10L,
                "Event Initiator"
        );

        testUser = new UserShortDto(
                1L,
                "Test User"
        );

        testEvent = EventFullDto.builder()
                .id(2L)
                .title("Test Event")
                .eventDate(LocalDateTime.now().plusDays(5).toString())
                .state(EventState.PUBLISHED.name())
                .initiator(eventInitiator)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        testRequest = ParticipationRequest.builder()
                .id(3L)
                .created(LocalDateTime.now())
                .eventId(testEvent.getId())
                .requesterId(testUser.getId())
                .status(RequestStatus.PENDING)
                .build();

        createDto = CreateUpdateRequestDto.builder()
                .userId(1L)
                .eventId(2L)
                .build();
    }

    @Test
    @DisplayName("Создание заявки - успешный сценарий")
    void createRequest_Success() {
        when(eventIntegrationService.getEventInfo(2L)).thenReturn(testEvent);
        when(userIntegrationService.checkUserExisting(1L)).thenReturn(true);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(testRequest);

        ParticipationRequestDto result = requestService.createRequest(createDto);

        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals(2L, result.getEvent());
        assertEquals(1L, result.getRequester());
        assertEquals("PENDING", result.getStatus());

        verify(userIntegrationService).checkUserExisting(1L);
        verify(eventIntegrationService).getEventInfo(2L);
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    @DisplayName("Получение списка заявок пользователя - успешный сценарий")
    void getRequestByUserId_Success() {
        when(requestRepository.findAllByUserId(1L)).thenReturn(List.of(testRequest));
        when(userIntegrationService.checkUserExisting(1L)).thenReturn(true);

        List<ParticipationRequestDto> result = requestService.getRequestByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(2L, result.get(0).getEvent());

        verify(requestRepository).findAllByUserId(1L);
    }

    @Test
    @DisplayName("Получение списка заявок - пользователь не найден (404)")
    void getRequestByUserId_UserNotFound() {
        assertThrows(NotFoundException.class, () -> requestService.getRequestByUserId(1L));

        verify(requestRepository, never()).findAllByUserId(1L);
    }

    @Test
    @DisplayName("Отмена заявки - пользователь не найден (404)")
    void canceledRequest_UserNotFound() {
        CreateUpdateRequestDto cancelDto = CreateUpdateRequestDto.builder()
                .eventId(2L)
                .userId(1L)
                .build();

        assertThrows(NotFoundException.class, () -> requestService.canceledRequest(1L, 3L));

        verify(userIntegrationService).checkUserExisting(1L);
        verify(eventIntegrationService, never()).getEventInfo(any());
    }

}