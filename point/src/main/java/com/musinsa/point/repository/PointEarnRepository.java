package com.musinsa.point.repository;

import com.musinsa.point.domain.PointEarn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.*;

public interface PointEarnRepository extends JpaRepository<PointEarn, String> {
    List<PointEarn> findPointEarnsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(String memberId, PointEarn.EarnStatus earnStatus, LocalDate localDate);
    Optional<PointEarn> findPointEarnByEarnIdAndMemberId(String earnId, String memberId);
}
