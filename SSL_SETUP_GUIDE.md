# SSL/HTTPS 설정 가이드 (Let's Encrypt)

## 현재 상태

- ❌ HTTPS(443) 없음
- ✅ HTTP(80)만 동작

## SSL 인증서 발급 절차

### 1. 사전 준비

#### A. Lightsail 방화벽 설정

```
Lightsail 콘솔
→ 네트워킹 탭
→ 방화벽 규칙 추가:
  - HTTP: 80
  - HTTPS: 443
```

#### B. DNS 설정 확인

```bash
# dev.teeto.to가 Lightsail IP를 가리키는지 확인
nslookup dev.teeto.to

# 또는
dig dev.teeto.to
```

### 2. 코드 커밋 & 배포

```bash
# 로컬에서
git add docker-compose.yml
git add nginx/conf.d/default.conf
git add nginx/conf.d/ssl.conf.example
git add SSL_SETUP_GUIDE.md

git commit -m "Add SSL/HTTPS support with Let's Encrypt"
git push origin develop
```

### 3. 서버에서 SSL 인증서 발급

```bash
# Lightsail SSH 접속
ssh -i ~/.ssh/lightsail-key.pem ubuntu@your-lightsail-ip

cd /opt/dev

# 최신 코드 받기
git pull origin develop

# docker-compose.yml 수정 (이메일 변경)
vim docker-compose.yml
# certbot 컨테이너의 email을 실제 이메일로 변경:
# --email your-email@example.com → --email real@example.com

# 컨테이너 재시작
docker compose down
docker compose up -d

# certbot 로그 확인
docker logs certbot

# 인증서 발급 확인
sudo ls -la /var/lib/docker/volumes/dev_certbot-etc/_data/live/dev.teeto.to/
```

### 4. HTTPS 설정 활성화

```bash
cd /opt/dev

# SSL 설정 파일 활성화
cp nginx/conf.d/ssl.conf.example nginx/conf.d/ssl.conf

# default.conf 비활성화 (HTTP 리다이렉트는 ssl.conf에 있음)
mv nginx/conf.d/default.conf nginx/conf.d/default.conf.disabled

# nginx 설정 테스트
docker exec nginx nginx -t

# nginx reload
docker exec nginx nginx -s reload

# 또는 재시작
docker compose restart nginx
```

### 5. 테스트

```bash
# HTTPS 접속 테스트
curl -v https://dev.teeto.to/api/health

# 브라우저에서
https://dev.teeto.to/swagger-ui.html
```

## 자동 갱신 설정

Let's Encrypt 인증서는 **90일**마다 갱신이 필요합니다.

### 방법 1: Cron Job

```bash
# Lightsail 서버에서
crontab -e

# 매일 새벽 2시에 갱신 시도 (갱신 필요시에만 실행됨)
0 2 * * * cd /opt/dev && docker compose run --rm certbot renew && docker exec nginx nginx -s reload
```

### 방법 2: docker-compose.yml에 갱신 컨테이너 추가

```yaml
  certbot-renew:
    image: certbot/certbot
    volumes:
      - certbot-etc:/etc/letsencrypt
      - certbot-var:/var/lib/letsencrypt
      - web-root:/var/www/html
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"
```

## 문제 해결

### 1. certbot 실패: "Failed authorization procedure"

**원인:** DNS가 올바르게 설정되지 않음

**해결:**
```bash
# DNS 확인
nslookup dev.teeto.to

# Lightsail IP와 일치하는지 확인
curl ifconfig.me
```

### 2. certbot 실패: "Timeout during connect"

**원인:** 방화벽에서 80 포트가 닫혀있음

**해결:**
- Lightsail 콘솔에서 80 포트 열기
- `docker ps`로 nginx 컨테이너 확인

### 3. nginx 설정 에러: "ssl_certificate" directive

**원인:** SSL 인증서가 아직 발급되지 않음

**해결:**
```bash
# 인증서 확인
docker exec nginx ls -la /etc/letsencrypt/live/dev.teeto.to/

# 없으면 certbot 다시 실행
docker compose up certbot
```

### 4. "This site can't provide a secure connection"

**원인:** nginx가 SSL 설정을 로드하지 못함

**해결:**
```bash
# nginx 설정 테스트
docker exec nginx nginx -t

# nginx 로그 확인
docker logs nginx

# nginx 재시작
docker compose restart nginx
```

## 간단한 방법 (HTTP만 사용)

SSL 설정이 복잡하면 **HTTP만** 사용할 수도 있습니다:

### 1. 프론트엔드 URL 변경

```javascript
// ❌ HTTPS
const API_URL = 'https://dev.teeto.to';

// ✅ HTTP (임시)
const API_URL = 'http://dev.teeto.to';
```

### 2. Mixed Content 경고 해결

프론트엔드가 HTTPS인데 API가 HTTP면 브라우저에서 차단됩니다.

**해결책:**
- 프론트엔드도 HTTP로 배포 (비추천)
- 또는 SSL 인증서 설정 (권장)

## 요약

### HTTP만 사용 (임시)

```bash
# 1. HTTP로 접근
curl http://dev.teeto.to/api/health

# 2. 프론트엔드에서도 HTTP 사용
```

### HTTPS 설정 (권장)

```bash
# 1. 커밋 & 배포
git add docker-compose.yml nginx/conf.d/
git commit -m "Add SSL support"
git push

# 2. 서버에서
cd /opt/dev
git pull
docker compose up -d

# 3. certbot 로그 확인
docker logs certbot

# 4. SSL 설정 활성화
cp nginx/conf.d/ssl.conf.example nginx/conf.d/ssl.conf
mv nginx/conf.d/default.conf nginx/conf.d/default.conf.disabled
docker exec nginx nginx -s reload

# 5. 테스트
curl https://dev.teeto.to/api/health
```

## 참고 자료

- [Let's Encrypt 공식 문서](https://letsencrypt.org/getting-started/)
- [Certbot Docker](https://hub.docker.com/r/certbot/certbot/)
- [Nginx SSL 설정](https://nginx.org/en/docs/http/configuring_https_servers.html)

---

**작성일:** 2026-02-07
**프로젝트:** TroPig Backend
**도메인:** dev.teeto.to
