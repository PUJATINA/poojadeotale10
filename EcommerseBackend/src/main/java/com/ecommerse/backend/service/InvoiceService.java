package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.MessageResponse;
import com.ecommerse.backend.entity.CustomerOrder;
import com.ecommerse.backend.entity.OrderItem;
import com.ecommerse.backend.entity.User;
import com.ecommerse.backend.repository.CustomerOrderRepository;
import com.ecommerse.backend.repository.OrderItemRepository;
import com.ecommerse.backend.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter INVOICE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.auth.mail.from:no-reply@shopsphere.local}")
    private String fromEmail;

    @Value("${app.invoice.mail.enabled:${app.auth.mail.enabled:false}}")
    private boolean invoiceMailEnabled;

    public InvoiceService(CustomerOrderRepository customerOrderRepository,
                          OrderItemRepository orderItemRepository,
                          UserRepository userRepository,
                          JavaMailSender mailSender) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public byte[] generateInvoicePdf(Long orderId, Long userId) {
        CustomerOrder order = validateOrderOwnership(orderId, userId);
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String invoiceNumber = generateInvoiceNumber(order);
        TaxBreakdown taxBreakdown = calculateTaxBreakdown(order.getTotalAmount());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50f;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 16f;

                y = writeLine(content, "ShopSphere Invoice", margin, y, 16, true);
                y -= 6;
                y = writeLine(content, "Invoice No: " + invoiceNumber, margin, y, 11, false);
                y = writeLine(content, "Order ID: " + order.getId(), margin, y, 11, false);
                y = writeLine(content, "Order Date: " + DATE_FORMATTER.format(order.getCreatedAt()), margin, y, 11, false);
                y = writeLine(content, "Customer: " + user.getName(), margin, y, 11, false);
                y = writeLine(content, "Email: " + user.getEmail(), margin, y, 11, false);
                y -= 12;

                y = writeLine(content, "Items", margin, y, 13, true);
                y -= 4;

                int idx = 1;
                BigDecimal recalculatedTotal = BigDecimal.ZERO;
                for (OrderItem item : items) {
                    BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    recalculatedTotal = recalculatedTotal.add(lineTotal);

                    y = writeLine(content, idx + ". " + item.getProductName(), margin, y, 11, true);
                    y = writeLine(content, "   Qty: " + item.getQuantity() + "   Price: Rs. " + item.getPrice() + "   Line Total: Rs. " + lineTotal, margin, y, 10, false);
                    if (item.getProductDescription() != null && !item.getProductDescription().isBlank()) {
                        y = writeLine(content, "   " + truncate(item.getProductDescription(), 120), margin, y, 10, false);
                    }
                    y -= 3;
                    idx++;
                }

                y -= 8;
                y = writeLine(content, "Taxable Amount: Rs. " + taxBreakdown.taxableAmount(), margin, y, 11, false);
                y = writeLine(content, "GST @18%: Rs. " + taxBreakdown.gstAmount(), margin, y, 11, false);
                y = writeLine(content, "Grand Total: Rs. " + order.getTotalAmount(), margin, y, 13, true);
                y -= leading;
                if (order.getTotalAmount().compareTo(recalculatedTotal) != 0) {
                    writeLine(content, "Note: Order total calculated at purchase time.", margin, y, 10, false);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            log.info("Invoice PDF generated orderId={} userId={} itemCount={}", orderId, userId, items.size());
            return out.toByteArray();
        } catch (IOException exception) {
            log.error("Invoice PDF generation failed orderId={} userId={}", orderId, userId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate invoice");
        }
    }

    public MessageResponse emailInvoice(Long orderId, Long userId) {
        CustomerOrder order = validateOrderOwnership(orderId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String invoiceNumber = generateInvoiceNumber(order);
        TaxBreakdown taxBreakdown = calculateTaxBreakdown(order.getTotalAmount());

        if (!invoiceMailEnabled) {
            log.warn("Invoice email skipped because mail is disabled orderId={} userId={}", orderId, userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice email is disabled on server");
        }

        byte[] invoicePdf = generateInvoicePdf(orderId, userId);
        String fileName = "invoice-order-" + orderId + ".pdf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Your ShopSphere Invoice - Order #" + orderId);
            helper.setText(
                    "Hello " + user.getName() + ",\n\n" +
                            "Please find attached the invoice for your order #" + orderId + ".\n" +
                            "Invoice Number: " + invoiceNumber + "\n" +
                            "Taxable Amount: Rs. " + taxBreakdown.taxableAmount() + "\n" +
                            "GST @18%: Rs. " + taxBreakdown.gstAmount() + "\n" +
                            "Order Total: Rs. " + order.getTotalAmount() + "\n\n" +
                            "Thank you for shopping with ShopSphere."
            );
            helper.addAttachment(fileName, new ByteArrayDataSource(invoicePdf, "application/pdf"));
            mailSender.send(message);
            log.info("Invoice email sent orderId={} userId={} email={}", orderId, userId, user.getEmail());
            return new MessageResponse(true, "Invoice sent successfully to " + user.getEmail());
        } catch (Exception exception) {
            log.error("Invoice email send failed orderId={} userId={}", orderId, userId, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send invoice email");
        }
    }

    private CustomerOrder validateOrderOwnership(Long orderId, Long userId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Invoice request failed: order not found orderId={} userId={}", orderId, userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });

        if (!order.getUserId().equals(userId)) {
            log.warn("Invoice request forbidden: order does not belong to user orderId={} userId={}", orderId, userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this invoice");
        }
        return order;
    }

    private float writeLine(PDPageContentStream content, String text, float x, float y, int fontSize, boolean bold) throws IOException {
        content.beginText();
        content.setFont(
                bold
                        ? new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                        : new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA),
                fontSize
        );
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - (fontSize + 4);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String generateInvoiceNumber(CustomerOrder order) {
        LocalDate invoiceDate = order.getCreatedAt().toLocalDate();
        return "INV-" + INVOICE_DATE_FORMATTER.format(invoiceDate) + "-" + order.getId();
    }

    private TaxBreakdown calculateTaxBreakdown(BigDecimal grossAmount) {
        BigDecimal divisor = BigDecimal.ONE.add(GST_RATE);
        BigDecimal taxableAmount = grossAmount.divide(divisor, 2, RoundingMode.HALF_UP);
        BigDecimal gstAmount = grossAmount.subtract(taxableAmount).setScale(2, RoundingMode.HALF_UP);
        return new TaxBreakdown(taxableAmount, gstAmount);
    }

    private record TaxBreakdown(BigDecimal taxableAmount, BigDecimal gstAmount) {
    }
}
