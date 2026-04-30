package com.musinsa.point.repository;

import com.musinsa.point.domain.PointWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointWalletRepository extends JpaRepository<PointWallet, String> {
    PointWallet findPointWalletByMemberId(String memberId);
}
