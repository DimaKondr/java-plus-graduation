package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.dto.request.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("""
            select r
            from ParticipationRequest r
            where r.requesterId = :userId
            """)
    List<ParticipationRequest> findAllByUserId(long userId);

    @Query("""
            UPDATE ParticipationRequest r
            SET r.status = :state
            WHERE r.id = :requestId
            """)
    int changeState(long requestId, RequestStatus state);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select new ru.practicum.ewm.dto.event.ConfirmedRequestCount(r.eventId, count(r.id)) " +
            "from ParticipationRequest r " +
            "where r.eventId in :eventIds and r.status = :status " +
            "group by r.eventId")
    List<ConfirmedRequestCount> countConfirmedRequestsByEventIds(List<Long> eventIds, RequestStatus status);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.eventId = :eventId AND r.status = :requestStatus")
    Long getRequestsCountByIdAndStatus(@Param("eventId") Long eventId,
                                       @Param("requestStatus") RequestStatus requestStatus);

}