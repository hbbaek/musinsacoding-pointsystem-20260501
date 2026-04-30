package com.musinsa.point.service;

import com.musinsa.point.domain.PointEarn;
import com.musinsa.point.domain.PointUse;
import com.musinsa.point.domain.PointUseDetail;
import com.musinsa.point.domain.PointWallet;
import com.musinsa.point.dto.PointUseDto;
import com.musinsa.point.exception.BusinessException;
import com.musinsa.point.repository.PointEarnRepository;
import com.musinsa.point.repository.PointUseDetailRepository;
import com.musinsa.point.repository.PointUseRepository;
import com.musinsa.point.repository.PointWalletRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class PointUseService {
    private final PointEarnRepository pointEarnRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointUseRepository pointUseRepository;
    private final PointUseDetailRepository pointUseDetailRepository;

    /*
    [사용]
    1. 주문시에만 포인트를 사용할 수 있다고 가정한다.
    2. 포인트 사용시에는 주문번호를 함께 기록하여 어떤 주문에서 얼마의 포인트를 사용했는지 식별할 수 있어야 한다.
    3. 포인트 사용시에는 관리자가 수기 지급한 포인트가 우선 사용되어야 하며, 만료일이 짧게 남은 순서로 사용해야 한다.
    */
    @Transactional
    public PointUseDto.UseResponse usePoint(PointUseDto.UseRequest request) {
        // 요청값 검증
        request.validate();

        String memberId = request.getMemberId();
        Long amount = request.getAmount();

        // 사용 적립 정보 생성
        PointUse pointUse = new PointUse(UUID.randomUUID().toString(), memberId, request.getOrderNumber(), amount, 0L, PointUse.UseStatus.USED);

        // 회원 지갑 정보 조회
        PointWallet pointWallet = pointWalletRepository.findPointWalletByMemberId(memberId);
        if(pointWallet == null)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "회원 지갑 정보가 없습니다.");

        // 회원 포인트 잔액 검증
        if(pointWallet.getBalance() < amount)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 잔액이 부족합니다.");

        // 사용 가능한 적립 원장 목록 조회
        List<PointEarn> pointEarnList = pointEarnRepository.findPointEarnsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(memberId, PointEarn.EarnStatus.EARNED, LocalDate.now());
        if(pointEarnList.isEmpty())
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 적립 내역이 존재하지 않습니다.");

        // 차감 우선순위 정렬
        pointEarnList.sort(new Comparator<PointEarn>() {
            @Override
            public int compare(PointEarn o1, PointEarn o2) {

                // 수기로 적립한 포인트를 먼저 차감
                if(o1.getEarnType() == PointEarn.EarnType.MANUAL && o2.getEarnType() != PointEarn.EarnType.MANUAL)
                    return -1;
                else if(o1.getEarnType() != PointEarn.EarnType.MANUAL && o2.getEarnType() == PointEarn.EarnType.MANUAL)
                    return 1;

                // 만료일자가 빠른 포인트를 먼저 차감
                if(o1.getExpiredAt().isBefore(o2.getExpiredAt()))
                    return -1;
                else if(o1.getExpiredAt().isAfter(o2.getExpiredAt()))
                    return 1;
                else { // 등록일자가 빠른 포인트를 먼저 차감
                    if(o1.getRegisteredAt().isBefore(o2.getRegisteredAt()))
                        return -1;
                    else if(o1.getRegisteredAt().isAfter(o2.getRegisteredAt()))
                        return 1;
                    return 0;
                }
            }
        });

        Long remainUseAmount = amount;
        List<PointUseDetail> detailList = new ArrayList<>();

        // 적립 원장 리스트 순회하면서 차감
        for(PointEarn pointEarn : pointEarnList) {
            if(remainUseAmount == 0L)
                break;

            if (pointEarn.getRemainingAmount() <= 0L)
                continue;

            // 이번 적립 포인트에서 실제로 사용할 금액
            // - 남은 사용 요청 금액과 적립 포인트 RemainingAmount 중 작은 값
            Long usablePoint = Math.min(remainUseAmount, pointEarn.getRemainingAmount());

            // 적립에서 RemainingAmount 차감
            pointEarn.setRemainingAmount(pointEarn.getRemainingAmount() - usablePoint);

            // 포인트 사용 상세 생성
            detailList.add(new PointUseDetail(pointUse.getUseId(), pointEarn.getEarnId(), usablePoint, 0L));

            // 차감
            remainUseAmount -= usablePoint;
        }

        if(remainUseAmount != 0L)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 사용에 실패하였습니다.");

        // 회원 지갑 잔액 감소
        pointWallet.decrease(amount);

        // DB 저장
        pointWalletRepository.save(pointWallet); // 회원 지갑 잔액 저장
        pointEarnRepository.saveAll(pointEarnList); // 포인트 적립 원장 저장
        pointUseRepository.save(pointUse); // 포인트 사용 원장 저장
        pointUseDetailRepository.saveAll(detailList); // 포인트 사용 상세 저장

        return new PointUseDto.UseResponse(pointUse.getUseId());
    }

    /*
    [사용 취소]
    1. 사용한 금액중 전체 또는 일부를 사용취소 할수 있다.
    2. 사용취소 시점에 이미 만료된 포인를 사용취소 해야 한다면 그 금액만큼 신규적립 처리 한다.
     */
    @Transactional
    public void cancelUsePoint(PointUseDto.CancelRequest request) {
        // 요청값 검증
        request.validate();

        String useId = request.getUseId();
        String memberId = request.getMemberId();
        Long amount = request.getAmount();

        // 회원 지갑 조회
        PointWallet pointWallet = pointWalletRepository.findPointWalletByMemberId(memberId);
        if(pointWallet == null)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "회원 지갑 정보가 없습니다.");

        // 사용 원장 조회
        PointUse pointUse = pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "포인트 사용 이력이 없습니다."));

        // 이미 취소 여부
        if(pointUse.getStatus() == PointUse.UseStatus.CANCELED)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 취소된 포인트 입니다.");

        // 포인트 사용 상세 조회
        List<PointUseDetail> pointUseDetailList = pointUseDetailRepository.findAllByUseIdIs(useId);
        if(pointUseDetailList.isEmpty())
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 사용 상세 정보가 없습니다.");

        // 적립 복구
        // - 부분 취소를 고려하여 요청 취소 금액(amount)만큼만 복구
        // - 포인트 상세 별로 취소 가능한 금액을 계산한 뒤, 남은 취소 요청 금액만큼만 복구
        Long remainCancelAmount = amount;

        List<PointEarn> pointEarnList = new ArrayList<>();
        for(PointUseDetail pointUseDetail : pointUseDetailList) {
            if(remainCancelAmount == 0L)
                break;

            // 사용 상세에서 아직 취소 가능한 금액 계산
            Long cancelableAmount = pointUseDetail.getUseAmount() - pointUseDetail.getCancelAmount();
            if (cancelableAmount <= 0L)
                continue;

            // 이번 포인트 상세에서 실제로 복구할 금액
            // - 남은 취소 요청 금액과 detail 취소 가능 금액 중 작은 값
            Long restoreAmount = Math.min(remainCancelAmount, cancelableAmount);

            // 적립 원장 조회
            PointEarn pointEarn = pointEarnRepository.findPointEarnByEarnIdAndMemberId(pointUseDetail.getEarnId(), memberId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "포인트 적립 정보가 존재하지 않습니다."));

            // 적립이 만료된 경우, 신규 적립
            if(pointEarn.isExpired()) {
                PointEarn cancelPointEarn = new PointEarn(UUID.randomUUID().toString(), memberId, restoreAmount, PointEarn.EarnType.USE_CANCEL);
                cancelPointEarn.setExpiredAt(LocalDate.now().plusDays(365));
                pointEarnList.add(cancelPointEarn);
            } else {
                // 기존 적립 내역에 차감되었던 금액만큼 remainingAmount 복구
                pointEarn.setRemainingAmount(pointEarn.getRemainingAmount() + restoreAmount);
                pointEarnList.add(pointEarn);
            }

            // 사용 상세의 취소 금액 누적
            pointUseDetail.setCancelAmount(pointUseDetail.getCancelAmount() + restoreAmount);

            remainCancelAmount -= restoreAmount;
        }

        if(remainCancelAmount != 0L)
            throw new BusinessException(HttpStatus.BAD_REQUEST, "포인트 사용 취소에 실패하였습니다.");

        // 지갑 복구
        pointWallet.increase(amount);

        // 사용 원장의 취소 금액 누적
        Long newCanceledAmount = pointUse.getCanceledAmount() + amount;
        pointUse.setCanceledAmount(newCanceledAmount);

        if (newCanceledAmount.equals(pointUse.getUseAmount())) {
            pointUse.setStatus(PointUse.UseStatus.CANCELED); // 요청 취소 금액과 취소 누적금액이 같으면 전체 취소
        } else {
            pointUse.setStatus(PointUse.UseStatus.PARTIAL_CANCELED); // 다르면 부분 취소
        }

        // 저장
        pointUseRepository.save(pointUse);
        pointWalletRepository.save(pointWallet);
        pointEarnRepository.saveAll(pointEarnList);
        pointUseDetailRepository.saveAll(pointUseDetailList);
    }
}
