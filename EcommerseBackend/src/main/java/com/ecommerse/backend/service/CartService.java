package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.*;
import com.ecommerse.backend.entity.CartItem;
import com.ecommerse.backend.entity.CustomerOrder;
import com.ecommerse.backend.entity.OrderItem;
import com.ecommerse.backend.entity.Product;
import com.ecommerse.backend.repository.CartItemRepository;
import com.ecommerse.backend.repository.CustomerOrderRepository;
import com.ecommerse.backend.repository.OrderItemRepository;
import com.ecommerse.backend.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {
    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public CartService(CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       CustomerOrderRepository customerOrderRepository,
                       OrderItemRepository orderItemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        log.info("Cart fetched for userId={} itemCount={}", userId, items.size());
        return buildCartResponse(userId, items);
    }

    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        log.info("Add to cart requested userId={} productId={} quantity={}",
                request.userId(), request.productId(), request.quantity());
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("Add to cart failed: product not found userId={} productId={}",
                            request.userId(), request.productId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                });

        if (request.quantity() > product.getStock()) {
            log.warn("Add to cart failed: quantity exceeds stock userId={} productId={} requested={} stock={}",
                    request.userId(), request.productId(), request.quantity(), product.getStock());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity exceeds stock");
        }

        CartItem item = cartItemRepository.findByUserIdAndProduct_Id(request.userId(), request.productId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUserId(request.userId());
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int updatedQuantity = item.getQuantity() + request.quantity();
        if (updatedQuantity > product.getStock()) {
            log.warn("Add to cart failed: total quantity exceeds stock userId={} productId={} updated={} stock={}",
                    request.userId(), request.productId(), updatedQuantity, product.getStock());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total cart quantity exceeds stock");
        }

        item.setQuantity(updatedQuantity);
        cartItemRepository.save(item);
        log.info("Cart item updated userId={} productId={} quantity={}",
                request.userId(), request.productId(), updatedQuantity);

        return getCart(request.userId());
    }

    @Transactional
    public CartResponse removeFromCart(Long userId, Long productId) {
        log.info("Remove from cart requested userId={} productId={}", userId, productId);
        CartItem item = cartItemRepository.findByUserIdAndProduct_Id(userId, productId)
                .orElseThrow(() -> {
                    log.warn("Remove from cart failed: cart item not found userId={} productId={}", userId, productId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
                });

        cartItemRepository.delete(item);
        log.info("Cart item removed userId={} productId={}", userId, productId);
        return getCart(userId);
    }

    @Transactional
    public CartResponse updateCartItemQuantity(UpdateCartItemRequest request) {
        log.info("Update cart quantity requested userId={} productId={} quantity={}",
                request.userId(), request.productId(), request.quantity());
        CartItem item = cartItemRepository.findByUserIdAndProduct_Id(request.userId(), request.productId())
                .orElseThrow(() -> {
                    log.warn("Update cart failed: cart item not found userId={} productId={}",
                            request.userId(), request.productId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
                });

        Product product = item.getProduct();
        if (request.quantity() > product.getStock()) {
            log.warn("Update cart failed: quantity exceeds stock userId={} productId={} requested={} stock={}",
                    request.userId(), request.productId(), request.quantity(), product.getStock());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity exceeds stock");
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        log.info("Cart quantity updated userId={} productId={} quantity={}",
                request.userId(), request.productId(), request.quantity());
        return getCart(request.userId());
    }

    @Transactional
    public OrderResponse buyCart(BuyRequest request) {
        log.info("Buy cart requested userId={}", request.userId());
        List<CartItem> cartItems = cartItemRepository.findByUserId(request.userId());

        if (cartItems.isEmpty()) {
            log.warn("Buy cart failed: cart is empty userId={}", request.userId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStock()) {
                log.warn("Buy cart failed: insufficient stock userId={} productName={} requested={} stock={}",
                        request.userId(), product.getName(), cartItem.getQuantity(), product.getStock());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for " + product.getName());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        CustomerOrder order = new CustomerOrder();
        order.setUserId(request.userId());
        order.setTotalAmount(total);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PLACED");
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        List<OrderItemResponse> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductName(product.getName());
            orderItem.setImageUrl(product.getImageUrl());
            orderItem.setProductDescription(product.getDescription());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItemRepository.save(orderItem);

            orderItems.add(new OrderItemResponse(
                    orderItem.getProductName(),
                    orderItem.getImageUrl(),
                    orderItem.getProductDescription(),
                    orderItem.getPrice(),
                    orderItem.getQuantity()
            ));
        }

        cartItemRepository.deleteByUserId(request.userId());
        log.info("Order placed successfully orderId={} userId={} itemCount={} total={}",
                savedOrder.getId(), request.userId(), orderItems.size(), savedOrder.getTotalAmount());

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getStatus(),
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt(),
                orderItems,
                "Order placed successfully"
        );
    }

    public List<OrderResponse> getOrders(Long userId) {
        log.info("Order history requested for userId={}", userId);
        List<CustomerOrder> orders = customerOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<OrderResponse> responses = orders.stream().map(order -> {
            List<OrderItemResponse> items = orderItemRepository.findByOrder_Id(order.getId())
                    .stream()
                    .map(item -> new OrderItemResponse(
                            item.getProductName(),
                            item.getImageUrl(),
                            item.getProductDescription(),
                            item.getPrice(),
                            item.getQuantity()
                    ))
                    .toList();

            return new OrderResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    items,
                    "Order fetched"
            );
        }).toList();
        log.info("Order history fetched for userId={} orderCount={}", userId, responses.size());
        return responses;
    }

    private CartResponse buildCartResponse(Long userId, List<CartItem> items) {
        List<CartItemResponse> cartItemResponses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product product = item.getProduct();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);

            cartItemResponses.add(new CartItemResponse(
                    product.getId(),
                    product.getName(),
                    product.getImageUrl(),
                    product.getPrice(),
                    item.getQuantity(),
                    lineTotal
            ));
        }

        return new CartResponse(userId, cartItemResponses, total);
    }
}
