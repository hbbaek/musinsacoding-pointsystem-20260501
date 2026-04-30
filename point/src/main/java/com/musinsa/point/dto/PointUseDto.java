package com.musinsa.point.dto;

import com.musinsa.point.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

public class PointUseDto {

    @Data
    public static class UseRequest {
        private String memberId;
        private Long amount;
        private String orderNumber;

        public void validate() {
            if(memberId == null || memberId.isBlank() || amount < 0
                    || orderNumber == null || orderNumber.isBlank())
                throw new BusinessException(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다.");
        }
    }

    @Data
    @AllArgsConstructor
    public static class UseResponse {
        private String useId;
    }

    @Data
    public static class CancelRequest {
        private String useId;
        private String memberId;
        private Long amount;

        public void validate() {
            if(useId == null || useId.isBlank() || memberId == null || memberId.isBlank() || amount < 0)
                throw new BusinessException(HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다.");
        }
    }
}
