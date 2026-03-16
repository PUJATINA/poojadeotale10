package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.AdminCategoryCreateRequest;
import com.ecommerse.backend.dto.AdminCategoryRenameRequest;
import com.ecommerse.backend.dto.AdminOrderStatusUpdateRequest;
import com.ecommerse.backend.dto.AdminProductRequest;
import com.ecommerse.backend.dto.AdminStockUpdateRequest;
import com.ecommerse.backend.dto.AdminUserRoleUpdateRequest;
import com.ecommerse.backend.dto.AdminUserSummaryResponse;
import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.dto.OrderResponse;
import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/products")
    public List<Product> listProducts() {
        return adminService.listProducts();
    }

    @PostMapping("/products")
    public Product createProduct(
            @Valid @RequestBody AdminProductRequest request
    ) {
        return adminService.createProduct(request);
    }

    @PutMapping("/products/{productId}")
    public Product updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody AdminProductRequest request
    ) {
        return adminService.updateProduct(productId, request);
    }

    @PatchMapping("/products/{productId}/stock")
    public Product updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody AdminStockUpdateRequest request
    ) {
        return adminService.updateStock(productId, request);
    }

    @DeleteMapping("/products/{productId}")
    public MessageResponse deleteProduct(@PathVariable Long productId) {
        return adminService.deleteProduct(productId);
    }

    @GetMapping("/categories")
    public List<String> listCategories() {
        return adminService.listCategories();
    }

    @PostMapping("/categories")
    public MessageResponse addCategory(
            @Valid @RequestBody AdminCategoryCreateRequest request
    ) {
        return adminService.addCategory(request);
    }

    @PostMapping("/categories/rename")
    public MessageResponse renameCategory(
            @Valid @RequestBody AdminCategoryRenameRequest request
    ) {
        return adminService.renameCategory(request);
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders() {
        return adminService.listOrders();
    }

    @PatchMapping("/orders/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminOrderStatusUpdateRequest request
    ) {
        return adminService.updateOrderStatus(orderId, request);
    }

    @GetMapping("/users")
    public List<AdminUserSummaryResponse> listUsers() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{userId}/role")
    public AdminUserSummaryResponse updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return adminService.updateUserRole(userId, request, principal == null ? null : principal.getId());
    }
}
