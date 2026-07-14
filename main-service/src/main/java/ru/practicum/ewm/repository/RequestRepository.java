package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.dto.event.ConfirmedRequestCount;
import ru.practicum.ewm.model.request.ParticipationRequest;
import ru.practicum.ewm.model.request.RequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("""
            select r
            from ParticipationRequest r
            where r.requester.id = :userId
            """)
    List<ParticipationRequest> findAllByUserId(long userId);

    @Query("""
            UPDATE ParticipationRequest r
            SET r.status = :state
            WHERE r.id = :requestId
            """)
    int changeState(long requestId, RequestStatus state);

    Long countByEvent_IdAndStatus(Long eventId, RequestStatus status);

    @Query("select new ru.practicum.ewm.dto.event.ConfirmedRequestCount(r.event.id, count(r.id)) " +
            "from ParticipationRequest r " +
            "where r.event.id in :eventIds and r.status = :status " +
            "group by r.event.id")
    List<ConfirmedRequestCount> countConfirmedRequestsByEventIds(List<Long> eventIds, RequestStatus status);

    List<ParticipationRequest> findAllByEvent_Id(Long eventId);

    Optional<ParticipationRequest> findByRequester_IdAndEvent_Id(Long userId, Long eventId);
}