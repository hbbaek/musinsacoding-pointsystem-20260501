package com.musinsa.point.repository;

import com.musinsa.point.domain.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {
    Optional<PointPolicy> findPointPolicyByMemberId(String memberId);
    Optional<PointPolicy> findPointPolicyByMemberIdIsNull();
}
