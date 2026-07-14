package ru.practicum.ewm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.category.NewCategoryRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.event.Event;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private NewCategoryRequest newCategoryRequest;
    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        newCategoryRequest = new NewCategoryRequest();
        newCategoryRequest.setName("Концерты");

        category = new Category();
        category.setId(1L);
        category.setName("Концерты");
        category.setEvents(new ArrayList<>());

        categoryDto = new CategoryDto(1L, "Концерты");
    }

    @Test
    void createCategory() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.createCategory(newCategoryRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Концерты");

        verify(categoryRepository, times(1)).existsByName("Концерты");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategoryWithDuplicateName() {
        when(categoryRepository.existsByName("Концерты")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(newCategoryRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Категория с именем 'Концерты' уже существует");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory() {
        CategoryDto updateDto = new CategoryDto(1L, "Спектакли");
        Category updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("Спектакли");
        updatedCategory.setEvents(new ArrayList<>());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Спектакли")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        CategoryDto result = categoryService.updateCategory(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Спектакли");

        verify(categoryRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void updateCategoryWithNonExistentId() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, categoryDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Категория с id=999 не найдена");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategoryWithDuplicateName() {
        CategoryDto updateDto = new CategoryDto(1L, "Спектакли");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Спектакли")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Категория с именем 'Спектакли' уже существует");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategoryWithSameName() {
        CategoryDto updateDto = new CategoryDto(1L, "Концерты");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.updateCategory(1L, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Концерты");

        verify(categoryRepository, never()).existsByName(anyString());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void deleteCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteCategoryWithNonExistentId() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Категория с id=999 не найдена");

        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategoryWithEvents() {
        List<Event> events = List.of(new Event());
        category.setEvents(events);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Невозможно удалить категорию, так как с ней связаны события");

        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void getCategoriesShouldReturnListOfCategories() {
        int from = 0;
        int size = 10;
        List<Category> categories = List.of(category);
        Pageable expectedPageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(categories);

        when(categoryRepository.findAll(expectedPageable)).thenReturn(page);

        List<CategoryDto> result = categoryService.getCategories(from, size);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Концерты");

        verify(categoryRepository, times(1)).findAll(expectedPageable);
    }

    @Test
    void getCategoriesWithPagination() {
        // given
        int from = 20;  // 20/10 = страница 2
        int size = 10;
        Pageable expectedPageable = PageRequest.of(2, 10);

        when(categoryRepository.findAll(expectedPageable)).thenReturn(Page.empty());

        categoryService.getCategories(from, size);

        verify(categoryRepository, times(1)).findAll(expectedPageable);
    }

    @Test
    void getCategoriesWhenNoCategories() {
        int from = 0;
        int size = 10;
        Pageable expectedPageable = PageRequest.of(0, 10);

        when(categoryRepository.findAll(expectedPageable)).thenReturn(Page.empty());

        List<CategoryDto> result = categoryService.getCategories(from, size);

        assertThat(result).isEmpty();
    }

    @Test
    void getCategoriesWithZeroSize() {
        int from = 0;
        int size = 0;

        assertThatThrownBy(() -> categoryService.getCategories(from, size))
                .isInstanceOf(ArithmeticException.class);

        verify(categoryRepository, never()).findAll(any(Pageable.class));
    }
}