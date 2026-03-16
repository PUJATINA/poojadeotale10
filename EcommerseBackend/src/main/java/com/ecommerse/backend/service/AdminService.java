package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.AdminCategoryCreateRequest;
import com.ecommerse.backend.dto.AdminCategoryRenameRequest;
import com.ecommerse.backend.dto.AdminOrderStatusUpdateRequest;
import com.ecommerse.backend.dto.AdminProductRequest;
import com.ecommerse.backend.dto.AdminStockUpdateRequest;
import com.ecommerse.backend.dto.AdminUserRoleUpdateRequest;
import com.ecommerse.backend.dto.AdminUserSummaryResponse;
import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.dto.OrderItemResponse;
import com.ecommerse.backend.dto.OrderResponse;
import com.ecommerse.backend.entity.Category;
import com.ecommerse.backend.entity.CustomerOrder;
import com.ecommerse.backend.entity.OrderItem;
import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.entity.UserRole;
import com.ecommerse.backend.repository.CategoryRepository;
import com.ecommerse.backend.repository.CustomerOrderRepository;
import com.ecommerse.backend.repository.OrderItemRepository;
import com.ecommerse.backend.repository.ProductRepository;
import com.ecommerse.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AdminService {
    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
            "PLACED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public AdminService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public Product createProduct(AdminProductRequest request) {
        Product product = new Product();
        applyProductData(product, request);
        Product saved = productRepository.save(product);
        ensureCategoryExists(saved.getCategory());
        return saved;
    }

    @Transactional
    public Product updateProduct(Long productId, AdminProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        applyProductData(product, request);
        Product saved = productRepository.save(product);
        ensureCategoryExists(saved.getCategory());
        return saved;
    }

    @Transactional
    public Product updateStock(Long productId, AdminStockUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setStock(request.stock());
        return productRepository.save(product);
    }

    @Transactional
    public MessageResponse deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        productRepository.delete(product);
        return new MessageResponse(true, "Product deleted");
    }

    public List<String> listCategories() {
        TreeSet<String> categories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Category category : categoryRepository.findAll()) {
            if (category.getName() != null && !category.getName().isBlank()) {
                categories.add(category.getName().trim());
            }
        }
        for (Product product : productRepository.findAll()) {
            if (product.getCategory() != null && !product.getCategory().isBlank()) {
                categories.add(product.getCategory().trim());
            }
        }
        return categories.stream().toList();
    }

    @Transactional
    public MessageResponse addCategory(AdminCategoryCreateRequest request) {
        String categoryName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(categoryName)) {
            return new MessageResponse(true, "Category already exists");
        }
        Category category = new Category();
        category.setName(categoryName);
        categoryRepository.save(category);
        return new MessageResponse(true, "Category added");
    }

    @Transactional
    public MessageResponse renameCategory(AdminCategoryRenameRequest request) {
        String source = request.currentCategory().trim();
        String target = request.newCategory().trim();
        if (source.equalsIgnoreCase(target)) {
            return new MessageResponse(true, "No changes applied");
        }

        int updated = 0;
        for (Product product : productRepository.findAll()) {
            if (product.getCategory() != null && product.getCategory().equalsIgnoreCase(source)) {
                product.setCategory(target);
                productRepository.save(product);
                updated++;
            }
        }

        Category sourceCategory = categoryRepository.findByNameIgnoreCase(source).orElse(null);
        Category targetCategory = categoryRepository.findByNameIgnoreCase(target).orElse(null);
        if (sourceCategory != null) {
            if (targetCategory != null) {
                categoryRepository.delete(sourceCategory);
            } else {
                sourceCategory.setName(target);
                categoryRepository.save(sourceCategory);
            }
        } else if (updated > 0 && targetCategory == null) {
            Category category = new Category();
            category.setName(target);
            categoryRepository.save(category);
        }

        return new MessageResponse(true, "Updated " + updated + " product(s)");
    }

    public List<OrderResponse> listOrders() {
        return customerOrderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(order -> new OrderResponse(
                        order.getId(),
                        order.getUserId(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCreatedAt(),
                        toItemResponses(order.getId()),
                        "Order fetched"
                ))
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, AdminOrderStatusUpdateRequest request) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ORDER_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }

        order.setStatus(normalizedStatus);
        CustomerOrder saved = customerOrderRepository.save(order);
        return new OrderResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getStatus(),
                saved.getTotalAmount(),
                saved.getCreatedAt(),
                toItemResponses(saved.getId()),
                "Order status updated"
        );
    }

    public List<AdminUserSummaryResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(this::toAdminUserSummary)
                .toList();
    }

    @Transactional
    public AdminUserSummaryResponse updateUserRole(Long userId, AdminUserRoleUpdateRequest request, Long actorUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserRole nextRole;
        try {
            nextRole = UserRole.valueOf(request.role().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        UserRole currentRole = user.getRole() == null ? UserRole.USER : user.getRole();
        if (currentRole == UserRole.ADMIN && nextRole == UserRole.USER) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one admin is required");
            }
            if (actorUserId != null && actorUserId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot demote your own admin account");
            }
        }

        user.setRole(nextRole);
        User saved = userRepository.save(user);
        return toAdminUserSummary(saved);
    }

    private void applyProductData(Product product, AdminProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setPrice(request.price());
        product.setImageUrl(request.imageUrl().trim());
        product.setStock(request.stock());
        product.setCategory(request.category() == null ? null : request.category().trim());
        product.setBrand(request.brand() == null ? null : request.brand().trim());
    }

    private List<OrderItemResponse> toItemResponses(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        return items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductName(),
                        item.getImageUrl(),
                        item.getProductDescription(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .toList();
    }

    private AdminUserSummaryResponse toAdminUserSummary(User user) {
        UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                role.name()
        );
    }

    private void ensureCategoryExists(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return;
        }
        String normalized = categoryName.trim();
        if (categoryRepository.existsByNameIgnoreCase(normalized)) {
            return;
        }
        Category category = new Category();
        category.setName(normalized);
        categoryRepository.save(category);
    }
}
