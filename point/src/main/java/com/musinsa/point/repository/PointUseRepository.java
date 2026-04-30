package com.musinsa.point.repository;

import com.musinsa.point.domain.PointUse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointUseRepository extends JpaRepository<PointUse, String> {
    Optional<PointUse> findPointUseByUseIdAndMemberId(String useId, String memberId);
}
