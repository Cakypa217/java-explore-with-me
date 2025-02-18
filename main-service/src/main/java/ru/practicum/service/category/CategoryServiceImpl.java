package ru.practicum.service.category;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.dto.category.CategoryDto;
import ru.practicum.model.dto.category.NewCategoryDto;
import ru.practicum.model.entity.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        log.info("Получен запрос на получение списка категорий");

        PageRequest pageRequest = PageRequest.of(from / size, size);
        Page<Category> categories = categoryRepository.findAll(pageRequest);

        List<CategoryDto> categoryDto = categories.getContent()
                .stream()
                .map(categoryMapper::toCategoryDto)
                .toList();

        log.info("Найдено {} категорий", categoryDto.size());
        return categoryDto;
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info("Получен запрос на получение категории с id {}", catId);

        Category category = findById(catId);
        CategoryDto categoryDto = categoryMapper.toCategoryDto(category);

        log.info("Найдена категория {}", categoryDto);
        return categoryDto;
    }

    @Override
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Получен запрос на создание категории {}", newCategoryDto);
        try {
            Category category = categoryMapper.toCategory(newCategoryDto);
            Category savedCategory = categoryRepository.save(category);
            CategoryDto categoryDto = categoryMapper.toCategoryDto(savedCategory);
            log.info("Категория {} создана", categoryDto);
            return categoryDto;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с именем " + newCategoryDto.getName() + " уже существует");
        }
    }

    @Override
    public void deleteCategory(Long catId) {
        log.info("Получен запрос на удаление категории с id {}", catId);
        findById(catId);

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Нельзя удалить категорию с привязанными событиями");
        }

        categoryRepository.deleteById(catId);
        log.info("Категория с id {} удалена", catId);
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        log.info("Получен запрос на обновление категории с id {}", catId);
        try {
            Category category = findById(catId);
            category.setName(newCategoryDto.getName());
            Category updatedCategory = categoryRepository.save(category);
            CategoryDto categoryDto = categoryMapper.toCategoryDto(updatedCategory);
            log.info("Категория с id {} обновлена: {} → {}", catId, category.getName(), updatedCategory.getName());
            return categoryDto;
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Категория с именем " + newCategoryDto.getName() + " уже существует");
        }
    }

    @Override
    public Category findById(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new EntityNotFoundException("Категория с id " + catId + " не найдена"));
    }
}