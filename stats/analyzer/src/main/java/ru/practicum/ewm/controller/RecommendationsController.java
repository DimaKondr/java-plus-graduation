package ru.practicum.ewm.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Поступил запрос на рекомендованные мероприятия для пользователя с ID: {}", request.getUserId());

        try {
            List<RecommendedEventProto> recommendedEvent = recommendationService.getRecommendationsForUser(
                    request.getUserId(),
                    request.getMaxResults()
            );
            log.info("Получены данные о рекомендованных мероприятиях: {}.", recommendedEvent);

            sendResponse(recommendedEvent, responseObserver);
        } catch (Exception e) {
            log.error("Ошибка обработки запроса на рекомендованные мероприятия для пользователя с ID: {}.",
                    request.getUserId(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка обработки запроса на данные о похожих мероприятиях.")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Поступил запрос на данные о мероприятиях, похожих на мероприятие с ID: {}, для пользователя с ID: {}",
                request.getEventId(), request.getUserId());

        try {
            List<RecommendedEventProto> similarEvents = recommendationService.getSimilarEvents(
                    request.getEventId(),
                    request.getUserId(),
                    request.getMaxResults()
            );
            log.info("Получены данные о похожих мероприятиях: {}.", similarEvents);

            sendResponse(similarEvents, responseObserver);
        } catch (Exception e) {
            log.error("Ошибка обработки запроса на данные о мероприятиях, похожих на мероприятие с ID: {}, " +
                            "для пользователя с ID: {}.", request.getEventId(), request.getUserId(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка обработки запроса на данные о похожих мероприятиях.")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Поступил запрос на данные о количестве взаимодействий по списку мероприятий: {}.",
                request.getEventIdList());

        try {
            List<RecommendedEventProto> eventsScores =
                    recommendationService.getInteractionsCount(request.getEventIdList());
            log.info("Получены данные о количестве взаимодействий по списку мероприятий: {}.", eventsScores);

            sendResponse(eventsScores, responseObserver);
        } catch (Exception e) {
            log.error("Ошибка обработки запроса на данные о количестве взаимодействий по списку мероприятий: {}.",
                    request.getEventIdList(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка обработки запроса на данные о взаимодействия с мероприятиями.")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private void sendResponse(List<RecommendedEventProto> eventsScores,
                              StreamObserver<RecommendedEventProto> responseObserver) {
        eventsScores.forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

}