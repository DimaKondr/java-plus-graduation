package ru.practicum.ewm;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Service
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub userActionControl;

    public void collectUserAction(Long userId, Long eventId, ActionTypeProto action) {
        UserActionProto userAction = UserActionProto.newBuilder()
                .setUserId(Math.toIntExact(userId))
                .setEventId(Math.toIntExact(eventId))
                .setActionType(action)
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();
        userActionControl.collectUserAction(userAction);
    }

}