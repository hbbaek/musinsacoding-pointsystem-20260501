package com.musinsa.point.domain;

import com.musinsa.point.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * [회원 포인트 지갑]
 *
 * - 회원의 현재 포인트 잔액을 관리
 *
 * 역할:
 * - 빠른 잔액 조회
 */
@Entity
@Table(name = "TB_POINT_WALLET")
@AllArgsConstructor
@Data
public class PointWallet {
    @Id
    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "BALANCE")
    private Long balance;

    public PointWallet() {

    }

    public void increase(Long amount) {
        this.balance += amount;
    }

    public void decrease(Long amount) {
        if(this.balance < amount)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        this.balance -= amount;
    }
}
