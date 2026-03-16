package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.AddToCartRequest;
import com.ecommerse.backend.dto.BuyRequest;
import com.ecommerse.backend.dto.CartResponse;
import com.ecommerse.backend.dto.OrderResponse;
import com.ecommerse.backend.dto.UpdateCartItemRequest;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.service.CartService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public CartResponse getCart(@PathVariable Long userId, @AuthenticationPrincipal AppUserPrincipal principal) {
        validateUserAccess(userId, principal);
        log.info("API hit: get cart userId={}", userId);
        return cartService.getCart(userId);
    }

    @PostMapping("/add")
    public CartResponse addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(request.userId(), principal);
        log.info("API hit: add to cart userId={} productId={}", principal.getId(), request.productId());
        AddToCartRequest securedRequest = new AddToCartRequest(principal.getId(), request.productId(), request.quantity());
        return cartService.addToCart(securedRequest);
    }

    @DeleteMapping("/remove")
    public CartResponse removeFromCart(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        log.info("API hit: remove from cart userId={} productId={}", userId, productId);
        return cartService.removeFromCart(userId, productId);
    }

    @PutMapping("/update")
    public CartResponse updateCartItemQuantity(
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(request.userId(), principal);
        log.info("API hit: update cart item userId={} productId={}", principal.getId(), request.productId());
        UpdateCartItemRequest securedRequest = new UpdateCartItemRequest(principal.getId(), request.productId(), request.quantity());
        return cartService.updateCartItemQuantity(securedRequest);
    }

    @PostMapping("/buy")
    public OrderResponse buyCart(
            @Valid @RequestBody BuyRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(request.userId(), principal);
        log.info("API hit: buy cart userId={}", principal.getId());
        return cartService.buyCart(new BuyRequest(principal.getId()));
    }

    private void validateUserAccess(Long requestedUserId, AppUserPrincipal principal) {
        if (!principal.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for requested user");
        }
    }
}
