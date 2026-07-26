package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.dto.request.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "request")
@AllArgsConstructor
@NoArgsConstructor
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}