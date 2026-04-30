package com.musinsa.point.dto;

import com.musinsa.point.domain.PointEarn;
import com.musinsa.point.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class PointEarnDto {

    @Data
    @AllArgsConstructor
    public static class EarnRequest {
        private String memberId;
        private Long amount;
        private PointEarn.EarnType earnType;
        private LocalDate expiredAt;

        public void validate() {
            if(memberId == null || memberId.isBlank() || amount < 0 || earnType == null)
                throw new BusinessException(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다.");

            if(expiredAt != null) {
                if (!expiredAt.isAfter(LocalDate.now())) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "만료일은 최소 1일 이상이어야 합니다.");
                }

                if (!expiredAt.isBefore(LocalDate.now().plusYears(5))) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "만료일은 최대 5년 미만이어야 합니다.");
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class EarnResponse {
        private String earnId;
    }

    @Data
    public static class CancelRequest {
        private String earnId;
        private String memberId;
        private Long amount;

        public void validate() {
            if(earnId == null || earnId.isBlank() || memberId == null || memberId.isBlank() || amount < 0)
                throw new BusinessException(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다.");
        }
    }
}
