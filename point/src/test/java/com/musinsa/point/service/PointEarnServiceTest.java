package com.musinsa.point.service;

import com.musinsa.point.domain.PointEarn;
import com.musinsa.point.domain.PointPolicy;
import com.musinsa.point.domain.PointWallet;
import com.musinsa.point.dto.PointEarnDto;
import com.musinsa.point.exception.BusinessException;
import com.musinsa.point.repository.PointEarnRepository;
import com.musinsa.point.repository.PointWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointEarnServiceTest {

    @InjectMocks
    private PointEarnService pointEarnService;

    @Mock
    private PointPolicyService pointPolicyService;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PointEarnRepository pointEarnRepository;

    @Test
    @DisplayName("포인트 적립 성공")
    void earnPoint_success() {
        // given
        String memberId = "user1";
        Long amount = 1000L;

        PointEarnDto.EarnRequest request = new PointEarnDto.EarnRequest(memberId, amount, PointEarn.EarnType.EVENT, null);

        PointPolicy pointPolicy = new PointPolicy();
        pointPolicy.setPolicyId(1L);
        pointPolicy.setMaxEarnAmount(100000L);
        pointPolicy.setMaxBalanceAmount(1000000L);

        when(pointPolicyService.getPolicy(memberId)).thenReturn(pointPolicy);
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(new PointWallet(memberId, 0L));

        // when
        PointEarnDto.EarnResponse response = pointEarnService.earnPoint(request);

        // then
        assertThat(response.getEarnId()).isNotBlank();
    }

    @Test
    @DisplayName("적립 가능 금액이 아니면 실패")
    void earnPoint_fail_invalidAmount() {
        // given
        String memberId = "user1";

        PointEarnDto.EarnRequest request = new PointEarnDto.EarnRequest(memberId, 100001L, PointEarn.EarnType.EVENT, null);

        PointPolicy pointPolicy = new PointPolicy();
        pointPolicy.setPolicyId(1L);
        pointPolicy.setMaxEarnAmount(100000L);
        pointPolicy.setMaxBalanceAmount(1000000L);

        when(pointPolicyService.getPolicy(memberId)).thenReturn(pointPolicy);

        // when & then
        assertThatThrownBy(() -> pointEarnService.earnPoint(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("최대 보유 포인트를 초과하면 실패")
    void earnPoint_fail_exceedMaxBalance() {
        // given
        String memberId = "user1";

        PointEarnDto.EarnRequest request = new PointEarnDto.EarnRequest(memberId, 1000L, PointEarn.EarnType.EVENT, null);

        PointPolicy pointPolicy = new PointPolicy();
        pointPolicy.setPolicyId(1L);
        pointPolicy.setMaxEarnAmount(10000L);
        pointPolicy.setMaxBalanceAmount(10000L);

        PointWallet wallet = new PointWallet(memberId, 9500L);

        when(pointPolicyService.getPolicy(memberId)).thenReturn(pointPolicy);
        when(pointWalletRepository.findPointWalletByMemberId(memberId))
                .thenReturn(wallet);

        // when & then
        assertThatThrownBy(() -> pointEarnService.earnPoint(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("포인트 적립 취소 성공")
    void cancelEarnPoint_success() {
        // given
        String memberId = "user1";
        String earnId = "earn1";
        Long amount = 1000L;

        PointEarnDto.CancelRequest request = new PointEarnDto.CancelRequest();
        request.setEarnId(earnId);
        request.setMemberId(memberId);
        request.setAmount(amount);

        PointEarn pointEarn = new PointEarn(earnId, memberId, amount, PointEarn.EarnType.EVENT);
        PointWallet wallet = new PointWallet(memberId, 1000L);

        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(wallet);

        // when
        pointEarnService.cancelEarnPoint(request);

        // then
        assertThat(pointEarn.getStatus()).isEqualTo(PointEarn.EarnStatus.CANCELED);
        assertThat(wallet.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 적립 내역이 없으면 취소 실패")
    void cancelEarnPoint_fail_notExist() {
        // given
        String memberId = "user1";
        String earnId = "earn2";
        Long amount = 1000L;

        PointEarnDto.CancelRequest request = new PointEarnDto.CancelRequest();
        request.setEarnId(earnId);
        request.setMemberId(memberId);
        request.setAmount(amount);

        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointEarnService.cancelEarnPoint(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("적립된 포인트가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("이미 취소된 적립 포인트이면 취소 실패")
    void cancelEarnPoint_fail_alreadyCancel() {
        // given
        String memberId = "user1";
        String earnId = "earn1";
        Long amount = 1000L;

        PointEarnDto.CancelRequest request = new PointEarnDto.CancelRequest();
        request.setEarnId(earnId);
        request.setMemberId(memberId);
        request.setAmount(amount);

        PointEarn pointEarn = new PointEarn(earnId, memberId, amount, PointEarn.EarnType.EVENT);
        PointWallet wallet = new PointWallet(memberId, 1000L);

        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));
        when(pointWalletRepository.findPointWalletByMemberId(memberId)).thenReturn(wallet);

        // 적립 취소 요청
        pointEarnService.cancelEarnPoint(request);

        // when & then
        // 동일한 내역으로 적립 취소 재 요청
        assertThatThrownBy(() -> pointEarnService.cancelEarnPoint(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 취소된 적립 포인트 입니다.");
    }

    @Test
    @DisplayName("이미 사용된 적립 포인트이면 취소 실패")
    void cancelEarnPoint_fail_alreadyUse() {
        // given
        String memberId = "user1";
        String earnId = "earn1";
        Long amount = 1000L;
        Long remainingAmount = 500L;

        PointEarnDto.CancelRequest request = new PointEarnDto.CancelRequest();
        request.setEarnId(earnId);
        request.setMemberId(memberId);
        request.setAmount(amount);

        PointEarn pointEarn = new PointEarn(earnId, memberId, amount, PointEarn.EarnType.EVENT);
        PointWallet wallet = new PointWallet(memberId, 1000L);

        // 적립 포인트 사용 처리
        pointEarn.setRemainingAmount(remainingAmount);

        when(pointEarnRepository.findPointEarnByEarnIdAndMemberId(earnId, memberId)).thenReturn(Optional.of(pointEarn));

        // when & then
        assertThatThrownBy(() -> pointEarnService.cancelEarnPoint(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("일부 사용된 적립 포인트 이므로, 취소가 불가능 합니다.");
    }
}