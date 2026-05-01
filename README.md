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

* 적립 금액은 1포인트 이상이어야 합니다.
* 1회 최대 적립 가능 금액은 PointPolicy 기준으로 검증합니다.
* 적립 후 회원의 총 보유 포인트가 최대 보유 가능 포인트를 초과하면 실패 처리합니다.
* 만료일이 없는 경우 기본 만료일은 365일로 설정합니다.
* 만료일을 직접 지정하는 경우 최소 1일 이상, 최대 5년 미만이어야 합니다.

---

### 포인트 적립 취소

* 적립한 금액 중 일부가 이미 사용된 경우 적립 취소가 불가능합니다.
* 이미 취소된 적립 건은 다시 취소할 수 없습니다.
* 적립 취소 시 회원 지갑 잔액을 차감하고 적립 상태를 `CANCELED`로 변경합니다.

---

### 포인트 사용

* 주문 시에만 포인트를 사용할 수 있다고 가정하고 주문번호를 필수로 저장합니다.  
* 회원 지갑 잔액이 사용 요청 금액보다 적으면 실패 처리합니다.  
* 사용 가능한 적립 원장이 없는 경우 실패 처리합니다.  
* 포인트 차감 우선순위는 다음과 같습니다.  
  * 수기 지급 포인트 (`MANUAL`)  
  * 만료일이 빠른 포인트  
  * 등록일이 빠른 포인트  
* 포인트 사용 시 `PointUseDetail`에 어떤 적립 포인트에서 얼마가 차감되었는지 저장합니다.  

---

### 포인트 사용 취소

* 사용한 포인트는 전체 또는 일부 취소할 수 있습니다.  
* 사용 상세 기준으로 취소 가능한 금액을 계산하여 요청 금액만큼 복구합니다.  
* 기존 적립 포인트가 만료되지 않은 경우 기존 적립 원장의 잔여 금액을 복구합니다.  
* 기존 적립 포인트가 만료된 경우 `USE_CANCEL` 유형의 신규 적립 포인트를 생성합니다.  
* 전체 취소 시 상태를 `CANCELED`, 일부 취소 시 상태를 `PARTIAL_CANCELED`로 변경합니다.
* 
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


```
```
