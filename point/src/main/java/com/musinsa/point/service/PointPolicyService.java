package com.musinsa.point.service;

import com.musinsa.point.domain.PointPolicy;
import com.musinsa.point.exception.BusinessException;
import com.musinsa.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointPolicyService {
    private final PointPolicyRepository pointPolicyRepository;

    public PointPolicy getPolicy(String memberId) {
        // 회원 전용 정책 조회
        Optional<PointPolicy> pointPolicy = pointPolicyRepository.findPointPolicyByMemberId(memberId);

        // 존재하면 반환
        if(pointPolicy.isPresent()) {
            return pointPolicy.get();
        }

        // 기본 정책 조회 후 반환
        return pointPolicyRepository.findPointPolicyByMemberIdIsNull()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "포인트 정책이 존재하지 않습니다."));
    }
}
