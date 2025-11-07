# HTTPS 설정 가이드 (Nginx + Let's Encrypt)

**작성일**: 2025-11-06
**대상 서버**: AWS EC2 (52.78.61.52)
**서버 환경**: Spring Boot (포트 8080)
**목표**: HTTP → HTTPS 전환 + 도메인 연결

---

## 📋 목차

1. [사전 준비](#1-사전-준비)
2. [도메인 구매 및 DNS 설정](#2-도메인-구매-및-dns-설정)
3. [Nginx 설치](#3-nginx-설치)
4. [방화벽 설정](#4-방화벽-설정)
5. [Nginx 기본 설정](#5-nginx-기본-설정)
6. [Let's Encrypt SSL 인증서 발급](#6-lets-encrypt-ssl-인증서-발급)
7. [Nginx HTTPS 설정](#7-nginx-https-설정)
8. [SSL 자동 갱신 설정](#8-ssl-자동-갱신-설정)
9. [테스트 및 검증](#9-테스트-및-검증)
10. [트러블슈팅](#10-트러블슈팅)

---

## 1. 사전 준비

### ✅ 체크리스트

- [ ] 서버 접속 가능 (SSH)
- [ ] 서버 IP 주소 확인: `52.78.61.52`
- [ ] Spring Boot 서버 실행 중 (포트 8080)
- [ ] 도메인 구매 예정 또는 보유
- [ ] 서버 OS 확인 (Ubuntu/Amazon Linux 등)

### 서버 정보 확인

```bash
# SSH 접속
ssh -i your-key.pem ubuntu@52.78.61.52

# OS 버전 확인
cat /etc/os-release

# Spring Boot 서버 실행 확인
curl http://localhost:8080/actuator/health
```

---

## 2. 도메인 구매 및 DNS 설정

### 2-1. 도메인 구매

**추천 업체** (한국):
- **가비아** (gabia.com) - 약 15,000원/년
- **Cloudflare** (cloudflare.com) - 약 $10/년
- **AWS Route 53** - 약 $12/년

**추천 도메인 예시**:
- `api.maruni.com` (서버용)
- `maruni.com` (메인), `www.maruni.com`

### 2-2. DNS A 레코드 설정

도메인 관리 페이지에서 설정:

```
Type: A
Name: api (또는 @)
Value: 52.78.61.52
TTL: 600 (10분)
```

**예시 (가비아)**:
```
호스트: api
값/위치: 52.78.61.52
타입: A
TTL: 600
```

### 2-3. DNS 전파 확인 (5-30분 소요)

```bash
# Windows (로컬)
nslookup api.maruni.com

# 예상 결과:
# Address: 52.78.61.52
```

또는 웹사이트에서 확인:
- https://dnschecker.org

---

## 3. Nginx 설치

### Ubuntu/Debian

```bash
# 패키지 업데이트
sudo apt update

# Nginx 설치
sudo apt install nginx -y

# 설치 확인
nginx -v
# 예상: nginx version: nginx/1.18.0 (Ubuntu)

# Nginx 시작
sudo systemctl start nginx
sudo systemctl enable nginx

# 상태 확인
sudo systemctl status nginx
```

### Amazon Linux 2

```bash
# Nginx 설치
sudo amazon-linux-extras install nginx1 -y

# 시작 및 활성화
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 설치 확인

브라우저에서 접속:
```
http://52.78.61.52
```

**예상 화면**: "Welcome to nginx!" 페이지

---

## 4. 방화벽 설정

### AWS EC2 Security Group

AWS 콘솔 → EC2 → Security Groups → 해당 보안 그룹 선택

**Inbound Rules 추가**:

| Type       | Protocol | Port Range | Source    | Description        |
|------------|----------|------------|-----------|-------------------|
| HTTP       | TCP      | 80         | 0.0.0.0/0 | Nginx HTTP        |
| HTTPS      | HTTPS    | 443        | 0.0.0.0/0 | Nginx HTTPS       |
| Custom TCP | TCP      | 8080       | 127.0.0.1/32 | Spring Boot (로컬만) |

**⚠️ 중요**: 8080 포트는 `127.0.0.1`만 허용 (외부 차단)

### Ubuntu UFW 방화벽 (서버 내부)

```bash
# UFW 상태 확인
sudo ufw status

# 80, 443 포트 열기
sudo ufw allow 'Nginx Full'

# 또는 수동으로
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# SSH 포트 확인 (차단되지 않도록)
sudo ufw allow 22/tcp

# 활성화
sudo ufw enable
```

---

## 5. Nginx 기본 설정

### 5-1. Spring Boot 연결 테스트 (HTTP)

```bash
# Nginx 설정 파일 생성
sudo nano /etc/nginx/sites-available/maruni
```

**파일 내용** (임시 HTTP 설정):

```nginx
server {
    listen 80;
    server_name api.maruni.com;  # 본인 도메인으로 변경

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 5-2. 설정 활성화

```bash
# 심볼릭 링크 생성
sudo ln -s /etc/nginx/sites-available/maruni /etc/nginx/sites-enabled/

# 기본 설정 제거 (선택 사항)
sudo rm /etc/nginx/sites-enabled/default

# 설정 문법 검사
sudo nginx -t
# 예상: syntax is ok, test is successful

# Nginx 재시작
sudo systemctl reload nginx
```

### 5-3. HTTP 동작 확인

브라우저에서 접속:
```
http://api.maruni.com/api/join/email-check?memberEmail=test@test.com
```

**예상 결과**: Spring Boot API 응답 확인

---

## 6. Let's Encrypt SSL 인증서 발급

### 6-1. Certbot 설치

#### Ubuntu

```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y
```

#### Amazon Linux 2

```bash
# EPEL 저장소 추가
sudo yum install -y epel-release

# Certbot 설치
sudo yum install certbot python3-certbot-nginx -y
```

### 6-2. SSL 인증서 발급

```bash
# 자동 설정 (Nginx 설정 자동 수정)
sudo certbot --nginx -d api.maruni.com

# 또는 수동 설정
sudo certbot certonly --nginx -d api.maruni.com
```

**프롬프트 질문**:

```
1. 이메일 입력: your-email@example.com
2. 약관 동의: Y
3. 뉴스레터 구독: N (선택)
4. HTTP → HTTPS 리다이렉트: 2 (Redirect - 자동 리다이렉트)
```

### 6-3. 발급 확인

```bash
# 인증서 파일 확인
sudo ls -la /etc/letsencrypt/live/api.maruni.com/

# 예상 파일:
# - fullchain.pem (인증서 체인)
# - privkey.pem (개인키)
```

---

## 7. Nginx HTTPS 설정

Certbot이 자동으로 설정했다면 이미 완료. 수동 확인/수정:

```bash
sudo nano /etc/nginx/sites-available/maruni
```

**최종 설정 파일**:

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name api.maruni.com;

    # Let's Encrypt 인증서 갱신용 경로
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # 나머지 모든 요청은 HTTPS로 리다이렉트
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS 서버
server {
    listen 443 ssl http2;
    server_name api.maruni.com;

    # SSL 인증서 설정
    ssl_certificate /etc/letsencrypt/live/api.maruni.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.maruni.com/privkey.pem;

    # SSL 보안 설정 (권장)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';

    # HSTS (선택, 보안 강화)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Spring Boot 프록시 설정
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 지원 (필요 시)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

### 설정 적용

```bash
# 문법 검사
sudo nginx -t

# Nginx 재시작
sudo systemctl reload nginx
```

---

## 8. SSL 자동 갱신 설정

Let's Encrypt 인증서는 **90일마다 갱신** 필요.

### 8-1. 자동 갱신 테스트

```bash
# Dry-run 테스트 (실제 갱신 안 함)
sudo certbot renew --dry-run

# 예상 출력: Congratulations, all simulated renewals succeeded
```

### 8-2. Cron 자동 갱신 설정 (이미 설정됨)

```bash
# Cron 작업 확인
sudo systemctl status certbot.timer

# 또는 수동 확인
sudo crontab -l

# 예상: 0 0,12 * * * certbot renew --quiet
```

Certbot 설치 시 자동으로 설정되므로 별도 작업 불필요.

---

## 9. 테스트 및 검증

### 9-1. HTTPS 접속 테스트

브라우저에서:
```
https://api.maruni.com/api/join/email-check?memberEmail=test@test.com
```

**확인 사항**:
- ✅ 자물쇠 아이콘 표시
- ✅ 인증서 유효
- ✅ API 정상 응답

### 9-2. HTTP → HTTPS 리다이렉트 확인

```
http://api.maruni.com/api/join/email-check?memberEmail=test@test.com
```

**예상**: 자동으로 `https://...`로 리다이렉트

### 9-3. SSL Labs 테스트

웹사이트에서 보안 등급 확인:
```
https://www.ssllabs.com/ssltest/analyze.html?d=api.maruni.com
```

**목표 등급**: A 이상

### 9-4. Vercel 환경 변수 업데이트

Vercel Dashboard:
```
Name: VITE_API_BASE_URL
Value: https://api.maruni.com/api  # HTTP → HTTPS 변경
```

**Save** 후 **Redeploy**

### 9-5. 클라이언트 로컬 환경 변수 업데이트

`.env.local` 파일:
```bash
VITE_API_BASE_URL=https://api.maruni.com/api
```

개발 서버 재시작:
```bash
npm run dev
```

### 9-6. 최종 통합 테스트

1. Vercel 배포 URL 접속
2. 회원가입/로그인 시도
3. 개발자 도구 Network 탭:
   - ✅ `https://api.maruni.com/api/...` 요청
   - ✅ 200 OK 응답
   - ❌ Mixed Content 에러 없음

---

## 10. 트러블슈팅

### ❌ 502 Bad Gateway

**원인**: Spring Boot 서버가 꺼짐

**해결**:
```bash
# Spring Boot 서버 상태 확인
sudo systemctl status maruni  # (systemd 사용 시)
# 또는
curl http://localhost:8080/actuator/health

# 서버 재시작
sudo systemctl restart maruni
```

### ❌ 인증서 발급 실패: "DNS resolution failed"

**원인**: DNS 설정 미완료 또는 전파 대기 중

**해결**:
```bash
# DNS 확인
nslookup api.maruni.com

# 15-30분 대기 후 재시도
sudo certbot --nginx -d api.maruni.com
```

### ❌ Nginx 설정 오류

**해결**:
```bash
# 문법 검사
sudo nginx -t

# 오류 로그 확인
sudo tail -f /var/log/nginx/error.log

# 설정 원복
sudo cp /etc/nginx/sites-available/maruni.backup /etc/nginx/sites-available/maruni
sudo systemctl reload nginx
```

### ❌ CORS 에러 (클라이언트)

**원인**: Spring Boot CORS 설정 누락

**해결**: `SecurityConfig.java`에서 CORS 설정 확인
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("https://maruni-client.vercel.app");
    config.addAllowedOrigin("http://localhost:3000");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### ❌ 방화벽 차단

**해결**:
```bash
# AWS Security Group 확인 (80, 443 열림)
# UFW 확인
sudo ufw status

# 포트 열기
sudo ufw allow 'Nginx Full'
```

---

## 📊 체크리스트 (전체 과정)

- [ ] 서버 SSH 접속 확인
- [ ] 도메인 구매 완료
- [ ] DNS A 레코드 설정 (`52.78.61.52`)
- [ ] DNS 전파 확인 (`nslookup`)
- [ ] Nginx 설치 완료
- [ ] AWS Security Group 80, 443 포트 오픈
- [ ] UFW 방화벽 80, 443 허용
- [ ] Nginx HTTP 프록시 설정
- [ ] HTTP 동작 확인 (`http://api.maruni.com`)
- [ ] Certbot 설치
- [ ] SSL 인증서 발급 성공
- [ ] Nginx HTTPS 설정 완료
- [ ] HTTPS 접속 확인 (`https://api.maruni.com`)
- [ ] HTTP → HTTPS 리다이렉트 확인
- [ ] SSL Labs 테스트 (A등급)
- [ ] Vercel 환경 변수 업데이트
- [ ] 로컬 `.env.local` 업데이트
- [ ] 클라이언트-서버 통합 테스트 성공

---

## 🔧 유지보수

### 인증서 만료 확인

```bash
# 인증서 유효기간 확인
sudo certbot certificates

# 예상 출력:
# Expiry Date: 2025-02-04 (90일 후)
```

### Nginx 로그 확인

```bash
# 접근 로그
sudo tail -f /var/log/nginx/access.log

# 에러 로그
sudo tail -f /var/log/nginx/error.log
```

### Spring Boot 로그 확인

```bash
# 애플리케이션 로그 (경로는 설정에 따라 다름)
sudo tail -f /var/log/maruni/application.log
```

---

## 📚 참고 자료

- [Let's Encrypt 공식 문서](https://letsencrypt.org/getting-started/)
- [Nginx 공식 문서](https://nginx.org/en/docs/)
- [Certbot 공식 문서](https://certbot.eff.org/)
- [SSL Labs 테스트](https://www.ssllabs.com/ssltest/)

---

**작성자**: Claude Code
**최종 업데이트**: 2025-11-06
**예상 소요 시간**: 1-2시간 (도메인 전파 시간 제외)

