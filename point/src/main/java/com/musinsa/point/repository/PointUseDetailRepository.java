package com.musinsa.point.repository;

import com.musinsa.point.domain.PointUseDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointUseDetailRepository extends JpaRepository<PointUseDetail, Long> {
    List<PointUseDetail> findAllByUseIdIs(String useId);
}
