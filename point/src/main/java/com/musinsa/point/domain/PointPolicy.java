package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [포인트 정책]
 *
 * - 포인트 적립 가능 금액 및 회원 보유 한도를 관리
 *
 * 정책 적용 우선순위:
 * 1. 특정 회원(member_id) 정책 존재 시 해당 정책 사용
 * 2. 없으면 기본 정책(member_id = null) 사용
 *
 * 검증 규칙:
 * - 적립 금액 <= MAX_EARN_AMOUNT
 * - 현재 잔액 + 적립 금액 <= MAX_BALANCE_AMOUNT
 */
@Entity
@Table(name = "TB_POINT_POLICY")
@NoArgsConstructor
@Data
public class PointPolicy {

    @Id
    @Column(name = "POLICY_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "MAX_EARN_AMOUNT", nullable = false)
    private Long maxEarnAmount;

    @Column(name = "MAX_BALANCE_AMOUNT", nullable = false)
    private Long maxBalanceAmount;

    public boolean isValidEarnAmount(Long amount) {
        return amount >= 1 && amount <= maxEarnAmount;
    }

    public boolean isExceedMaxBalance(Long currentAmount, Long earnAmount) {
        return currentAmount + earnAmount > maxBalanceAmount;
    }
}
