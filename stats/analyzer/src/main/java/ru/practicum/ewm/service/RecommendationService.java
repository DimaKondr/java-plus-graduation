package ru.practicum.ewm.service;

import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;

public interface RecommendationService {

    List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults);

    List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults);

    List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}