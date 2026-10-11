package com.canteen.app.service;

import com.canteen.app.model.Category;
import com.canteen.app.model.MenuItem;
import com.canteen.app.repository.CategoryRepository;
import com.canteen.app.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public MenuService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<MenuItem> getAllAvailableItems() {
        return menuItemRepository.findByIsAvailableTrue();
    }

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getAvailableItemsByCategory(Long categoryId) {
        return menuItemRepository.findByCategoryIdAndIsAvailableTrue(categoryId);
    }

    public List<MenuItem> getItemsByCategory(Long categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    public List<MenuItem> searchItems(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllItems();
        }
        return menuItemRepository.searchByKeyword(keyword.trim());
    }

    @Transactional
    public MenuItem saveMenuItem(MenuItem item) {
        return menuItemRepository.save(item);
    }

    @Transactional
    public boolean toggleAvailability(Long itemId) {
        Optional<MenuItem> itemOpt = menuItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            MenuItem item = itemOpt.get();
            item.setIsAvailable(!Boolean.TRUE.equals(item.getIsAvailable()));
            menuItemRepository.save(item);
            return item.getIsAvailable();
        }
        return false;
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    @Transactional
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }
}
