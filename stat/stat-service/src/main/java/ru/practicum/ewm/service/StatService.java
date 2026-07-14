package ru.practicum.ewm.service;

import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;

import java.util.List;

public interface StatService {

    /**
     * Сохранение информации о том, что на uri конкретного сервиса был отправлен запрос пользователем.
     * Название сервиса, uri и ip пользователя указаны в теле запроса.
     *
     * @param hitDto Содержит информацию о сервисе и его uri, ip пользователя и время запроса.
     * @return Возвращает информацию, поступившую в параметре, с id соответствующей записи из БД.
     */
    HitDto createHit(HitDto hitDto);

    /**
     * Получение статистики по посещениям.
     *
     * @param start  Дата и время начала диапазона
     *               за который нужно выгрузить статистику (в формате "yyyy-MM-dd HH:mm:ss").
     * @param end    Дата и время конца диапазона
     *               за который нужно выгрузить статистику (в формате "yyyy-MM-dd HH:mm:ss").
     * @param uris   Список uri для которых нужно выгрузить статистику.
     * @param unique Нужно ли учитывать только уникальные посещения (только с уникальным ip). Default value : false
     * @return Список данных о статистике посещений.
     */
    List<StatResponseDto> getStats(String start, String end, List<String> uris, Boolean unique);
}