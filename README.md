# 무신사 서버 개발자 사전과제
## 포인트 시스템

### 1. 요구사항 분석
- 회원의 포인트 적립, 적립 취소, 사용, 사용 취소 기능을 구현합니다.
- 포인트 적립 시 1회 적립 가능 금액과 회원별 보유 가능 포인트 한도를 정책으로 관리합니다.
- 포인트 사용 시 수기 지급 포인트를 우선 차감하고, 이후 만료일이 빠른 포인트부터 차감합니다.
- 포인트 사용 시 주문번호를 함께 저장하여 어떤 주문에서 얼마의 포인트가 사용되었는지 추적할 수 있도록 합니다.
- 사용한 포인트는 전체 또는 일부 사용 취소할 수 있습니다.
- 사용 취소 시 기존 적립 포인트가 만료된 경우 신규 적립 포인트로 복구합니다.

### 2. 기술 스택
- Java 21
- Spring Boot 3.5.14
- Spring Data JPA
- H2 Database
- JUnit5
- Lombok

### 3. 시스템 설계
- 회원 포인트 지갑, 포인트 적립 원장, 포인트 사용 원장, 포인트 사용 상세, 포인트 정책을 도메인으로 분리하였습니다.
- `PointWallet`은 회원의 현재 포인트 잔액을 관리합니다.
- `PointEarn`은 적립 단위별 포인트 금액, 잔여 금액, 적립 유형, 만료일을 관리합니다.
- `PointUse`는 주문 단위의 포인트 사용 금액과 취소 금액, 사용 상태를 관리합니다.
- `PointUseDetail`은 하나의 포인트 사용이 어떤 적립 원장에서 얼마씩 차감되었는지 추적합니다.
- `PointPolicy`는 1회 최대 적립 가능 금액과 회원별 최대 보유 가능 금액을 관리합니다.
- 정책은 회원별 정책이 존재하면 해당 정책을 우선 적용하고, 없으면 기본 정책을 적용하도록 설계하였습니다.

#### 3.1 핵심 도메인 정의
- `PointWallet` : 회원 포인트 지갑
- `PointEarn` : 포인트 적립 원장
- `PointUse` : 포인트 사용 원장
- `PointUseDetail` : 포인트 사용 상세
- `PointPolicy` : 포인트 정책

#### 3.2 테이블 구조

```text
[TB_POINT_WALLET]
- MEMBER_ID (PK) : 회원 ID
- BALANCE : 현재 보유 포인트

[TB_POINT_EARN]
- EARN_ID (PK) : 적립 ID
- MEMBER_ID : 회원 ID
- AMOUNT : 적립 금액
- EARN_TYPE : 적립 유형 (MANUAL / EVENT / USE_CANCEL)
- STATUS : 적립 상태 (EARNED / CANCELED)
- REMAINING_AMOUNT : 잔여 포인트
- EXPIRED_AT : 만료일
- REGISTERED_AT : 등록일

[TB_POINT_USE]
- USE_ID (PK) : 사용 ID
- MEMBER_ID : 회원 ID
- ORDER_NUMBER : 주문번호
- USE_AMOUNT : 사용 포인트
- CANCEL_AMOUNT : 취소된 포인트
- STATUS : 사용 상태 (USED / PARTIAL_CANCELED / CANCELED)
- REGISTERED_AT : 등록일

[TB_POINT_USE_DETAIL]
- DETAIL_ID (PK) : 사용 상세 ID
- USE_ID : 사용 ID
- EARN_ID : 적립 ID
- USE_AMOUNT : 해당 적립 건에서 사용한 포인트
- CANCEL_AMOUNT : 해당 상세 건에서 취소된 포인트

[TB_POINT_POLICY]
- POLICY_ID (PK) : 정책 ID
- MEMBER_ID : 회원 ID
- MAX_EARN_AMOUNT : 1회 최대 적립 가능 포인트
- MAX_BALANCE_AMOUNT : 최대 보유 가능 포인트
