package com.badminton.service.impl;

import com.badminton.dto.request.AddToCartRequest; // ✅ THÊM
import com.badminton.dto.request.ChatRequest;
import com.badminton.dto.request.CreateOrderRequest; // ✅ THÊM (thay OrderRequest)
import com.badminton.dto.response.*;
import com.badminton.entity.*;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.*;
import com.badminton.service.*;
import com.fasterxml.jackson.core.type.TypeReference; // ✅ THÊM
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final LocationService locationService;
    private final ProductService productService;
    private final UserTierService userTierService;
    private final CourtService courtService;
    private final CartService cartService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openaiModel;

    @Override
    public ChatResponse processMessage(Long userId, ChatRequest request) {
        log.info("💬 Processing chat message from user {}: {}", userId, request.getMessage());

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

            // Get or create session
            ChatSession session = getOrCreateSession(user, request.getSessionId());

            // Update location if provided
            if (request.getLatitude() != null && request.getLongitude() != null) {
                updateLocationFromRequest(user, request);
            }

            // ✅ KIỂM TRA XEM CÓ PHẢI ACTION KHÔNG
            if (request.getMessage().startsWith("ACTION:")) {
                return handleAction(user, request, session);
            }

            // Analyze intent
            MessageIntent intent = analyzeIntent(request.getMessage(), user);

            // Process based on intent
            ChatResponse response = switch (intent.getType()) {
                case COURT_SEARCH -> handleCourtSearch(user, intent, session);
                case COURT_BOOKING -> handleCourtBooking(user, intent, session);
                case PRODUCT_SEARCH -> handleProductSearch(user, intent, session);
                case PRODUCT_ORDER -> handleProductOrder(user, intent, session);
                case TIER_INFO -> handleTierInfo(user, session);
                case VIEW_CART -> handleViewCart(user, session); // ✅ THÊM
                case GENERAL -> handleGeneralChat(user, request.getMessage(), session);
                default -> buildErrorResponse("Xin lỗi, tôi chưa hiểu yêu cầu của bạn. Bạn có thể nói rõ hơn không?");
            };

            // Save message
            saveChatMessage(user, request.getMessage(), response.getAiResponse(), intent.getType());

            response.setSessionId(session.getSessionId());
            response.setTimestamp(LocalDateTime.now());

            return response;

        } catch (Exception e) {
            log.error("❌ Unexpected error processing chat message", e);
            return buildErrorResponse("Đã có lỗi xảy ra. Vui lòng thử lại sau.");
        }
    }

    // ✅ THÊM: Xử lý actions từ quick buttons
    private ChatResponse handleAction(User user, ChatRequest request, ChatSession session) {
        try {
            String actionData = request.getMessage().substring(7); // Remove "ACTION:"
            Map<String, Object> params = objectMapper.readValue(actionData,
                    new TypeReference<Map<String, Object>>() {
                    });

            String action = (String) params.get("action");

            return switch (action) {
                case "ADD_TO_CART" -> handleAddToCart(user, params, session);
                case "BUY_NOW" -> handleBuyNow(user, params, session);
                case "VIEW_CART" -> handleViewCart(user, session);
                case "REMOVE_FROM_CART" -> handleRemoveFromCart(user, params, session);
                case "CHECKOUT" -> handleCheckout(user, params, session);
                case "VIEW_PRODUCT_DETAIL" -> handleViewProductDetail(user, params, session);
                case "BOOK_COURT" -> handleBookCourtAction(user, params, session);
                case "VIEW_ORDER" -> handleViewOrder(user, params, session);
                case "VIEW_ORDERS" -> handleViewOrders(user, session);
                default -> buildErrorResponse("Action không hợp lệ");
            };
        } catch (Exception e) {
            log.error("❌ Error handling action", e);
            return buildErrorResponse("Không thể xử lý yêu cầu. Vui lòng thử lại.");
        }
    }

    private ChatResponse handleAddToCart(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long productId = Long.valueOf(params.get("productId").toString());
            Integer quantity = params.containsKey("quantity")
                    ? Integer.valueOf(params.get("quantity").toString())
                    : 1;

            // ✅ SỬA: Tạo AddToCartRequest
            AddToCartRequest cartRequest = new AddToCartRequest();
            cartRequest.setProductId(productId);
            cartRequest.setQuantity(quantity);

            // Add to cart
            cartService.addToCart(user.getId(), cartRequest);

            // Get product info
            ProductDetailResponse product = productService.getProductById(productId);

            String aiResponse = String.format(
                    "✅ Đã thêm %dx %s vào giỏ hàng!\n\n" +
                            "Giá: %,dđ\n" +
                            "Tổng: %,dđ",
                    quantity,
                    product.getName(),
                    product.getPrice().longValue(),
                    product.getPrice().longValue() * quantity);

            List<ChatResponse.QuickAction> quickActions = List.of(
                    ChatResponse.QuickAction.builder()
                            .label("🛒 Xem giỏ hàng")
                            .action("VIEW_CART")
                            .params(new HashMap<>())
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("💳 Thanh toán ngay")
                            .action("CHECKOUT")
                            .params(new HashMap<>())
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("🛍️ Tiếp tục mua sắm")
                            .action("SEARCH_PRODUCTS")
                            .params(new HashMap<>())
                            .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.TEXT)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error adding to cart", e);
            return buildErrorResponse("Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.");
        }
    }

    // ✅ SỬA: handleBuyNow - Dùng AddToCartRequest
    private ChatResponse handleBuyNow(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long productId = Long.valueOf(params.get("productId").toString());
            Integer quantity = params.containsKey("quantity")
                    ? Integer.valueOf(params.get("quantity").toString())
                    : 1;

            // ✅ SỬA: Tạo AddToCartRequest
            AddToCartRequest cartRequest = new AddToCartRequest();
            cartRequest.setProductId(productId);
            cartRequest.setQuantity(quantity);

            // Get product info
            ProductDetailResponse product = productService.getProductById(productId);

            String aiResponse = String.format(
                    "🛍️ Mua ngay: %s\n\n" +
                            "Số lượng: %d\n" +
                            "Đơn giá: %,dđ\n" +
                            "Tổng tiền: %,dđ\n\n" +
                            "Bạn muốn thanh toán như thế nào?",
                    product.getName(),
                    quantity,
                    product.getPrice().longValue(),
                    product.getPrice().longValue() * quantity);

            List<ChatResponse.QuickAction> quickActions = List.of(
                    ChatResponse.QuickAction.builder()
                            .label("💳 Thanh toán online")
                            .action("CHECKOUT")
                            .params(Map.of(
                                    "paymentMethod", "ONLINE",
                                    "productId", productId,
                                    "quantity", quantity))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("💵 Thanh toán COD")
                            .action("CHECKOUT")
                            .params(Map.of(
                                    "paymentMethod", "COD",
                                    "productId", productId,
                                    "quantity", quantity))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("🛒 Thêm vào giỏ hàng")
                            .action("ADD_TO_CART")
                            .params(Map.of("productId", productId, "quantity", quantity))
                            .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.ORDER_CONFIRM)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error in buy now", e);
            return buildErrorResponse("Không thể xử lý mua hàng. Vui lòng thử lại.");
        }
    }

    private ChatResponse handleRemoveFromCart(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long cartItemId = Long.valueOf(params.get("cartItemId").toString());

            // ✅ SỬA: removeItem() → removeCartItem()
            cartService.removeCartItem(user.getId(), cartItemId);

            return ChatResponse.builder()
                    .aiResponse("✅ Đã xóa sản phẩm khỏi giỏ hàng!")
                    .messageType(ChatResponse.MessageType.TEXT)
                    .quickActions(List.of(
                            ChatResponse.QuickAction.builder()
                                    .label("🛒 Xem giỏ hàng")
                                    .action("VIEW_CART")
                                    .params(new HashMap<>())
                                    .build(),
                            ChatResponse.QuickAction.builder()
                                    .label("🛍️ Tiếp tục mua")
                                    .action("SEARCH_PRODUCTS")
                                    .params(new HashMap<>())
                                    .build()))
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error removing from cart", e);
            return buildErrorResponse("Không thể xóa sản phẩm. Vui lòng thử lại.");
        }
    }

    // ✅ SỬA: handleCheckout - Dùng CreateOrderRequest và user.getPhone()
    private ChatResponse handleCheckout(User user, Map<String, Object> params, ChatSession session) {
        try {
            String paymentMethod = params.containsKey("paymentMethod")
                    ? (String) params.get("paymentMethod")
                    : "BANK_TRANSFER"; // Default changed from ONLINE to BANK_TRANSFER

            log.info("🔵 Checkout started - paymentMethod: {}", paymentMethod);
            log.info("🔵 Params: {}", params);

            // ✅ SỬA: Lưu giá trị original để hiển thị message đúng
            String originalPaymentMethod = params.containsKey("paymentMethod")
                    ? (String) params.get("paymentMethod")
                    : "BANK_TRANSFER";

            // ✅ SỬA: Map "ONLINE" to "BANK_TRANSFER"
            if ("ONLINE".equals(paymentMethod)) {
                paymentMethod = "BANK_TRANSFER";
                log.info("🔵 Mapped ONLINE → BANK_TRANSFER");
            }

            // ✅ SỬA: Xác định xem đây là "Mua ngay" hay "Thanh toán từ giỏ hàng"
            boolean isBuyNow = params.containsKey("productId");

            // ✅ CHECK: User info
            log.info("🔵 User info - Name: {}, Phone: {}, Address: {}",
                    user.getFullName(), user.getPhone(), user.getAddress());

            if (user.getPhone() == null || user.getPhone().isEmpty()) {
                return buildErrorResponse("Vui lòng cập nhật số điện thoại trong hồ sơ trước khi đặt hàng.");
            }

            if (user.getAddress() == null || user.getAddress().isEmpty()) {
                return buildErrorResponse("Vui lòng cập nhật địa chỉ trong hồ sơ trước khi đặt hàng.");
            }

            // ✅ SỬA: Tạo CreateOrderRequest
            CreateOrderRequest orderRequest = new CreateOrderRequest();
            orderRequest.setRecipientName(user.getFullName());
            orderRequest.setRecipientPhone(user.getPhone());
            orderRequest.setShippingAddress(user.getAddress());
            orderRequest.setShippingProvince(user.getProvince());
            orderRequest.setShippingDistrict(user.getDistrict());
            orderRequest.setShippingWard(user.getWard());
            orderRequest.setNote("Đặt hàng từ chat");
            orderRequest.setPaymentMethod(CreateOrderRequest.PaymentMethod.valueOf(paymentMethod));

            if (isBuyNow) {
                // ✅ FLOW "MUA NGAY": Tạo order trực tiếp từ productId, KHÔNG thêm vào cart
                log.info("🔵 Buy Now flow - creating order directly from productId");
                Long productId = Long.valueOf(params.get("productId").toString());
                Integer quantity = params.containsKey("quantity")
                        ? Integer.valueOf(params.get("quantity").toString())
                        : 1;

                CreateOrderRequest.OrderItemRequest orderItem = new CreateOrderRequest.OrderItemRequest();
                orderItem.setProductId(productId);
                orderItem.setQuantity(quantity);
                orderRequest.setItems(List.of(orderItem));

                log.info("🔵 Order items count: 1 (buy now)");
            } else {
                // ✅ FLOW "THANH TOÁN TỪ GIỎ HÀNG": Lấy items từ cart
                log.info("🔵 Checkout from cart flow");
                CartResponse cart = cartService.getCart(user.getId());
                log.info("🔵 Cart items: {}", cart.getItems().size());

                if (cart.getItems().isEmpty()) {
                    log.warn("⚠️ Cart is empty");
                    return buildErrorResponse("Giỏ hàng trống. Vui lòng thêm sản phẩm trước.");
                }

                // Tạo order items từ cart
                List<CreateOrderRequest.OrderItemRequest> items = cart.getItems().stream()
                        .map(item -> {
                            CreateOrderRequest.OrderItemRequest orderItem = new CreateOrderRequest.OrderItemRequest();
                            orderItem.setProductId(item.getProductId());
                            orderItem.setQuantity(item.getQuantity());
                            return orderItem;
                        })
                        .collect(Collectors.toList());
                orderRequest.setItems(items);

                log.info("🔵 Order items count: {}", items.size());
            }

            log.info("🔵 Creating order...");
            OrderResponse order = orderService.createOrder(user.getId(), orderRequest);
            log.info("✅ Order created: {}", order.getOrderNumber());

            String aiResponse = String.format(
                    "✅ Đơn hàng #%s đã được tạo!\n\n" +
                            "Tổng tiền: %,dđ\n" +
                            "Phương thức: %s\n" +
                            "Địa chỉ: %s\n\n" +
                            "%s",
                    order.getOrderNumber(),
                    order.getTotalAmount().longValue(),
                    "ONLINE".equals(originalPaymentMethod) || "BANK_TRANSFER".equals(originalPaymentMethod)
                            ? "Thanh toán online"
                            : "COD",
                    order.getShippingAddress(),
                    "ONLINE".equals(originalPaymentMethod) || "BANK_TRANSFER".equals(originalPaymentMethod)
                            ? "Vui lòng thanh toán để hoàn tất đơn hàng."
                            : "Đơn hàng sẽ được giao trong 2-3 ngày.");

            List<ChatResponse.QuickAction> quickActions = new ArrayList<>();

            if ("ONLINE".equals(originalPaymentMethod) || "BANK_TRANSFER".equals(originalPaymentMethod)) {
                log.info("✅ Adding VNPay, MoMo and PayOS payment options");

                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("💳 Thanh toán VNPay")
                        .action("PAY_VNPAY")
                        .params(Map.of("orderId", order.getId()))
                        .build());

                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("💰 Thanh toán MoMo")
                        .action("PAY_MOMO")
                        .params(Map.of("orderId", order.getId()))
                        .build());

                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("🏦 Thanh toán PayOS")
                        .action("PAY_PAYOS")
                        .params(Map.of("orderId", order.getId()))
                        .build());
            }

            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("📦 Xem đơn hàng")
                    .action("VIEW_ORDER")
                    .params(Map.of("orderId", order.getId()))
                    .build());

            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("🛍️ Tiếp tục mua")
                    .action("SEARCH_PRODUCTS")
                    .params(new HashMap<>())
                    .build());

            log.info("🔵 Quick actions count: {}", quickActions.size());

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("order", order);

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.ORDER_CONFIRM)
                    .actionData(actionData)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error in checkout", e);
            log.error("❌ Error details: {}", e.getMessage());
            log.error("❌ Stack trace: ", e);
            return buildErrorResponse("Không thể tạo đơn hàng. Vui lòng thử lại. Lỗi: " + e.getMessage());
        }
    }

    // ✅ NEW: Handle VIEW_ORDER action
    private ChatResponse handleViewOrder(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());
            OrderResponse order = orderService.getOrderById(orderId, user.getId());

            StringBuilder aiResponse = new StringBuilder();
            aiResponse.append(String.format("📦 **Đơn hàng #%s**\n\n", order.getOrderNumber()));
            aiResponse.append(String.format("📅 Ngày đặt: %s\n", order.getCreatedAt()));
            aiResponse.append(String.format("💰 Tổng tiền: %,dđ\n", order.getTotalAmount().longValue()));
            aiResponse.append(String.format("📍 Trạng thái: %s\n", getOrderStatusText(order.getStatus())));
            aiResponse.append(String.format("💳 Thanh toán: %s\n\n", getPaymentMethodText(order.getPaymentMethod())));

            aiResponse.append("**Sản phẩm:**\n");
            if (order.getItems() != null) {
                for (var item : order.getItems()) {
                    aiResponse.append(String.format("• %s x%d - %,dđ\n",
                            item.getProductName(),
                            item.getQuantity(),
                            item.getPrice().longValue()));
                }
            }

            aiResponse.append(String.format("\n📫 Giao đến: %s\n", order.getShippingAddress()));
            aiResponse.append(String.format("📞 SĐT: %s\n", order.getRecipientPhone()));

            List<ChatResponse.QuickAction> quickActions = new ArrayList<>();

            // Add cancel button if order is pending
            if ("PENDING".equals(order.getStatus())) {
                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("❌ Hủy đơn hàng")
                        .action("CANCEL_ORDER")
                        .params(Map.of("orderId", order.getId()))
                        .build());
            }

            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("📋 Xem tất cả đơn")
                    .action("VIEW_ORDERS")
                    .params(new HashMap<>())
                    .build());

            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("🛍️ Tiếp tục mua")
                    .action("SEARCH_PRODUCTS")
                    .params(new HashMap<>())
                    .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse.toString())
                    .messageType(ChatResponse.MessageType.TEXT)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error viewing order", e);
            return buildErrorResponse("Không thể xem chi tiết đơn hàng. Vui lòng thử lại.");
        }
    }

    // ✅ NEW: Handle VIEW_ORDERS action
    private ChatResponse handleViewOrders(User user, ChatSession session) {
        try {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<OrderResponse> ordersPage = orderService.getUserOrders(user.getId(), pageable);

            if (ordersPage.isEmpty()) {
                return ChatResponse.builder()
                        .aiResponse("📦 Bạn chưa có đơn hàng nào.\n\nHãy khám phá các sản phẩm của chúng tôi!")
                        .messageType(ChatResponse.MessageType.TEXT)
                        .quickActions(List.of(
                                ChatResponse.QuickAction.builder()
                                        .label("🛍️ Xem sản phẩm")
                                        .action("SEARCH_PRODUCTS")
                                        .params(new HashMap<>())
                                        .build()))
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            StringBuilder aiResponse = new StringBuilder();
            aiResponse.append(String.format("📦 **Danh sách đơn hàng** (%d đơn)\n\n", ordersPage.getTotalElements()));

            List<ChatResponse.QuickAction> quickActions = new ArrayList<>();

            for (OrderResponse order : ordersPage.getContent()) {
                aiResponse.append(String.format("🔹 Đơn #%s\n", order.getOrderNumber()));
                aiResponse.append(String.format("   Ngày: %s\n", order.getCreatedAt()));
                aiResponse.append(String.format("   Tổng: %,dđ\n", order.getTotalAmount().longValue()));
                aiResponse.append(String.format("   Trạng thái: %s\n\n", getOrderStatusText(order.getStatus())));

                // Add quick action for each order
                quickActions.add(ChatResponse.QuickAction.builder()
                        .label(String.format("📦 #%s",
                                order.getOrderNumber().substring(0, Math.min(8, order.getOrderNumber().length()))))
                        .action("VIEW_ORDER")
                        .params(Map.of("orderId", order.getId()))
                        .build());
            }

            // Add general actions
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("🛍️ Tiếp tục mua")
                    .action("SEARCH_PRODUCTS")
                    .params(new HashMap<>())
                    .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse.toString())
                    .messageType(ChatResponse.MessageType.TEXT)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error viewing orders", e);
            return buildErrorResponse("Không thể xem danh sách đơn hàng. Vui lòng thử lại.");
        }
    }

    // Helper methods for order display
    private String getOrderStatusText(String status) {
        return switch (status) {
            case "PENDING" -> "⏳ Chờ xác nhận";
            case "CONFIRMED" -> "✅ Đã xác nhận";
            case "PROCESSING" -> "📦 Đang xử lý";
            case "SHIPPING" -> "🚚 Đang giao hàng";
            case "DELIVERED" -> "✅ Đã giao hàng";
            case "CANCELLED" -> "❌ Đã hủy";
            default -> status;
        };
    }

    private String getPaymentMethodText(String method) {
        return switch (method) {
            case "CASH" -> "💵 Tiền mặt";
            case "BANK_TRANSFER" -> "🏦 Chuyển khoản";
            case "MOMO" -> "🟣 MoMo";
            case "VNPAY" -> "🔵 VNPay";
            default -> method;
        };
    }

    private ChatResponse handleBookCourtAction(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long courtId = Long.valueOf(params.get("courtId").toString());
            CourtResponse court = courtService.getCourtById(courtId);

            String aiResponse = String.format(
                    "🏸 Đặt sân: %s\n\n" +
                            "📍 Địa chỉ: %s\n" +
                            "💰 Giá: %,dđ/giờ\n" +
                            "⏰ Giờ mở cửa: %s - %s\n\n" +
                            "Bạn muốn đặt sân vào ngày nào?",
                    court.getName(),
                    court.getAddress(),
                    court.getPricePerHour().longValue(),
                    court.getOpenTime() != null ? court.getOpenTime().toString() : "N/A",
                    court.getCloseTime() != null ? court.getCloseTime().toString() : "N/A");

            List<ChatResponse.QuickAction> quickActions = List.of(
                    ChatResponse.QuickAction.builder()
                            .label("📅 Đặt hôm nay")
                            .action("BOOK_COURT_TODAY")
                            .params(Map.of("courtId", courtId))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("📅 Đặt ngày mai")
                            .action("BOOK_COURT_TOMORROW")
                            .params(Map.of("courtId", courtId))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("📅 Chọn ngày khác")
                            .action("BOOK_COURT_CUSTOM")
                            .params(Map.of("courtId", courtId))
                            .build());

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("court", court);

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.BOOKING_CONFIRM)
                    .actionData(actionData)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error in book court action", e);
            return buildErrorResponse("Không thể xử lý đặt sân. Vui lòng thử lại.");
        }
    }

    private ChatResponse handlePayVNPay(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long orderId = Long.valueOf(params.get("orderId").toString());

            // Gọi VNPay service (cần inject VNPayService)
            // VNPayPaymentResponse vnpayResponse =
            // vnPayService.createOrderPaymentUrl(orderId, request);

            String aiResponse = String.format(
                    "✅ Đã tạo link thanh toán VNPay!\n\n" +
                            "Vui lòng click vào link bên dưới để thanh toán:\n" +
                            "👉 [Thanh toán ngay](payment_url_here)\n\n" +
                            "Link có hiệu lực trong 15 phút.");

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.TEXT)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating VNPay payment", e);
            return buildErrorResponse("Không thể tạo thanh toán VNPay. Vui lòng thử lại.");
        }
    }

    // ✅ THÊM: Xem giỏ hàng
    private ChatResponse handleViewCart(User user, ChatSession session) {
        try {
            CartResponse cart = cartService.getCart(user.getId());

            if (cart.getItems().isEmpty()) {
                return ChatResponse.builder()
                        .aiResponse("🛒 Giỏ hàng của bạn đang trống.\n\nHãy tìm sản phẩm để mua sắm nhé!")
                        .messageType(ChatResponse.MessageType.TEXT)
                        .quickActions(List.of(
                                ChatResponse.QuickAction.builder()
                                        .label("🛍️ Tìm sản phẩm")
                                        .action("SEARCH_PRODUCTS")
                                        .params(new HashMap<>())
                                        .build()))
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            StringBuilder cartInfo = new StringBuilder("🛒 Giỏ hàng của bạn:\n\n");

            for (CartItemResponse item : cart.getItems()) {
                cartInfo.append(String.format(
                        "• %s\n" +
                                "  Số lượng: %d x %,dđ = %,dđ\n\n",
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice().longValue(),
                        item.getSubtotal().longValue()));
            }

            cartInfo.append(String.format(
                    "━━━━━━━━━━━━━━━━━━\n" +
                            "Tổng cộng: %,dđ\n\n" +
                            "Bạn muốn làm gì tiếp theo?",
                    cart.getTotalAmount().longValue()));

            List<ChatResponse.QuickAction> quickActions = new ArrayList<>();
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("💳 Thanh toán")
                    .action("CHECKOUT")
                    .params(new HashMap<>())
                    .build());
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("🛍️ Tiếp tục mua")
                    .action("SEARCH_PRODUCTS")
                    .params(new HashMap<>())
                    .build());

            // Add remove buttons for each item
            for (CartItemResponse item : cart.getItems()) {
                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("🗑️ Xóa " + item.getProductName())
                        .action("REMOVE_FROM_CART")
                        .params(Map.of("cartItemId", item.getId()))
                        .build());
            }

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("cart", cart);

            return ChatResponse.builder()
                    .aiResponse(cartInfo.toString())
                    .messageType(ChatResponse.MessageType.TEXT)
                    .actionData(actionData)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error viewing cart", e);
            return buildErrorResponse("Không thể xem giỏ hàng. Vui lòng thử lại.");
        }
    }

    // ✅ THÊM: Xem chi tiết sản phẩm
    private ChatResponse handleViewProductDetail(User user, Map<String, Object> params, ChatSession session) {
        try {
            Long productId = Long.valueOf(params.get("productId").toString());
            ProductDetailResponse product = productService.getProductById(productId);

            String aiResponse = String.format(
                    "📦 %s\n\n" +
                            "💰 Giá: %,dđ %s\n" +
                            "🏷️ Thương hiệu: %s\n" +
                            "📊 Đã bán: %d\n" +
                            "⭐ Đánh giá: %.1f/5 (%d reviews)\n" +
                            "📦 Còn lại: %d sản phẩm\n\n" +
                            "📝 Mô tả:\n%s",
                    product.getName(),
                    product.getPrice().longValue(),
                    product.getDiscountPercent() > 0
                            ? String.format("(-% d%%) từ %,dđ", product.getDiscountPercent(),
                                    product.getOriginalPrice().longValue())
                            : "",
                    product.getBrand(),
                    product.getSoldQuantity(),
                    product.getAverageRating(),
                    product.getReviewCount(),
                    product.getStockQuantity(),
                    product.getDescription());

            List<ChatResponse.QuickAction> quickActions = List.of(
                    ChatResponse.QuickAction.builder()
                            .label("🛒 Thêm vào giỏ")
                            .action("ADD_TO_CART")
                            .params(Map.of("productId", productId, "quantity", 1))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("💳 Mua ngay")
                            .action("BUY_NOW")
                            .params(Map.of("productId", productId, "quantity", 1))
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("🔍 Tìm sản phẩm tương tự")
                            .action("SEARCH_PRODUCTS")
                            .params(Map.of("categoryId", product.getCategoryId()))
                            .build());

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("product", product);

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.TEXT)
                    .actionData(actionData)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error viewing product detail", e);
            return buildErrorResponse("Không thể xem chi tiết sản phẩm. Vui lòng thử lại.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatResponse> getChatHistory(Long userId, Pageable pageable) {
        return chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(msg -> ChatResponse.builder()
                        .messageId(msg.getId())
                        .aiResponse(msg.getAiResponse())
                        .timestamp(msg.getCreatedAt())
                        .build());
    }

    @Override
    public void clearSession(Long userId, String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionIdAndStatus(
                sessionId, ChatSession.SessionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy session"));

        session.setStatus(ChatSession.SessionStatus.COMPLETED);
        chatSessionRepository.save(session);
        log.info("✅ Session cleared: {}", sessionId);
    }

    // ==================== PRIVATE METHODS ====================

    private ChatSession getOrCreateSession(User user, String sessionId) {
        if (sessionId != null) {
            Optional<ChatSession> existing = chatSessionRepository.findBySessionIdAndStatus(
                    sessionId, ChatSession.SessionStatus.ACTIVE);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ChatSession newSession = new ChatSession();
        newSession.setUser(user);
        newSession.setSessionId(UUID.randomUUID().toString());
        newSession.setStatus(ChatSession.SessionStatus.ACTIVE);
        newSession.setExpiredAt(LocalDateTime.now().plusHours(24));
        newSession.setContext("{}");

        return chatSessionRepository.save(newSession);
    }

    private void updateLocationFromRequest(User user, ChatRequest request) {
        try {
            user.setLatitude(request.getLatitude());
            user.setLongitude(request.getLongitude());
            userRepository.save(user);
            log.info("📍 Updated user location: {}, {}", request.getLatitude(), request.getLongitude());
        } catch (Exception e) {
            log.error("❌ Failed to update user location", e);
        }
    }

    private MessageIntent analyzeIntent(String message, User user) {
        String lowerMessage = message.toLowerCase().trim();

        // Cart keywords
        if (lowerMessage.contains("giỏ hàng") || lowerMessage.contains("cart") ||
                lowerMessage.contains("xem giỏ")) {
            return new MessageIntent(IntentType.VIEW_CART, new HashMap<>());
        }

        // Court search keywords
        if (lowerMessage.contains("sân") || lowerMessage.contains("court") ||
                lowerMessage.contains("gần") || lowerMessage.contains("tìm sân") ||
                lowerMessage.contains("sân nào")) {
            return new MessageIntent(IntentType.COURT_SEARCH, extractCourtSearchParams(message));
        }

        // Booking keywords
        if (lowerMessage.contains("đặt sân") || lowerMessage.contains("book") ||
                lowerMessage.contains("thuê sân") || lowerMessage.contains("booking")) {
            return new MessageIntent(IntentType.COURT_BOOKING, extractBookingParams(message));
        }

        // Product search keywords
        if (lowerMessage.contains("vợt") || lowerMessage.contains("giày") ||
                lowerMessage.contains("áo") || lowerMessage.contains("sản phẩm") ||
                lowerMessage.contains("mua") || lowerMessage.contains("shop") ||
                lowerMessage.contains("quần") || lowerMessage.contains("phụ kiện")) {
            return new MessageIntent(IntentType.PRODUCT_SEARCH, extractProductSearchParams(message));
        }

        // Tier info keywords
        if (lowerMessage.contains("cấp bậc") || lowerMessage.contains("tier") ||
                lowerMessage.contains("hạng") || lowerMessage.contains("ưu đãi") ||
                lowerMessage.contains("vip") || lowerMessage.contains("thành viên")) {
            return new MessageIntent(IntentType.TIER_INFO, new HashMap<>());
        }

        return new MessageIntent(IntentType.GENERAL, new HashMap<>());
    }

    // ✅ FIXED: Court Search Handler
    private ChatResponse handleCourtSearch(User user, MessageIntent intent, ChatSession session) {
        List<CourtResponse> courts = new ArrayList<>();

        try {
            if (user.getLatitude() != null && user.getLongitude() != null) {
                // Tìm sân gần user
                courts = locationService.findNearbyCourts(user.getId(), 10.0);
                log.info("📍 Found {} courts near user location", courts.size());
            } else {
                // Lấy tất cả sân nếu chưa có location
                Page<CourtResponse> courtPage = courtService.getAllCourts(
                        PageRequest.of(0, 5, Sort.by("createdAt").descending()));
                courts = courtPage.getContent();
                log.info("📍 Returning {} courts (no user location)", courts.size());
            }
        } catch (Exception e) {
            log.error("❌ Error fetching courts", e);
            return buildErrorResponse("Không thể tìm sân lúc này. Vui lòng thử lại sau.");
        }

        // Handle empty courts
        if (courts.isEmpty()) {
            return buildNoCourtsResponse(user);
        }

        return buildCourtListResponse(courts, user);
    }

    private ChatResponse handleCourtBooking(User user, MessageIntent intent, ChatSession session) {
        try {
            var tierInfo = userTierService.getUserTierInfo(user.getId());
            boolean canBookWithoutDeposit = userTierService.canBookWithoutDeposit(user);

            String aiResponse = canBookWithoutDeposit
                    ? "🎉 Bạn là thành viên VIP! Bạn có thể đặt sân mà không cần cọc. " +
                            "Bạn vẫn muốn thanh toán trước không?"
                    : String.format("Để đặt sân, bạn cần cọc %d%% (cấp bậc %s). " +
                            "Bạn muốn cọc hay thanh toán toàn bộ?",
                            tierInfo.getDepositPercentage(), tierInfo.getTier());

            List<ChatResponse.QuickAction> quickActions = new ArrayList<>();
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("💰 Cọc " + tierInfo.getDepositPercentage() + "%")
                    .action("BOOK_WITH_DEPOSIT")
                    .params(intent.getParams())
                    .build());
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("💳 Thanh toán toàn bộ")
                    .action("BOOK_WITH_FULL_PAYMENT")
                    .params(intent.getParams())
                    .build());

            if (canBookWithoutDeposit) {
                quickActions.add(ChatResponse.QuickAction.builder()
                        .label("👑 Đặt không cần cọc")
                        .action("BOOK_WITHOUT_DEPOSIT")
                        .params(intent.getParams())
                        .build());
            }

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.BOOKING_CONFIRM)
                    .quickActions(quickActions)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error handling court booking", e);
            return buildErrorResponse("Không thể xử lý đặt sân lúc này. Vui lòng thử lại sau.");
        }
    }

    private ChatResponse handleProductSearch(User user, MessageIntent intent, ChatSession session) {
        try {
            String keyword = extractKeywordFromMessage(intent.getParams());

            // Search products by keyword
            List<ProductResponse> products;

            if (keyword != null && !keyword.isEmpty()) {
                // ✅ TÌM THEO KEYWORD
                products = productService.searchProducts(keyword, 0, 10).getContent();
                log.info("🔍 Found {} products for keyword: {}", products.size(), keyword);
            } else {
                // ✅ FALLBACK: Lấy sản phẩm bán chạy
                products = productService.getBestSellingProducts();
                log.info("📊 Returning {} best-selling products", products.size());
            }

            if (products.isEmpty()) {
                return ChatResponse.builder()
                        .aiResponse("Xin lỗi, hiện tại chưa có sản phẩm phù hợp với yêu cầu của bạn. Bạn muốn tìm gì?")
                        .messageType(ChatResponse.MessageType.TEXT)
                        .timestamp(LocalDateTime.now())
                        .quickActions(List.of(
                                ChatResponse.QuickAction.builder()
                                        .label("🏸 Tìm vợt cầu lông")
                                        .action("SEARCH_PRODUCTS")
                                        .params(Map.of("keyword", "vợt"))
                                        .build(),
                                ChatResponse.QuickAction.builder()
                                        .label("👟 Tìm giày")
                                        .action("SEARCH_PRODUCTS")
                                        .params(Map.of("keyword", "giày"))
                                        .build(),
                                ChatResponse.QuickAction.builder()
                                        .label("👕 Tìm quần áo")
                                        .action("SEARCH_PRODUCTS")
                                        .params(Map.of("keyword", "quần áo"))
                                        .build()))
                        .build();
            }

            // ✅ TẠO RESPONSE VỚI THÔNG TIN CHI TIẾT
            String aiResponse = keyword != null
                    ? String.format("Tôi tìm thấy %d sản phẩm về '%s' cho bạn:", products.size(), keyword)
                    : String.format("Đây là %d sản phẩm bán chạy nhất:", products.size());

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("products", products);
            actionData.put("keyword", keyword);

            List<ChatResponse.QuickAction> quickActions = products.stream()
                    .limit(5) // ✅ TĂNG TỪ 3 LÊN 5
                    .map(product -> ChatResponse.QuickAction.builder()
                            .label(String.format("Mua %s - %,dđ",
                                    product.getName().length() > 30
                                            ? product.getName().substring(0, 30) + "..."
                                            : product.getName(),
                                    product.getPrice().longValue()))
                            .action("BUY_PRODUCT")
                            .params(Map.of(
                                    "productId", product.getId(),
                                    "productName", product.getName(),
                                    "price", product.getPrice()))
                            .build())
                    .collect(Collectors.toList());

            // ✅ THÊM ACTION XEM TẤT CẢ
            quickActions.add(ChatResponse.QuickAction.builder()
                    .label("📋 Xem tất cả sản phẩm")
                    .action("VIEW_ALL_PRODUCTS")
                    .params(Map.of("keyword", keyword != null ? keyword : ""))
                    .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.PRODUCT_LIST)
                    .actionData(actionData)
                    .quickActions(quickActions)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error searching products", e);
            return buildErrorResponse("Không thể tìm sản phẩm lúc này. Vui lòng thử lại sau.");
        }
    }

    // ✅ THÊM METHOD EXTRACT KEYWORD
    private String extractKeywordFromMessage(Map<String, Object> params) {
        if (params.containsKey("keyword")) {
            return (String) params.get("keyword");
        }
        return null;
    }

    private ChatResponse handleProductOrder(User user, MessageIntent intent, ChatSession session) {
        try {
            String aiResponse = "Bạn muốn thanh toán online hay khi nhận hàng (COD)?";

            List<ChatResponse.QuickAction> quickActions = List.of(
                    ChatResponse.QuickAction.builder()
                            .label("💳 Thanh toán online")
                            .action("ORDER_ONLINE_PAYMENT")
                            .params(intent.getParams())
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("💵 Thanh toán khi nhận hàng (COD)")
                            .action("ORDER_COD")
                            .params(intent.getParams())
                            .build(),
                    ChatResponse.QuickAction.builder()
                            .label("🛒 Thêm vào giỏ hàng")
                            .action("ADD_TO_CART")
                            .params(intent.getParams())
                            .build());

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.ORDER_CONFIRM)
                    .quickActions(quickActions)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error handling product order", e);
            return buildErrorResponse("Không thể xử lý đơn hàng lúc này. Vui lòng thử lại sau.");
        }
    }

    private ChatResponse handleTierInfo(User user, ChatSession session) {
        try {
            var tierInfo = userTierService.getUserTierInfo(user.getId());

            String aiResponse = String.format(
                    "🏆 Thông tin cấp bậc của bạn:\n\n" +
                            "Cấp hiện tại: %s\n" +
                            "Tổng chi tiêu: %,d VND\n" +
                            "Phần trăm cọc: %d%%\n" +
                            "%s\n\n" +
                            "Ưu đãi: %s",
                    tierInfo.getTier(),
                    tierInfo.getTotalSpent().longValue(),
                    tierInfo.getDepositPercentage(),
                    tierInfo.getNextTierThreshold() != null
                            ? String.format("Còn %,d VND nữa lên cấp tiếp theo",
                                    tierInfo.getNextTierThreshold().subtract(tierInfo.getTotalSpent()).longValue())
                            : "Bạn đã đạt cấp cao nhất!",
                    tierInfo.getTierBenefits());

            return ChatResponse.builder()
                    .aiResponse(aiResponse)
                    .messageType(ChatResponse.MessageType.TEXT)
                    .quickActions(List.of(
                            ChatResponse.QuickAction.builder()
                                    .label("🏸 Đặt sân ngay")
                                    .action("SEARCH_COURTS")
                                    .params(new HashMap<>())
                                    .build(),
                            ChatResponse.QuickAction.builder()
                                    .label("🛍️ Mua sắm")
                                    .action("SEARCH_PRODUCTS")
                                    .params(new HashMap<>())
                                    .build()))
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting tier info", e);
            return buildErrorResponse("Không thể lấy thông tin cấp bậc lúc này. Vui lòng thử lại sau.");
        }
    }

    private ChatResponse handleGeneralChat(User user, String message, ChatSession session) {
        try {
            // Call OpenAI API if key is configured
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                String aiResponse = callOpenAI(message, user, session);
                return ChatResponse.builder()
                        .aiResponse(aiResponse)
                        .messageType(ChatResponse.MessageType.TEXT)
                        .build();
            } else {
                // Fallback response
                return buildDefaultResponse();
            }
        } catch (Exception e) {
            log.error("❌ Error in general chat", e);
            return buildDefaultResponse();
        }
    }

    private String callOpenAI(String userMessage, User user, ChatSession session) {
        try {
            String systemPrompt = buildSystemPrompt(user);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiModel);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }

            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";

        } catch (Exception e) {
            log.error("❌ Error calling OpenAI API", e);
            return "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
        }
    }

    private String buildSystemPrompt(User user) {
        try {
            var tierInfo = userTierService.getUserTierInfo(user.getId());

            return String.format(
                    "Bạn là trợ lý AI của hệ thống đặt sân cầu lông và bán hàng thể thao.\n" +
                            "Thông tin người dùng:\n" +
                            "- Tên: %s\n" +
                            "- Cấp bậc: %s\n" +
                            "- Phần trăm cọc: %d%%\n" +
                            "- Tổng chi tiêu: %,d VND\n\n" +
                            "Nhiệm vụ của bạn:\n" +
                            "1. Hỗ trợ tìm và đặt sân cầu lông\n" +
                            "2. Tư vấn và bán sản phẩm thể thao\n" +
                            "3. Giải thích về hệ thống cấp bậc và ưu đãi\n" +
                            "4. Trả lời thân thiện, ngắn gọn, dễ hiểu\n\n" +
                            "Hãy trả lời bằng tiếng Việt.",
                    user.getFullName(),
                    tierInfo.getTier(),
                    tierInfo.getDepositPercentage(),
                    tierInfo.getTotalSpent().longValue());
        } catch (Exception e) {
            log.error("❌ Error building system prompt", e);
            return "Bạn là trợ lý AI của hệ thống đặt sân cầu lông và bán hàng thể thao. " +
                    "Hãy trả lời thân thiện bằng tiếng Việt.";
        }
    }

    private void saveChatMessage(User user, String userMessage, String aiResponse, IntentType type) {
        try {
            ChatMessage message = new ChatMessage();
            message.setUser(user);
            message.setUserMessage(userMessage);
            message.setAiResponse(aiResponse);
            message.setMessageType(ChatMessage.MessageType.valueOf(type.name()));
            chatMessageRepository.save(message);
        } catch (Exception e) {
            log.error("❌ Failed to save chat message", e);
            // Don't throw - this is not critical
        }
    }

    private Map<String, Object> extractCourtSearchParams(String message) {
        // TODO: Extract params like location, time, price range using NLP
        return new HashMap<>();
    }

    private Map<String, Object> extractBookingParams(String message) {
        // TODO: Extract params like courtId, date, time using NLP
        return new HashMap<>();
    }

    private Map<String, Object> extractProductSearchParams(String message) {
        Map<String, Object> params = new HashMap<>();
        String lowerMessage = message.toLowerCase().trim();

        // ✅ DETECT KEYWORD
        if (lowerMessage.contains("vợt")) {
            params.put("keyword", "vợt");
        } else if (lowerMessage.contains("giày")) {
            params.put("keyword", "giày");
        } else if (lowerMessage.contains("áo") || lowerMessage.contains("quần")) {
            params.put("keyword", "quần áo");
        } else if (lowerMessage.contains("cầu lông") && !lowerMessage.contains("vợt")) {
            params.put("keyword", "cầu lông"); // Quả cầu
        } else if (lowerMessage.contains("túi") || lowerMessage.contains("balo")) {
            params.put("keyword", "túi");
        } else if (lowerMessage.contains("phụ kiện")) {
            params.put("keyword", "phụ kiện");
        }

        // ✅ DETECT BRAND
        if (lowerMessage.contains("yonex")) {
            params.put("brand", "Yonex");
        } else if (lowerMessage.contains("victor")) {
            params.put("brand", "Victor");
        } else if (lowerMessage.contains("lining")) {
            params.put("brand", "Lining");
        }

        // ✅ DETECT PRICE RANGE
        if (lowerMessage.contains("rẻ") || lowerMessage.contains("giá rẻ")) {
            params.put("maxPrice", 1000000);
        } else if (lowerMessage.contains("cao cấp") || lowerMessage.contains("đắt")) {
            params.put("minPrice", 3000000);
        }

        return params;
    }

    // ==================== HELPER METHODS ====================

    private ChatResponse buildErrorResponse(String message) {
        return ChatResponse.builder()
                .aiResponse(message)
                .messageType(ChatResponse.MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .quickActions(List.of(
                        ChatResponse.QuickAction.builder()
                                .label("🏠 Về trang chủ")
                                .action("GO_HOME")
                                .params(new HashMap<>())
                                .build(),
                        ChatResponse.QuickAction.builder()
                                .label("📞 Liên hệ hỗ trợ")
                                .action("CONTACT_SUPPORT")
                                .params(new HashMap<>())
                                .build()))
                .build();
    }

    private ChatResponse buildNoCourtsResponse(User user) {
        boolean hasLocation = user.getLatitude() != null && user.getLongitude() != null;

        String message = hasLocation
                ? "Hiện tại không có sân nào gần vị trí của bạn. Bạn có thể:\n" +
                        "1. Mở rộng phạm vi tìm kiếm\n" +
                        "2. Xem tất cả sân có sẵn\n" +
                        "3. Xem sản phẩm thể thao"
                : "Hiện tại chưa có sân nào. Bạn có thể:\n" +
                        "1. Cung cấp vị trí để tìm sân gần bạn\n" +
                        "2. Xem sản phẩm thể thao";

        List<ChatResponse.QuickAction> actions = new ArrayList<>();
        actions.add(ChatResponse.QuickAction.builder()
                .label("📍 Cập nhật vị trí")
                .action("UPDATE_LOCATION")
                .params(new HashMap<>())
                .build());
        actions.add(ChatResponse.QuickAction.builder()
                .label("🛍️ Xem sản phẩm")
                .action("SEARCH_PRODUCTS")
                .params(new HashMap<>())
                .build());

        return ChatResponse.builder()
                .aiResponse(message)
                .messageType(ChatResponse.MessageType.TEXT)
                .quickActions(actions)
                .build();
    }

    private ChatResponse buildCourtListResponse(List<CourtResponse> courts, User user) {
        boolean hasLocation = user.getLatitude() != null && user.getLongitude() != null;

        String aiResponse = hasLocation
                ? String.format("Tôi tìm thấy %d sân gần bạn:", courts.size())
                : String.format("Đây là %d sân có sẵn:", courts.size());

        Map<String, Object> actionData = new HashMap<>();
        actionData.put("courts", courts);

        List<ChatResponse.QuickAction> quickActions = courts.stream()
                .limit(3) // Giới hạn 3 quick actions
                .map(court -> ChatResponse.QuickAction.builder()
                        .label("Đặt sân " + court.getName())
                        .action("BOOK_COURT")
                        .params(Map.of("courtId", court.getId()))
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .aiResponse(aiResponse)
                .messageType(ChatResponse.MessageType.COURT_LIST)
                .actionData(actionData)
                .quickActions(quickActions)
                .build();
    }

    private ChatResponse buildDefaultResponse() {
        return ChatResponse.builder()
                .aiResponse("Xin chào! Tôi có thể giúp bạn:\n" +
                        "🏸 Tìm và đặt sân cầu lông\n" +
                        "🛍️ Mua sắm sản phẩm thể thao\n" +
                        "👑 Xem thông tin cấp bậc\n\n" +
                        "Bạn cần giúp gì?")
                .messageType(ChatResponse.MessageType.TEXT)
                .quickActions(List.of(
                        ChatResponse.QuickAction.builder()
                                .label("🏸 Tìm sân")
                                .action("SEARCH_COURTS")
                                .params(new HashMap<>())
                                .build(),
                        ChatResponse.QuickAction.builder()
                                .label("🛍️ Mua sắm")
                                .action("SEARCH_PRODUCTS")
                                .params(new HashMap<>())
                                .build(),
                        ChatResponse.QuickAction.builder()
                                .label("👑 Cấp bậc")
                                .action("VIEW_TIER")
                                .params(new HashMap<>())
                                .build()))
                .build();
    }

    // ==================== INNER CLASSES ====================

    private enum IntentType {
        GENERAL, COURT_SEARCH, COURT_BOOKING, PRODUCT_SEARCH,
        PRODUCT_ORDER, TIER_INFO, LOCATION_UPDATE, VIEW_CART // ← THÊM VIEW_CART
    }

    private static class MessageIntent {
        private final IntentType type;
        private final Map<String, Object> params;

        public MessageIntent(IntentType type, Map<String, Object> params) {
            this.type = type;
            this.params = params;
        }

        public IntentType getType() {
            return type;
        }

        public Map<String, Object> getParams() {
            return params;
        }
    }
}
