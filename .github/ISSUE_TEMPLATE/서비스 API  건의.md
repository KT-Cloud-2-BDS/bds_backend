---
name: "서비스 API 건의"
about: 서비스 API 건의
title: "[서비스 API 건의] : "
labels: docs
---

### 1. 제안 배경 및 필요성 (Why)
- **현황 및 목적:** 주문 시작 전, 사용자의 **계정 생성일(가입일)**에 따라 주문 가능한 특수 상품군이 달라지는 비즈니스 정책이 추가되었습니다.
- **요청 사항:** 주문 생성 로직 진입 시 해당 유저가 권한이 있는지 검증하기 위해, 주문 서비스에서 특정 유저의 상세 정보(특히 계정 생성일)를 동기식으로 조회할 수 있는 API 제공을 건의합니다.

### 2. API 명세 구상안 (What)
- **희망 통신 방식:** 
- **희망 HTTP Method & URL:** `GET /api/v1/users/{userId}` 또는 `GET /api/v1/users/?userId=1`

#### 🔹 Request (요청)
- **Query Parameter / Path Variable:**

| **Parameter** | **Type** | **설명** | **필수 여부** |
| --- | --- | --- | --- |
| `userId` | Long | 조회를 원하는 유저의 고유 식별 ID | 필수 |


#### 🔹 Response (응답 희망안)
```
{
  "status": "SUCCESS",
  "message": "유저 정보 조회가 완료되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "createdAt": "2025-02-27T00:00:00Z"
  }
}
```

#### 🔹 Response 데이터 필드 상세

| **Field** | **Type** | **요청 사유 및 비고** |
| --- | --- | --- |
| `userId` | Long | 요청한 유저 ID 일치 여부 검증용 |
| `email` | String | (선택) 데이터 정합성 확인용 유저 이메일 |
| `createdAt` | String (ISO 8601) | **[필수]** 주문 가능 상품 필터링 로직의 핵심 기준 데이터 |
