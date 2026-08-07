package ru.practicum.ewm.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.service.CollectorService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class CollectorController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final CollectorService collectorService;

    @Override
    public void collectUserAction(UserActionProto userAction, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Поступили данные о действии пользователя c ID: {} с событием с ID: {}. Тип действия: {}.",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

            collectorService.collectUserAction(userAction);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка обработки данных о действии пользователя c ID: {} с событием с ID: {}. Тип действия: {}.",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType(), e);

            responseObserver.onError(Status.INTERNAL
                    .withDescription("Ошибка обработки данных о действии пользователя c событием.")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

}