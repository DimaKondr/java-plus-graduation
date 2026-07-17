package ru.practicum.ewm.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.constants.Constants;
import ru.practicum.ewm.dto.event.AdminEventRequestParam;
import ru.practicum.ewm.dto.event.PublicEventRequestParam;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.model.event.QEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomEventRepositoryImpl implements CustomEventRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (param.getUsers() != null && !param.getUsers().isEmpty()) {
            builder.and(QEvent.event.initiator.id.in(param.getUsers()));
        }

        if (param.getStates() != null && !param.getStates().isEmpty()) {
            builder.and(QEvent.event.state.stringValue().in(param.getStates()));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            builder.and(QEvent.event.category.id.in(param.getCategories()));
        }

        if (param.getRangeStart() != null && !param.getRangeStart().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), Constants.FORMATTER);
            builder.and(QEvent.event.eventDate.goe(start));
        }

        if (param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), Constants.FORMATTER);
            builder.and(QEvent.event.eventDate.loe(end));
        }

        return queryFactory
                .selectFrom(QEvent.event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(QEvent.event.eventDate.asc())
                .fetch();
    }

    @Override
    public List<Event> findByPublicRequest(PublicEventRequestParam param, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (param.getText() != null && !param.getText().isBlank()) {
            builder.and(QEvent.event.annotation.containsIgnoreCase(param.getText())
                    .or(QEvent.event.description.containsIgnoreCase(param.getText())));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            builder.and(QEvent.event.category.id.in(param.getCategories()));
        }

        if (param.getPaid() != null) {
            builder.and(QEvent.event.paid.eq(param.getPaid()));
        }

        if (param.getRangeStart() != null && !param.getRangeStart().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), Constants.FORMATTER);
            builder.and(QEvent.event.eventDate.goe(start));
        }

        if (param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), Constants.FORMATTER);
            builder.and(QEvent.event.eventDate.loe(end));
        }

        if ((param.getRangeStart() == null || param.getRangeStart().isBlank())
                && (param.getRangeEnd() == null || param.getRangeEnd().isBlank())) {
            builder.and(QEvent.event.eventDate.goe(LocalDateTime.now()));
        }

        return queryFactory
                .selectFrom(QEvent.event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(QEvent.event.eventDate.asc())
                .fetch();
    }

}