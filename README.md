# 무신사 서버 개발자 사전과제

## 포인트 시스템

---

## 1. 요구사항 분석

* 회원의 포인트 적립, 적립 취소, 사용, 사용 취소 기능을 구현합니다.
* 포인트 적립 시 1회 적립 가능 금액과 회원별 보유 가능 포인트 한도를 정책으로 관리합니다.
* 포인트 사용 시 수기 지급 포인트를 우선 차감하고, 이후 만료일이 빠른 포인트부터 차감합니다.
* 포인트 사용 시 주문번호를 함께 저장하여 어떤 주문에서 얼마의 포인트가 사용되었는지 추적할 수 있도록 합니다.
* 사용한 포인트는 전체 또는 일부 사용 취소할 수 있습니다.
* 사용 취소 시 기존 적립 포인트가 만료된 경우 신규 적립 포인트로 복구합니다.

---

## 2. 기술 스택

* Java 21
* Spring Boot 3.5.14
* Spring Data JPA
* H2 Database
* JUnit5
* 
---

## 3. 시스템 설계

### 3.1 설계 개요

* 회원 포인트 지갑, 포인트 적립 원장, 포인트 사용 원장, 포인트 사용 상세, 포인트 정책을 도메인으로 분리하였습니다.
* `PointPolicy`는 1회 최대 적립 가능 금액과 회원별 최대 보유 가능 금액을 관리합니다.
* 정책은 회원별 정책이 존재하면 해당 정책을 우선 적용하고, 없으면 기본 정책을 적용하도록 설계하였습니다.
* `PointWallet`은 회원의 현재 포인트 잔액을 관리합니다.
* `PointEarn`은 적립 단위별 포인트 금액, 잔여 금액, 적립 유형, 만료일을 관리합니다.
* `PointUse`는 주문 단위의 포인트 사용 금액과 취소 금액, 사용 상태를 관리합니다.
* `PointUseDetail`은 하나의 포인트 사용이 어떤 적립 원장에서 얼마씩 차감되었는지 추적합니다.

### 3.2 핵심 도메인

* **PointPolicy** : 포인트 정책 관리
* **PointWallet** : 회원 포인트 잔액 관리
* **PointEarn** : 포인트 적립 원장
* **PointUse** : 포인트 사용 원장
* **PointUseDetail** : 적립 단위별 사용 상세

---

### 3.3 테이블 구조

```
[TB_POINT_POLICY]
- POLICY_ID (PK)
- MEMBER_ID
- MAX_EARN_AMOUNT
- MAX_BALANCE_AMOUNT

[TB_POINT_WALLET]
- MEMBER_ID (PK)
- BALANCE

[TB_POINT_EARN]
- EARN_ID (PK)
- MEMBER_ID
- AMOUNT
- REMAINING_AMOUNT
- EARN_TYPE (MANUAL / EVENT / USE_CANCEL)
- STATUS (EARNED / CANCELED)
- EXPIRED_AT
- REGISTERED_AT

[TB_POINT_USE]
- USE_ID (PK)
- MEMBER_ID
- ORDER_NUMBER
- USE_AMOUNT
- CANCEL_AMOUNT
- STATUS (USED / PARTIAL_CANCELED / CANCELED)
- REGISTERED_AT

[TB_POINT_USE_DETAIL]
- DETAIL_ID (PK)
- USE_ID
- EARN_ID
- USE_AMOUNT
- CANCEL_AMOUNT
```

---

## 4. API 명세

### 4.1 API 목록

| Method | URI                      | 설명        |
| ------ | ------------------------ | --------- |
| POST   | `/api/point/earn`        | 포인트 적립    |
| POST   | `/api/point/earn/cancel` | 포인트 적립 취소 |
| POST   | `/api/point/use`         | 포인트 사용    |
| POST   | `/api/point/use/cancel`  | 포인트 사용 취소 |

---

### 4.2 API 상세

#### 1) 포인트 적립

```
POST /api/point/earn
```

Request:

```json
{
  "memberId": "member1",
  "amount": 1000,
  "earnType": "EVENT",
  "expiredAt": "2026-05-01"
}
```

Response:

```json
{
  "earnId": "생성된 적립 ID"
}
```

---

#### 2) 포인트 적립 취소

```
POST /api/point/earn/cancel
```

Request:

```json
{
  "earnId": "earn1",
  "memberId": "member1",
  "amount": 1000
}
```

Response:

```
HTTP 200 OK
```

---

#### 3) 포인트 사용

```
POST /api/point/use
```

Request:

```json
{
  "memberId": "member1",
  "orderNumber": "ORDER-001",
  "amount": 1000
}
```

Response:

```json
{
  "useId": "생성된 사용 ID"
}
```

---

#### 4) 포인트 사용 취소

```
POST /api/point/use/cancel
```

Request:

```json
{
  "useId": "use1",
  "memberId": "member1",
  "amount": 500
}
```

Response:

```
HTTP 200 OK
```

---

## 5. 주요 비즈니스 규칙

### 포인트 적립

* 1포인트 이상만 적립 가능
* 정책(`PointPolicy`) 기준 최대 적립 금액 검증
* 최대 보유 포인트 초과 시 실패
* 기본 만료일은 365일
* 만료일 직접 지정 시 1일 이상 ~ 5년 미만

---

### 포인트 적립 취소

* 일부라도 사용된 적립 건은 취소 불가
* 이미 취소된 적립 건 재취소 불가
* 취소 시 지갑 잔액 차감

---

### 포인트 사용

* 주문번호 기반 사용
* 보유 포인트 부족 시 실패
* 차감 우선순위

  1. 수기 지급 (MANUAL)
  2. 만료일 빠른 순
  3. 등록일 빠른 순
* 사용 내역은 상세 단위로 기록

---

### 포인트 사용 취소

* 전체/부분 취소 가능
* 취소 가능 금액 검증
* 만료 안된 포인트 → 원복
* 만료된 포인트 → 신규 적립 생성 (USE_CANCEL)
* 상태 변경

  * 전체 취소 → CANCELED
  * 부분 취소 → PARTIAL_CANCELED

---

## 6. 테스트

### 단위 테스트

* PointEarnServiceTest
* PointUseServiceTest

검증 항목:

* 포인트 적립/취소
* 포인트 사용/취소
* 정책 검증
* 만료일 기준 차감 로직
* 부분 취소 / 전체 취소

---

## 7. 실행 방법

### 실행

```bash
cd point
./gradlew bootRun
```

### 테스트

```bash
cd point
./gradlew test
```

---

## 8. H2 Console

```
http://localhost:8080/h2-console
```

```
JDBC URL: jdbc:h2:file:./data/pointdb
User: sa
Password:
```

---

## 9. 주요 고려사항

* 포인트를 적립 단위로 관리하여 1원 단위 추적 가능하도록 설계
* 정책 분리로 하드코딩 제거
* 취소 시 만료 여부에 따른 분기 처리
* 부분 취소를 고려한 데이터 모델 설계

```
```
