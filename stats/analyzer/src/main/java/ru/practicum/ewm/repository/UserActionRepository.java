package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.UserAction;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByEventIdIn(List<Long> eventId);

    List<UserAction> findAllByUserId(Long userId);

    @Query("SELECT ua FROM UserAction ua " +
            "WHERE ua.userId = :userId " +
            "ORDER BY ua.ts DESC")
    List<UserAction> findAllByUserIdOrderByTsDesc(@Param("userId") Long userId);
}