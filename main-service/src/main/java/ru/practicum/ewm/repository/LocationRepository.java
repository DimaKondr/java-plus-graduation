package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.model.event.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}