package com.badminton.dto.request;

import lombok.Data;

@Data
public class MoMoWebhookRequest {
    private String partnerCode;
    private String orderId;
    private String requestId;
    
    private Long amount;
    private String orderInfo;
    private String orderType;
    private Long transId;
    private Integer resultCode;
    private String message;
    private String payType;
    private Long responseTime;
    private String extraData;
    private String signature;
}
