package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.constants.AggregatorConstants;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final Map<Long, Map<Long, BigDecimal>> userActionOnEventMatrix = new ConcurrentHashMap<>();
    private final Map<Long, BigDecimal> totalEventWeightValue = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, BigDecimal>> minWeightsSums = new ConcurrentHashMap<>();

    @Override
    public List<EventSimilarityAvro> handleUserAction(UserActionAvro userAction) {
        log.info("Получены данные о взаимодействии пользователя с событием: {}.", userAction);

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();

        userActionOnEventMatrix.putIfAbsent(eventId, new ConcurrentHashMap<>());

        BigDecimal currentWeight = getUserWeight(eventId, userId);
        BigDecimal newWeight = getWeightValueByActionType(userAction.getActionType());

        if (newWeight.compareTo(currentWeight) <= 0) {
            log.info("Вес нового действия меньше и равен текущему весу. Текущий вес = {}, новый вес = {}. " +
                    "Пересчитывать сходство не требуется.", currentWeight, newWeight);
            return new ArrayList<>();
        }

        userActionOnEventMatrix
                .computeIfAbsent(eventId, id -> new HashMap<>())
                .put(userId, newWeight);
        log.info("Обновлена матрица весов действий пользователя с ID: {} для события с ID: {}. Новый вес = {}.",
                userId, eventId, newWeight);

        BigDecimal deltaWeight = newWeight.subtract(currentWeight);
        BigDecimal currentEventSum = totalEventWeightValue.getOrDefault(eventId, BigDecimal.ZERO);
        BigDecimal newEventSum = currentEventSum.add(deltaWeight);

        totalEventWeightValue.merge(eventId, deltaWeight, BigDecimal::add);
        log.info("Обновлены данные о сумме весов для события с ID: {}. Предыдущий вес: {}. Новый вес: {}.",
                eventId, currentEventSum, newEventSum);

        return updateSimilarity(userId, eventId, currentWeight, newWeight, userAction.getTimestamp());
    }

    private List<EventSimilarityAvro> updateSimilarity(
            Long userId,
            Long eventId,
            BigDecimal currentWeight,
            BigDecimal newWeight,
            Instant msgTimestamp
    ) {
        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : userActionOnEventMatrix.entrySet()) {
            Long otherEventId = entry.getKey();

            if (otherEventId.equals(eventId)) {
                continue;
            }

            Map<Long, BigDecimal> userWeightsForOtherEvent = entry.getValue();

            if (userWeightsForOtherEvent.containsKey(userId)) {
                BigDecimal otherUserWeight = userWeightsForOtherEvent.get(userId);

                BigDecimal oldMinContribution = currentWeight.min(otherUserWeight);
                BigDecimal newMinContribution = newWeight.min(otherUserWeight);
                BigDecimal deltaSMin = newMinContribution.subtract(oldMinContribution);

                Long firstKey = Math.min(eventId, otherEventId);
                Long secondKey = Math.max(eventId, otherEventId);

                minWeightsSums.putIfAbsent(firstKey, new ConcurrentHashMap<>());

                minWeightsSums.get(firstKey).merge(secondKey, deltaSMin, BigDecimal::add);
                log.info("Обновлена S_min для пары ({}, {}): {}", firstKey, secondKey, deltaSMin);

                BigDecimal sMin = minWeightsSums.get(firstKey).getOrDefault(secondKey, BigDecimal.ZERO);
                BigDecimal firstSum = totalEventWeightValue.getOrDefault(firstKey, BigDecimal.ZERO);
                BigDecimal secondSum = totalEventWeightValue.getOrDefault(secondKey, BigDecimal.ZERO);

                double similarity = 0.0;
                if (firstSum.compareTo(BigDecimal.ZERO) > 0 && secondSum.compareTo(BigDecimal.ZERO) > 0) {
                    similarity = calculateSimilarity(sMin, firstSum, secondSum);
                }

                EventSimilarityAvro eventSimilarityAvro = EventSimilarityAvro.newBuilder()
                        .setEventA(firstKey)
                        .setEventB(secondKey)
                        .setScore(similarity)
                        .setTimestamp(msgTimestamp)
                        .build();

                result.add(eventSimilarityAvro);
            }
        }
        return result;
    }

    private BigDecimal getUserWeight(Long eventId, Long userId) {
        Map<Long, BigDecimal> userWeights = userActionOnEventMatrix.get(eventId);
        return userWeights != null ? userWeights.getOrDefault(userId, BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    private double calculateSimilarity(BigDecimal updatedMinSum, BigDecimal firstSum, BigDecimal secondSum) {
        BigDecimal denominator = (
                (firstSum.sqrt(AggregatorConstants.MATH_CONTEXT))
                        .multiply((secondSum.sqrt(AggregatorConstants.MATH_CONTEXT)))
        );
        return (updatedMinSum.divide(denominator, 2, RoundingMode.HALF_UP)).doubleValue();
    }

    private BigDecimal getWeightValueByActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> AggregatorConstants.VIEW_WEIGHT;
            case REGISTER -> AggregatorConstants.REGISTER_WEIGHT;
            case LIKE -> AggregatorConstants.LIKE_WEIGHT;
        };
    }

}