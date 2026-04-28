package com.fintrac.service;

import com.fintrac.dto.CategoryDTO;
import com.fintrac.exception.ResourceNotFoundException;
import com.fintrac.model.Category;
import com.fintrac.model.TransactionType;
import com.fintrac.model.User;
import com.fintrac.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        Long userId = authService.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserIdOrIsDefaultTrue(userId);
        return categories.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesByType(TransactionType type) {
        Long userId = authService.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserIdOrDefaultAndType(userId, type);
        return categories.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return toDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        User currentUser = authService.getCurrentUser();

        Category category = Category.builder()
                .name(categoryDTO.getName())
                .type(categoryDTO.getType())
                .icon(categoryDTO.getIcon())
                .color(categoryDTO.getColor())
                .user(currentUser)
                .isDefault(false)
                .build();

        Category saved = categoryRepository.save(category);
        return toDTO(saved);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (category.getIsDefault() != null && category.getIsDefault()) {
            throw new IllegalArgumentException("Cannot modify default category");
        }

        category.setName(categoryDTO.getName());
        category.setIcon(categoryDTO.getIcon());
        category.setColor(categoryDTO.getColor());

        Category updated = categoryRepository.save(category);
        return toDTO(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (category.getIsDefault() != null && category.getIsDefault()) {
            throw new IllegalArgumentException("Cannot delete default category");
        }

        categoryRepository.delete(category);
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .color(category.getColor())
                .isDefault(category.getIsDefault())
                .build();
    }
}
