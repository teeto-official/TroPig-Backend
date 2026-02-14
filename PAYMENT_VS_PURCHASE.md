# Payment와 Purchase의 차이점

## 개요

`Payment`와 `Purchase`는 결제 시스템에서 서로 다른 목적을 가진 엔티티입니다.

## 1. Payment (결제 거래)

### 목적
- **결제 시스템과의 거래 정보 관리**
- 포트원(PortOne) 결제 API와의 거래 기록

### 역할
- 외부 결제 시스템(포트원)과의 거래 추적
- 결제 수단, 결제 상태, 포트원 응답 등 결제 거래 자체의 정보 관리

### 주요 필드
- `portonePaymentId`: 포트원 결제 ID (고유값)
- `amount`: 결제 금액
- `status`: 결제 상태 (`PaymentStatus`: PENDING, PAID, FAILED 등)
- `method`: 결제 수단 (카드, 계좌이체 등)
- `portoneResponse`: 포트원 API 응답 전체 (JSON)
- `failureReason`: 실패 사유
- `currency`: 통화 (기본값: KRW)
- `channelKey`: 채널 키
- `storeId`: 스토어 ID

### 사용 예시
```kotlin
// 결제 거래 생성
val payment = Payment(
    memberId = memberId,
    portonePaymentId = paymentId,
    amount = content.price.toLong(),
    status = PaymentStatus.PENDING,
    currency = "KRW",
    channelKey = request.channel.channelKey,
    storeId = storeId
)
```

## 2. Purchase (작품 구매)

### 목적
- **특정 작품(Content)에 대한 구매 정보 관리**
- 비즈니스 로직상의 구매 기록

### 역할
- 어떤 작품을 구매했는지 추적
- 구매 상태 관리 (대기, 완료, 취소, 환불)
- 작품 접근 권한 확인에 사용

### 주요 필드
- `contentId`: 구매한 작품 ID
- `paymentId`: 결제 거래 참조 (Payment 엔티티의 ID)
- `amount`: 구매 금액
- `status`: 구매 상태 (`PurchaseStatus`: PENDING, COMPLETED, CANCELLED, REFUNDED)
- `memberId`: 구매한 회원 ID

### 사용 예시
```kotlin
// 작품 구매 기록 생성
val purchase = Purchase(
    memberId = memberId,
    contentId = request.contentId,
    paymentId = savedPayment.id,  // Payment 참조
    amount = content.price.toLong(),
    status = PurchaseStatus.PENDING
)
```

## 관계

```
Payment (1) ────────< (N) Purchase
```

- **Purchase는 Payment를 참조합니다** (`paymentId` 필드)
- 하나의 Payment는 여러 Purchase를 가질 수 있습니다 (현재 코드에서는 1:1 관계로 사용)
- Purchase는 "어떤 작품을 구매했는지"를 나타내고, Payment는 "어떻게 결제했는지"를 나타냅니다

## Repository 차이

### PaymentRepository
- **용도**: 결제 거래 정보 조회
- **주요 메서드**:
  - `findByPortonePaymentId()`: 포트원 결제 ID로 조회
  - `findByMemberIdAndStatus()`: 회원의 결제 상태별 조회
  - `findByMemberIdOrderByCreatedAtDesc()`: 회원의 결제 내역 조회

### PurchaseRepository
- **용도**: 작품 구매 정보 조회
- **주요 메서드**:
  - `findByMemberIdAndContentId()`: 특정 작품 구매 여부 확인
  - `existsByMemberIdAndContentIdAndStatus()`: 구매 상태별 존재 여부 확인
  - `findByMemberIdOrderByCreatedAtDesc()`: 회원의 구매 내역 조회
  - `findByMemberIdAndPaymentId()`: 결제 ID로 구매 내역 조회

## 실제 사용 시나리오

### 시나리오 1: 작품 구매하기
1. **Payment 생성**: 포트원 결제 거래 시작 (PENDING 상태)
2. **Purchase 생성**: 작품 구매 기록 생성 (PENDING 상태, Payment 참조)
3. **결제 승인 후**: 
   - Payment 상태 → PAID
   - Purchase 상태 → COMPLETED

### 시나리오 2: 작품 접근 권한 확인
```kotlin
// PaymentContentService에서 사용
fun isContentPurchased(memberId: Long, contentId: Long): Boolean {
    return purchaseRepository.existsByMemberIdAndContentIdAndStatus(
        memberId = memberId,
        contentId = contentId,
        status = PurchaseStatus.COMPLETED
    )
}
```
- **PurchaseRepository 사용**: 어떤 작품을 구매했는지 확인
- PaymentRepository는 사용하지 않음 (결제 거래 자체가 아닌 구매 여부만 확인)

### 시나리오 3: 결제 내역 조회
```kotlin
// PaymentService에서 사용
fun getPurchase(memberId: Long, purchaseId: Long): PurchaseResponse {
    val purchase = purchaseRepository.findById(purchaseId)  // Purchase 조회
    val payment = paymentRepository.findById(purchase.paymentId)  // Payment 조회
    // 두 정보를 조합하여 응답
}
```
- **PurchaseRepository**: 구매한 작품 정보
- **PaymentRepository**: 결제 거래 상세 정보

## 결론

| 구분 | Payment | Purchase |
|------|---------|----------|
| **목적** | 결제 거래 정보 | 작품 구매 정보 |
| **관심사** | "어떻게 결제했는가" | "무엇을 구매했는가" |
| **외부 시스템** | 포트원 결제 API | 내부 비즈니스 로직 |
| **주요 사용처** | 결제 승인, 결제 내역 조회 | 작품 접근 권한, 구매 내역 조회 |
| **Repository** | PaymentRepository | PurchaseRepository |

**요약**: 
- `Payment`는 결제 시스템과의 거래를 추적하는 엔티티
- `Purchase`는 비즈니스 로직상의 작품 구매를 추적하는 엔티티
- 작품 구매 여부를 확인할 때는 `PurchaseRepository`를 사용합니다
