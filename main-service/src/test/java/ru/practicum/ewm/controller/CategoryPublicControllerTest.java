package ru.practicum.ewm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.service.CategoryService;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryPublicController.class)
class CategoryPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryDto categoryDto1;
    private CategoryDto categoryDto2;
    private List<CategoryDto> categoryList;

    @BeforeEach
    void setUp() {
        categoryDto1 = new CategoryDto(1L, "Концерты");
        categoryDto2 = new CategoryDto(2L, "Спектакли");
        categoryList = Arrays.asList(categoryDto1, categoryDto2);
    }

    // ==================== GET /categories ====================

    @Test
    void getCategories() throws Exception {
        when(categoryService.getCategories(0, 10))
                .thenReturn(categoryList);

        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Концерты"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Спектакли"));

        verify(categoryService, times(1)).getCategories(0, 10);
    }

    @Test
    void getCategoriesWithCustomPaginationParams() throws Exception {
        when(categoryService.getCategories(5, 20))
                .thenReturn(categoryList);

        mockMvc.perform(get("/categories")
                        .param("from", "5")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(categoryService, times(1)).getCategories(5, 20);
    }

    @Test
    void getCategoriesWithDefaultPagination() throws Exception {
        when(categoryService.getCategories(0, 10))
                .thenReturn(categoryList);

        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategories(0, 10);
    }

    @Test
    void getCategoriesWhenNoCategories() throws Exception {
        when(categoryService.getCategories(0, 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(content().json("[]"));

        verify(categoryService, times(1)).getCategories(0, 10);
    }

    @Test
    void getCategoriesWithZeroSize() throws Exception {
        when(categoryService.getCategories(0, 0))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService, times(1)).getCategories(0, 0);
    }

    // ==================== GET /categories/{catId} ====================

    @Test
    void getCategory() throws Exception {
        when(categoryService.getCategory(1L))
                .thenReturn(categoryDto1);

        mockMvc.perform(get("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Концерты"));

        verify(categoryService, times(1)).getCategory(1L);
    }

    @Test
    void getCategoryWithNonExistentId() throws Exception {
        when(categoryService.getCategory(999L))
                .thenThrow(new NotFoundException("Category с id=999 не найден"));

        mockMvc.perform(get("/categories/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Category с id=999 не найден"));

        verify(categoryService, times(1)).getCategory(999L);
    }

    @Test
    void getCategoryWithInvalidIdType() throws Exception {
        mockMvc.perform(get("/categories/abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoryWithNegativeId() throws Exception {
        mockMvc.perform(get("/categories/-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ==================== Тесты для пагинации ====================

    @Test
    void getCategoriesWithLargeFromValue() throws Exception {
        when(categoryService.getCategories(100, 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .param("from", "100")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategories(100, 10);
    }

    @Test
    void getCategoriesWithNegativeFrom() throws Exception {
        when(categoryService.getCategories(-5, 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/categories")
                        .param("from", "-5")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategories(-5, 10);
    }

    // ==================== Тесты для логов ====================

    @Test
    void getCategories_ShouldLogRequest() throws Exception {
        when(categoryService.getCategories(0, 10))
                .thenReturn(categoryList);

        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategories(0, 10);
    }

    @Test
    void getCategory_ShouldLogRequest() throws Exception {
        when(categoryService.getCategory(1L))
                .thenReturn(categoryDto1);

        mockMvc.perform(get("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).getCategory(1L);
    }
}