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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointUseServiceTest {
    @InjectMocks
    private PointUseService pointUseService;

    @Mock
    private PointEarnRepository pointEarnRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PointUseRepository pointUseRepository;

    @Mock
    private PointUseDetailRepository pointUseDetailRepository;

    @Test
    @DisplayName("포인트 사용 성공")
    void usePoint_success() {
        // given
        String memberId = "user1";
        Long amount = 1000L;

        // 포인트 사용 정보 생성
        PointUseDto.UseRequest request = new PointUseDto.UseRequest();
        request.setMemberId(memberId);
        request.setAmount(amount);
        request.setOrderNumber("order1");

        PointWallet pointWallet = new PointWallet(memberId, 3000L);

        // 포인트 적립 정보 생성
        PointEarn pointEarn = new PointEarn("earn1", memberId, 3000L, PointEarn.EarnType.EVENT);
        pointEarn.setExpiredAt(LocalDate.now().plusDays(10));

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointEarnRepository.findPointEarnsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(memberId, PointEarn.EarnStatus.EARNED, LocalDate.now())).thenReturn(new ArrayList<>(List.of(pointEarn)));

        // when
        PointUseDto.UseResponse response = pointUseService.usePoint(request);

        // then
        assertThat(response.getUseId()).isNotBlank();
        assertThat(pointWallet.getBalance()).isEqualTo(2000L);
        assertThat(pointEarn.getRemainingAmount()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("사용 가능한 적립 내역이 없으면 사용 실패")
    void usePoint_fail_emptyEarnList() {
        // given
        String memberId = "user1";

        PointUseDto.UseRequest request = new PointUseDto.UseRequest();
        request.setMemberId(memberId);
        request.setAmount(1000L);
        request.setOrderNumber("order1");

        PointWallet wallet = new PointWallet(memberId, 3000L);

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(wallet);

        when(pointEarnRepository.findPointEarnsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(memberId, PointEarn.EarnStatus.EARNED, LocalDate.now())).thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> pointUseService.usePoint(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("포인트 지갑 잔액이 부족하면 포인트 사용 실패")
    void usePoint_fail_notEnoughWalletBalance() {
        // given
        String memberId = "user1";

        PointUseDto.UseRequest request = new PointUseDto.UseRequest();
        request.setMemberId(memberId);
        request.setAmount(1000L);
        request.setOrderNumber("order1");

        PointWallet wallet = new PointWallet(memberId, 500L);
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(wallet);

        // when & then
        assertThatThrownBy(() -> pointUseService.usePoint(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("적립 원장 remainingAmount 부족으로 포인트 사용 실패")
    void usePoint_fail_notEnoughRemainPoint() {
        // given
        String memberId = "user1";
        String earnId = "earn1";

        PointUseDto.UseRequest request = new PointUseDto.UseRequest();
        request.setMemberId(memberId);
        request.setAmount(1000L);
        request.setOrderNumber("order1");

        PointWallet wallet = new PointWallet(memberId, 2000L);
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(wallet);

        PointEarn pointEarn = new PointEarn(earnId, memberId, 500L, PointEarn.EarnType.EVENT);
        pointEarn.setRemainingAmount(500L);
        pointEarn.setExpiredAt(LocalDate.now().plusDays(365));
        pointEarn.setRegisteredAt(LocalDate.now());

        when(pointEarnRepository.findPointEarnsByMemberIdAndStatusAndExpiredAtGreaterThanEqual(memberId, PointEarn.EarnStatus.EARNED, LocalDate.now())).thenReturn(new ArrayList<>(List.of(pointEarn)));

        // when & then
        assertThatThrownBy(() -> pointUseService.usePoint(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("포인트 전체 사용취소 성공")
    void cancelUsePoint_success() {
        // given
        String memberId = "member1";
        String useId = "use1";
        String earnId = "earn1";

        PointUseDto.CancelRequest request = new PointUseDto.CancelRequest();
        request.setMemberId(memberId);
        request.setUseId(useId);
        request.setAmount(1000L);

        // 회원 지갑 정보
        PointWallet pointWallet = new PointWallet(memberId, 2000L);

        // 적립 정보
        PointEarn pointEarn = new PointEarn(earnId, memberId, 1000L, PointEarn.EarnType.EVENT);
        pointEarn.setRemainingAmount(0L);
        pointEarn.setExpiredAt(LocalDate.now().plusDays(10));

        // 사용 정보
        PointUse pointUse = new PointUse(useId, memberId, "ORDER-001", 1000L, 0L, PointUse.UseStatus.USED);
        PointUseDetail pointUseDetail = new PointUseDetail(useId, earnId, 1000L, 0L);

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)).thenReturn(Optional.of(pointUse));
        when(pointUseDetailRepository.findAllByUseIdIs(useId)).thenReturn(new ArrayList<>(List.of(pointUseDetail)));
        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));

        // when
        pointUseService.cancelUsePoint(request);

        // then
        assertThat(pointEarn.getRemainingAmount()).isEqualTo(1000L);
        assertThat(pointUseDetail.getCancelAmount()).isEqualTo(1000L);
        assertThat(pointUse.getCanceledAmount()).isEqualTo(1000L);
        assertThat(pointUse.getStatus()).isEqualTo(PointUse.UseStatus.CANCELED); // 기존 사용 금액의 전체가 취소되었으므로 전체 취소 상태
        assertThat(pointWallet.getBalance()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("포인트 부분 사용취소 성공")
    void cancelUsePoint_success_partial() {
        // given
        String memberId = "member1";
        String useId = "use1";
        String earnId = "earn1";

        PointUseDto.CancelRequest request = new PointUseDto.CancelRequest();
        request.setMemberId(memberId);
        request.setUseId(useId);
        request.setAmount(500L);

        // 회원 지갑 정보
        PointWallet pointWallet = new PointWallet(memberId, 2000L);

        // 적립 정보
        PointEarn pointEarn = new PointEarn(earnId, memberId, 1000L, PointEarn.EarnType.EVENT);
        pointEarn.setRemainingAmount(0L);
        pointEarn.setExpiredAt(LocalDate.now().plusDays(10));

        // 사용 정보
        PointUse pointUse = new PointUse(useId, memberId, "ORDER-001", 1000L, 0L, PointUse.UseStatus.USED);
        PointUseDetail pointUseDetail = new PointUseDetail(useId, earnId, 1000L, 0L);

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)).thenReturn(Optional.of(pointUse));
        when(pointUseDetailRepository.findAllByUseIdIs(useId)).thenReturn(new ArrayList<>(List.of(pointUseDetail)));
        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));

        // when
        pointUseService.cancelUsePoint(request);

        // then
        assertThat(pointEarn.getRemainingAmount()).isEqualTo(500L);
        assertThat(pointUseDetail.getCancelAmount()).isEqualTo(500L);
        assertThat(pointUse.getCanceledAmount()).isEqualTo(500L);
        assertThat(pointUse.getStatus()).isEqualTo(PointUse.UseStatus.PARTIAL_CANCELED); // 기존 사용 금액의 일부만 취소되었으므로 부분 취소 상태
        assertThat(pointWallet.getBalance()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("이미 만료된 사용취소 시 신규 적립")
    void cancelUsePoint_success_expiredEarnPoint() {
        // given
        String memberId = "member1";
        String useId = "use1";
        String earnId = "earn1";

        PointUseDto.CancelRequest request = new PointUseDto.CancelRequest();
        request.setMemberId(memberId);
        request.setUseId(useId);
        request.setAmount(1000L);

        // 회원 지갑 정보
        PointWallet pointWallet = new PointWallet(memberId, 2000L);

        // 적립 정보
        PointEarn pointEarn = new PointEarn(earnId, memberId, 1000L, PointEarn.EarnType.EVENT);
        pointEarn.setRemainingAmount(0L);
        pointEarn.setExpiredAt(LocalDate.now().minusDays(1)); // 만료된 적립 정보

        // 사용 정보
        PointUse pointUse = new PointUse(useId, memberId, "ORDER-001", 1000L, 0L, PointUse.UseStatus.USED);
        PointUseDetail pointUseDetail = new PointUseDetail(useId, earnId, 1000L, 0L);

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)).thenReturn(Optional.of(pointUse));
        when(pointUseDetailRepository.findAllByUseIdIs(useId)).thenReturn(new ArrayList<>(List.of(pointUseDetail)));
        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));

        // when
        pointUseService.cancelUsePoint(request);

        // then
        assertThat(pointEarn.getRemainingAmount()).isEqualTo(0L); // 기존 earn 상태 체크 (잔액이 복구되면 안됨)
        verify(pointEarnRepository).saveAll(anyList()); // 신규 적립 발생 체크

        assertThat(pointUseDetail.getCancelAmount()).isEqualTo(1000L);
        assertThat(pointUse.getCanceledAmount()).isEqualTo(1000L);
        assertThat(pointUse.getStatus()).isEqualTo(PointUse.UseStatus.CANCELED);
        assertThat(pointWallet.getBalance()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("사용 내역이 없는 포인트이면 취소 실패")
    void cancelUsePoint_fail_notExist() {
        // given
        String memberId = "member1";
        String useId = "use1";

        PointUseDto.CancelRequest request = new PointUseDto.CancelRequest();
        request.setMemberId(memberId);
        request.setUseId(useId);
        request.setAmount(1000L);

        // 회원 지갑 정보
        PointWallet pointWallet = new PointWallet(memberId, 2000L);

        // 사용 정보
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointUseService.cancelUsePoint(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("포인트 사용 이력이 없습니다.");
    }

    @Test
    @DisplayName("이미 취소된 사용 포인트이면 취소 실패")
    void cancelUsePoint_fail_alreadyCancel() {
        // given
        String memberId = "member1";
        String useId = "use1";

        PointUseDto.CancelRequest request = new PointUseDto.CancelRequest();
        request.setMemberId(memberId);
        request.setUseId(useId);
        request.setAmount(1000L);

        // 회원 지갑 정보
        PointWallet pointWallet = new PointWallet(memberId, 2000L);

        // 사용 정보
        PointUse pointUse = new PointUse(useId, memberId, "ORDER-001", 1000L, 0L, PointUse.UseStatus.CANCELED); // 취소상태

        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(pointWallet);
        when(pointUseRepository.findPointUseByUseIdAndMemberId(useId, memberId)).thenReturn(Optional.of(pointUse));

        // when & then
        assertThatThrownBy(() -> pointUseService.cancelUsePoint(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 취소된 포인트 입니다.");
    }
}