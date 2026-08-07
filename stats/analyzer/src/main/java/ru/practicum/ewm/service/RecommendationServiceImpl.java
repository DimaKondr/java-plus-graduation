package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        if (maxResults <= 0) {
            log.error("Количество рекомендованных событий должно быть больше нуля: {}.", userId);
            return new ArrayList<>();
        }

        List<UserAction> userActions = userActionRepository.findAllByUserIdOrderByTsDesc(userId);
        if (userActions.isEmpty()) {
            log.info("Пользователь с ID: {} пока не взаимодействовал с событиями.", userId);
            return new ArrayList<>();
        }

        List<Long> recentInteractedEvents = userActions.stream()
                .map(UserAction::getEventId)
                .limit(maxResults)
                .toList();

        List<EventSimilarity> similarEvents = eventSimilarityRepository
                .findByEvent1InOrEvent2InOrderBySimilarityDesc(recentInteractedEvents);

        Map<Long, BigDecimal> recomendedEvents = new HashMap<>();

        for (EventSimilarity event : similarEvents) {
            Long event1 = event.getEvent1();
            Long event2 = event.getEvent2();
            BigDecimal score = event.getSimilarity();

            boolean hasEvent1 = recentInteractedEvents.contains(event1);
            boolean hasEvent2 = recentInteractedEvents.contains(event2);

            if (hasEvent1 != hasEvent2) {
                Long candidateEventId = hasEvent1 ? event2 : event1;
                if (!recentInteractedEvents.contains(candidateEventId)) {
                    recomendedEvents.put(candidateEventId, score);
                }
            }
        }

        List<RecommendedEventProto> result = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : recomendedEvents.entrySet()) {
            Long candidateEventId = entry.getKey();

            List<EventSimilarity> nearEvent = eventSimilarityRepository
                    .findByEvent1OrEvent2OrderBySimilarityDesc(candidateEventId)
                    .stream()
                    .filter(similarity -> {
                        Long neighborId = similarity.getEvent1().equals(candidateEventId)
                                ? similarity.getEvent2()
                                : similarity.getEvent1();
                        return recentInteractedEvents.contains(neighborId);
                    })
                    .limit(maxResults)
                    .toList();

            if (nearEvent.isEmpty()) {
                continue;
            }

            BigDecimal weightedSum = BigDecimal.valueOf(0);
            BigDecimal similaritySum = BigDecimal.valueOf(0);

            for (EventSimilarity neighbor : nearEvent) {
                Long neighborId = neighbor.getEvent1().equals(candidateEventId)
                        ? neighbor.getEvent2()
                        : neighbor.getEvent1();

                BigDecimal rating = userActions.stream()
                        .filter(action -> action.getEventId().equals(neighborId))
                        .map(UserAction::getRating)
                        .findFirst()
                        .orElse(BigDecimal.valueOf(0));

                weightedSum = weightedSum.add(neighbor.getSimilarity().multiply(rating));
                similaritySum = similaritySum.add(neighbor.getSimilarity());
            }

            BigDecimal predictedScore =
                    weightedSum.compareTo(BigDecimal.ZERO) > 0
                            ? weightedSum.divide(similaritySum, 2, RoundingMode.HALF_UP) : BigDecimal.valueOf(0);


            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(candidateEventId)
                    .setScore(predictedScore.doubleValue())
                    .build()
            );
        }

        return result;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        if (maxResults <= 0) {
            log.error("Количество похожих событий должно быть больше нуля: {}.", userId);
            return new ArrayList<>();
        }

        List<UserAction> userActions = userActionRepository.findAllByUserId(userId);
        List<Long> interactedEvents = userActions.stream()
                .map(UserAction::getEventId)
                .toList();

        List<EventSimilarity> similarEvents = eventSimilarityRepository
                .findByEvent1OrEvent2OrderBySimilarityDesc(eventId);

        List<RecommendedEventProto> result = new ArrayList<>();
        for (EventSimilarity event : similarEvents) {
            Long similarEventId = event.getEvent1().equals(eventId)
                    ? event.getEvent2()
                    : event.getEvent1();

            if (!interactedEvents.contains(similarEventId)) {
                result.add(RecommendedEventProto.newBuilder()
                        .setEventId(Math.toIntExact(similarEventId))
                        .setScore(event.getSimilarity().doubleValue())
                        .build());

                if (result.size() >= maxResults) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.error("Поступивший список событий не может быть null или пустым: {}.", eventIds);
            return new ArrayList<>();
        }

        List<Long> uniqueEventIds = eventIds.stream().distinct().toList();
        List<UserAction> usersActions = userActionRepository.findAllByEventIdIn(uniqueEventIds);

        Map<Long, BigDecimal> eventScore = new HashMap<>();

        for (UserAction userAction : usersActions) {
            BigDecimal rating = userAction.getRating();

            if (rating != null) {
                eventScore.merge(
                        userAction.getEventId(),
                        rating,
                        BigDecimal::add
                );
            }
        }

        List<RecommendedEventProto> result = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : eventScore.entrySet()) {
            result.add(
                    RecommendedEventProto.newBuilder()
                            .setEventId(entry.getKey())
                            .setScore(entry.getValue().doubleValue())
                            .build()
            );
        }

        return result;
    }

}