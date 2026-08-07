package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.dto.event.EventState;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>,
        QuerydslPredicateExecutor<Event>, CustomEventRepository {

    List<Event> findAllByInitiatorIdOrderByEventDateAsc(Long initiatorId, Pageable pageable);

    List<Event> findAllByIdInOrderByIdAsc(List<Long> eventsIds);

    List<Event> findAllByIdIn(List<Long> eventsIds);

    boolean existsByIdAndState(Long id, EventState state);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e WHERE e.category.id = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

}