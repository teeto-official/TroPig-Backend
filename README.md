# Teeto Backend

Teeto 서비스의 백엔드 API 서버입니다.
Kotlin + Spring Boot 기반으로 구현되었으며, PostgreSQL / Redis / AWS S3를 사용합니다.

## 기술 스택

- **Language**: Kotlin 1.9
- **Framework**: Spring Boot 3.4
- **Database**: PostgreSQL, Redis
- **Storage**: AWS S3
- **Payment**: PortOne
- **Auth**: JWT

## 로컬 실행 방법

### 1. 환경변수 설정

`application-local.yml`에서 로컬 환경변수를 설정합니다. 필요한 키들은 `application-example.yml`을 참고해주세요.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트: `8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 코드 스타일 (ktlint)

이 프로젝트는 [ktlint](https://github.com/pinterest/ktlint)를 사용해 코드 스타일을 관리합니다.

### pre-commit 훅 설정 (권장)

커밋 전 자동으로 lint 검사가 실행되도록 설정하는 것을 권장합니다.

```bash
./gradlew addKtlintCheckGitPreCommitHook
```

### 수동 실행

```bash
# 검사
./gradlew ktlintCheck

# 자동 수정
./gradlew ktlintFormat
```
