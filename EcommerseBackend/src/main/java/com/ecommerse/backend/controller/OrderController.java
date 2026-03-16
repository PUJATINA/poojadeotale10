package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.dto.OrderResponse;
import com.ecommerse.backend.security.AppUserPrincipal;
import com.ecommerse.backend.service.CartService;
import com.ecommerse.backend.service.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final CartService cartService;
    private final InvoiceService invoiceService;

    public OrderController(CartService cartService, InvoiceService invoiceService) {
        this.cartService = cartService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{userId}")
    public List<OrderResponse> getOrders(@PathVariable Long userId, @AuthenticationPrincipal AppUserPrincipal principal) {
        validateUserAccess(userId, principal);
        log.info("API hit: get orders userId={}", userId);
        return cartService.getOrders(userId);
    }

    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId,
            @RequestParam Long userId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        log.info("API hit: download invoice orderId={} userId={}", orderId, userId);
        byte[] pdfBytes = invoiceService.generateInvoicePdf(orderId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-order-" + orderId + ".pdf")
                .body(pdfBytes);
    }

    @PostMapping("/{orderId}/invoice/email")
    public MessageResponse emailInvoice(
            @PathVariable Long orderId,
            @RequestParam Long userId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        validateUserAccess(userId, principal);
        log.info("API hit: email invoice orderId={} userId={}", orderId, userId);
        return invoiceService.emailInvoice(orderId, userId);
    }

    private void validateUserAccess(Long requestedUserId, AppUserPrincipal principal) {
        if (!principal.getId().equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for requested user");
        }
    }
}
