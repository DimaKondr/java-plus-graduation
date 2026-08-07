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

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final Map<Long, Map<Long, BigDecimal>> userActionOnEventMatrix = new HashMap<>();
    private final Map<Long, BigDecimal> totalEventWeightValue = new HashMap<>();
    private final Map<Long, Map<Long, BigDecimal>> minWeightsSums = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> handleUserAction(UserActionAvro userAction) {
        log.info("Получены данные о взаимодействии пользователя с событием: {}.", userAction);

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();

        BigDecimal currentWeight = getUserWeight(eventId, userId);
        BigDecimal newWeight = getWeightValueByActionType(userAction.getActionType());

        if (newWeight.compareTo(currentWeight) < 0) {
            log.info("Вес нового действия меньше текущего веса. Текущий вес = {}, новый вес = {}. " +
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
        totalEventWeightValue.put(eventId, newEventSum);
        log.info("Обновлены данные о сумме весов для события с ID: {}. Предыдущий вес: {}. Новый вес: {}.",
                eventId, currentEventSum, newEventSum);

        return updateSimilarity(userId, eventId, currentWeight, newWeight);
    }

    private List<EventSimilarityAvro> updateSimilarity(Long userId, Long eventId,
                                                       BigDecimal currentWeight, BigDecimal newWeight) {

        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Long otherEventId : totalEventWeightValue.keySet()) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            BigDecimal otherUserWeight = getUserWeight(otherEventId, userId);

            if (otherUserWeight.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            Long firstKey = Math.min(eventId, otherEventId);
            Long secondKey = Math.max(eventId, otherEventId);

            BigDecimal firstSum = totalEventWeightValue.getOrDefault(firstKey, BigDecimal.ZERO);
            BigDecimal secondSum = totalEventWeightValue.getOrDefault(secondKey, BigDecimal.ZERO);

            if (firstSum.compareTo(BigDecimal.ZERO) <= 0 || secondSum.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal minValue = newWeight.min(otherUserWeight);
            BigDecimal currentMinSum = getMinSum(firstKey, secondKey);
            BigDecimal updatedMinSum = currentMinSum.add(minValue.subtract(currentWeight.min(otherUserWeight)));

            minWeightsSums
                    .computeIfAbsent(firstKey, id -> new HashMap<>())
                    .put(secondKey, updatedMinSum);

            log.info("Обновлена S_min для пары ({}, {}): {}", firstKey, secondKey, updatedMinSum);
            double similarity = calculateSimilarity(updatedMinSum, firstSum, secondSum);

            EventSimilarityAvro eventSimilarityAvro = EventSimilarityAvro.newBuilder()
                    .setEventA(firstKey)
                    .setEventB(secondKey)
                    .setScore(similarity)
                    .setTimestamp(Instant.now())
                    .build();

            result.add(eventSimilarityAvro);
        }

        return result;
    }

    private BigDecimal getUserWeight(Long eventId, Long userId) {
        Map<Long, BigDecimal> userWeights = userActionOnEventMatrix.get(eventId);
        return userWeights != null ? userWeights.getOrDefault(userId, BigDecimal.ZERO) : BigDecimal.ZERO;
    }

    private BigDecimal getMinSum(Long firstKey, Long secondKey) {
        Map<Long, BigDecimal> internalMap = minWeightsSums.get(firstKey);
        return internalMap != null ? internalMap.getOrDefault(secondKey, BigDecimal.ZERO) : BigDecimal.ZERO;
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