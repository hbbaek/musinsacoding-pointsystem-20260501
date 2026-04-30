package com.musinsa.point.service;

import com.musinsa.point.domain.PointEarn;
import com.musinsa.point.domain.PointPolicy;
import com.musinsa.point.domain.PointWallet;
import com.musinsa.point.dto.PointEarnDto;
import com.musinsa.point.exception.BusinessException;
import com.musinsa.point.repository.PointEarnRepository;
import com.musinsa.point.repository.PointWalletRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class PointEarnService {
    final PointPolicyService pointPolicyService;
    final PointWalletRepository pointWalletRepository;
    final PointEarnRepository pointEarnRepository;

    /*
    [적립]
    1. 1회 적립가능 포인트는 1포인트 이상, 10만포인트 이하로 가능하며 1회 최대 적립가능 포인트는 하드코딩이 아닌 방법으로 제어할수 있어야 한다.
    2. 개인별로 보유 가능한 무료포인트의 최대금액 제한이 존재하며, 하드코딩이 아닌 별도의 방법으로 변경할 수 있어야 한다.
    3. 특정 시점에 적립된 포인트는 1원단위까지 어떤 주문에서 사용되었는지 추적할수 있어야 한다.
    4. 포인트 적립은 관리자가 수기로 지급할 수 있으며, 수기지급한 포인트는 다른 적립과 구분되어 식별할 수 있어야 한다.
    5. 모든 포인트는 만료일이 존재하며, 최소 1일이상 최대 5년 미만의 만료일을 부여할 수 있다. (기본 365일)
     */
    @Transactional
    public PointEarnDto.EarnResponse earnPoint(PointEarnDto.EarnRequest request) {
        // 요청값 검증
        request.validate();

        String memberId = request.getMemberId();
        Long amount = request.getAmount();

        // 회원에게 적용할 포인트 정책 조회
        PointPolicy pointPolicy = pointPolicyService.getPolicy(memberId);

        // 적립 금액이 정책의 최대 적립 가능 금액 범위 안에 있는지 검증
        if(!pointPolicy.isValidEarnAmount(request.getAmount())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "적립 가능한 포인트 금액이 아닙니다.");
        }

        // 회원 포인트 지갑 정보 조회
        PointWallet pointWallet = pointWalletRepository.findPointWalletByMemberId(memberId);
        // 없으면 회원 포인트 지갑 생성
        if(pointWallet == null)
            pointWallet = new PointWallet(memberId, 0L);

        // 현재 잔액 + 적립 금액이 정책의 최대 보유 가능 포인트를 초과하는지 검증
        if(pointPolicy.isExceedMaxBalance(pointWallet.getBalance(), amount))
            throw new BusinessException(HttpStatus.BAD_REQUEST, "최대 보유 가능 포인트를 초과합니다.");

        // 포인트 적립 원장 생성
        PointEarn pointEarn = new PointEarn(UUID.randomUUID().toString(), memberId, amount, request.getEarnType());

        if (request.getExpiredAt() != null) {
            pointEarn.setExpiredAt(request.getExpiredAt()); // 만료일 부여 시 최소 1일이상 최대 5년 미만 체크 추가 필요 (요청 파라미터에서 검증 선행)
        } else {
            pointEarn.setExpiredAt(LocalDate.now().plusDays(365)); // 만료일 설정이 없는 경우, 기본 365일로 설정
        }

        // 포인트 지갑 잔액 증가
        pointWallet.increase(amount);

        // 포인트 적립 원장, 포인트 지갑 DB 저장
        pointEarnRepository.save(pointEarn);
        pointWalletRepository.save(pointWallet);

        return new PointEarnDto.EarnResponse(pointEarn.getEarnId());
    }

    /*
    [적립 취소]
    1. 특정 적립행위에서 적립한 금액만큼 취소 가능하며, 적립한 금액중 일부가 사용된 경우라면 적립 취소 될 수 없다.
     */
    @Transactional
    public void cancelEarnPoint(PointEarnDto.CancelRequest request) {
        // 요청값 검증
        request.validate();

        String earnId = request.getEarnId();
        String memberId = request.getMemberId();
        Long amount = request.getAmount();

        // 포인트 적립 원장 조회
        PointEarn pointEarn = pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "적립된 포인트가 존재하지 않습니다."));

        // 이미 취소된 적립 건인지 검증
        if(pointEarn.getStatus() == PointEarn.EarnStatus.CANCELED)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 취소된 적립 포인트 입니다.");

        // 적립한 금액중 일부가 사용되었는지 검증
        if(!pointEarn.getRemainingAmount().equals(amount)) {
            log.info("remaining amount : {}, request amount : {}", pointEarn.getRemainingAmount(), amount);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "일부 사용된 적립 포인트 이므로, 취소가 불가능 합니다.");
        }

        // 회원 지갑 정보 조회
        PointWallet pointWallet = pointWalletRepository.findPointWalletByMemberId(memberId);
        if(pointWallet == null)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "회원 지갑 정보가 없습니다.");

        // 회원 지갑 잔액을 적립 금액만큼 감소
        pointWallet.decrease(amount);

        // 적립 원장 상태 변경
        pointEarn.setStatus(PointEarn.EarnStatus.CANCELED);
    }
}
