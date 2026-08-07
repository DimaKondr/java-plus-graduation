package ru.practicum.ewm;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.processor.EventSimilarityProcessor;
import ru.practicum.ewm.processor.UserActionProcessor;

@Component
@AllArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    UserActionProcessor userActionProcessor;
    EventSimilarityProcessor eventSimilarityProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        Thread eventSimilarityThread = new Thread(eventSimilarityProcessor);
        eventSimilarityThread.setName("UserActionHandlerThread");
        eventSimilarityThread.start();
    }

}