package com.badminton.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoMoTransactionStatusResponse {
    private String orderId;
    private String requestId;
    private Long transId;
    private Integer resultCode;
    private String message;
    private Long amount;
    private String payType;
    private Long responseTime;
    private String extraData;
    
    // Thêm field để hiển thị cho admin
    private String statusDescription;
    private Boolean canConfirmManually;
}
