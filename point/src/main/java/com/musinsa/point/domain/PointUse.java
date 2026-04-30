package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * [포인트 사용 원장]
 *
 * - 특정 주문에서 사용된 포인트를 관리
 *
 * 필드 설명:
 * - useAmount: 최초 사용 금액
 * - canceledAmount: 취소된 금액
 *
 * 역할:
 * - 주문 단위 포인트 사용 관리
 *
 * 현재 사용 상태:
 * - 실제 사용 금액 = useAmount - canceledAmount
 */
@Entity
@Table(name = "TB_POINT_USE")
@NoArgsConstructor
@Data
public class PointUse {
    @Id
    @Column(name = "USE_ID")
    private String useId;

    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "ORDER_NUMBER")
    private String orderNumber;

    @Column(name = "USE_AMOUNT")
    private Long useAmount;

    @Column(name = "CANCEL_AMOUNT")
    private Long canceledAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private UseStatus status;

    @Column(name = "REGISTERED_AT")
    private LocalDate registeredAt;

    public PointUse(String useId, String memberId, String orderNumber, Long useAmount, Long canceledAmount, UseStatus status) {
        this.useId = useId;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.useAmount = useAmount;
        this.canceledAmount = canceledAmount;
        this.registeredAt = LocalDate.now();
        this.status = status;
    }

    public enum UseStatus {
        USED,        // 정상 사용
        PARTIAL_CANCELED, // 일부 취소
        CANCELED     // 전체 취소
    }
}
