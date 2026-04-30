package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * [포인트 적립 원장]
 *
 * - 적립 단위별 포인트 관리
 *
 * 역할:
 * - 어떤 적립 포인트가 얼마 남았는지 관리
 * - 포인트 사용 시 차감 대상
 */
@Entity
@Table(name = "TB_POINT_EARN")
@Data
@NoArgsConstructor
public class PointEarn {
    @Id
    @Column(name = "EARN_ID")
    private String earnId;

    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "AMOUNT")
    private Long amount;

    @Column(name = "EARN_TYPE")
    @Enumerated(EnumType.STRING)
    private EarnType earnType;

    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private EarnStatus status;

    @Column(name = "REMAINING_AMOUNT")
    private Long remainingAmount;

    @Column(name = "EXPIRED_AT")
    private LocalDate expiredAt;

    @Column(name = "REGISTERED_AT")
    private LocalDate registeredAt;

    public PointEarn(String earnId, String memberId, Long amount, EarnType earnType) {
        this.earnId = earnId;
        this.memberId = memberId;
        this.amount = amount;
        this.remainingAmount = amount;
        this.earnType = earnType;
        this.status = EarnStatus.EARNED;
        this.registeredAt = LocalDate.now();
    }

    public enum EarnType {
        MANUAL,
        EVENT,
        USE_CANCEL
    }

    public enum EarnStatus {
        EARNED,
        CANCELED
    }

    public boolean isExpired() {
        return expiredAt.isBefore(LocalDate.now());
    }
}
