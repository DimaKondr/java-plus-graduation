package ru.practicum.ewm.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    @InjectMocks
    private ErrorHandler errorHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void testHandleValidationExceptions() {
        FieldError fieldError1 = new FieldError("object", "app", "App cannot be empty");
        FieldError fieldError2 = new FieldError("object", "uri", "URI cannot be empty");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        Map<String, String> result = errorHandler.handleValidationExceptions(methodArgumentNotValidException);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("App cannot be empty", result.get("app"));
        assertEquals("URI cannot be empty", result.get("uri"));

        verify(methodArgumentNotValidException, times(1)).getBindingResult();
        verify(bindingResult, times(1)).getAllErrors();
    }

    @Test
    void testHandleJsonParseException() {
        Map<String, String> result = errorHandler.handleJsonParseException();

        assertNotNull(result);
        assertEquals("Неверный формат JSON", result.get("error"));
    }

    @Test
    void testHandleMissingParams() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("start", "String");

        Map<String, String> result = errorHandler.handleMissingParams(exception);

        assertNotNull(result);
        assertEquals("Отсутствует параметр: start", result.get("error"));
    }

    @Test
    void testHandleIllegalArgument() {
        IllegalArgumentException exception =
                new IllegalArgumentException("Дата начала не может быть позже даты окончания");

        Map<String, String> result = errorHandler.handleIllegalArgument(exception);

        assertNotNull(result);
        assertEquals("Дата начала не может быть позже даты окончания", result.get("error"));
    }
}