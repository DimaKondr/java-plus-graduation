package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.event.AdminEventRequestParam;
import ru.practicum.ewm.dto.event.PublicEventRequestParam;
import ru.practicum.ewm.model.event.Event;

import java.util.List;

public interface CustomEventRepository {

    List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable);

    List<Event> findByPublicRequest(PublicEventRequestParam param, Pageable pageable);

}