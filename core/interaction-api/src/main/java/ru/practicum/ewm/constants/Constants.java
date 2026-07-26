package ru.practicum.ewm.constants;

import java.time.format.DateTimeFormatter;

public final class Constants {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Constants() {
    }
}