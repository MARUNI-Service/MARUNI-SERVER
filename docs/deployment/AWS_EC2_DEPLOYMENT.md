# AWS EC2 Ubuntu 배포 가이드 (MobaXterm 사용)

## 📋 목차
1. [사전 준비 사항](#1-사전-준비-사항)
1-1. [MobaXterm 설치 및 설정](#1-1-mobaxterm-설치-및-설정)
2. [EC2 인스턴스 초기 설정](#2-ec2-인스턴스-초기-설정)
3. [필수 패키지 설치](#3-필수-패키지-설치)
4. [Docker 및 Docker Compose 설치](#4-docker-및-docker-compose-설치)
5. [프로젝트 배포](#5-프로젝트-배포)
6. [환경 변수 설정](#6-환경-변수-설정)
7. [애플리케이션 실행](#7-애플리케이션-실행)
8. [도메인 및 HTTPS 설정 (선택)](#8-도메인-및-https-설정-선택)
9. [모니터링 및 로그 확인](#9-모니터링-및-로그-확인)
10. [트러블슈팅](#10-트러블슈팅)

---

## 1. 사전 준비 사항

### ✅ 체크리스트
- [ ] AWS EC2 Ubuntu 인스턴스 생성 완료
- [ ] SSH 키페어 (.pem 파일) 다운로드
- [ ] 보안 그룹 설정 확인
- [ ] OpenAI API Key 준비 (AI 대화 기능용)
- [ ] Firebase 서비스 계정 키 (푸시 알림용, 선택)

### 📌 EC2 인스턴스 권장 사양
- **OS**: Ubuntu 22.04 LTS 또는 24.04 LTS
- **인스턴스 타입**: t3.small 이상 (t2.micro는 메모리 부족 가능)
- **스토리지**: 20GB 이상
- **메모리**: 최소 2GB (권장 4GB)

### 🔒 보안 그룹 인바운드 규칙
```
포트 22 (SSH)        - 내 IP 주소만 허용
포트 80 (HTTP)       - 0.0.0.0/0 (모든 IP)
포트 443 (HTTPS)     - 0.0.0.0/0 (모든 IP)
포트 8080 (앱)       - 0.0.0.0/0 (테스트용, 나중에 제거 권장)
```

---

## 1-1. MobaXterm 설치 및 설정

### 1-1-1. MobaXterm 설치
1. **다운로드**
   - 공식 사이트: https://mobaxterm.mobatek.net/download.html
   - **Home Edition (Free)** 다운로드
   - Installer edition 또는 Portable edition 선택

2. **설치**
   - Installer edition: 다운로드 후 실행하여 설치
   - Portable edition: 압축 해제 후 바로 실행

### 1-1-2. SSH 세션 생성

#### Step 1: 새 세션 만들기
1. MobaXterm 실행
2. 좌측 상단 **"Session"** 버튼 클릭
3. **"SSH"** 선택

#### Step 2: 세션 설정
**Basic SSH settings 탭:**
```
Remote host: your-ec2-public-ip (예: 43.201.123.45)
✓ Specify username 체크
Username: ubuntu
Port: 22
```

**Advanced SSH settings 탭:**
```
✓ Use private key 체크
Private key 파일 선택 → your-key.pem 파일 찾아서 선택
```

#### Step 3: 연결
1. **"OK"** 버튼 클릭
2. 최초 연결 시 "Do you want to add the host key?" → **"Yes"** 클릭
3. 연결 성공!

### 1-1-3. MobaXterm 주요 기능

#### 왼쪽 패널: SFTP 파일 브라우저
- EC2 서버의 파일/폴더를 GUI로 탐색
- 드래그앤드롭으로 파일 업로드/다운로드 가능
- 파일 더블클릭으로 내장 에디터로 편집 가능

#### 오른쪽 패널: 터미널
- Linux 명령어 실행
- 복사/붙여넣기: Ctrl+Shift+C / Ctrl+Shift+V
- 또는 마우스 우클릭으로 붙여넣기

#### 세션 저장
- 설정한 SSH 세션은 자동으로 왼쪽 "Sessions" 탭에 저장됨
- 다음부터는 더블클릭만으로 바로 접속 가능

#### 멀티 세션
- 상단 탭으로 여러 SSH 세션 동시 사용 가능
- 같은 서버에 여러 터미널 동시 열기 가능

---

## 2. EC2 인스턴스 초기 설정

### 2.1 SSH 접속 확인
MobaXterm으로 EC2에 접속했다면, 터미널에 다음과 같이 표시됩니다:
```
Welcome to Ubuntu 22.04.x LTS (GNU/Linux ...)

ubuntu@ip-xxx-xxx-xxx-xxx:~$
```

### 2.2 시스템 업데이트
```bash
# 패키지 목록 업데이트
sudo apt update

# 설치된 패키지 업그레이드
sudo apt upgrade -y
```

### 2.3 타임존 설정 (선택)
```bash
# 서울 시간대로 설정
sudo timedatectl set-timezone Asia/Seoul

# 확인
timedatectl
```

### 2.4 스왑 메모리 추가 (t2.micro 등 메모리 부족 시)
```bash
# 2GB 스왑 파일 생성
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 영구 적용
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 확인
free -h
```

---

## 3. 필수 패키지 설치

```bash
# Git 설치
sudo apt install -y git

# curl, wget 설치 (보통 기본 설치되어 있음)
sudo apt install -y curl wget

# unzip 설치
sudo apt install -y unzip

# 빌드 도구 설치 (Java 빌드용)
sudo apt install -y build-essential
```

---

## 4. Docker 및 Docker Compose 설치

### 4.1 Docker 설치
```bash
# 기존 Docker 제거 (있을 경우)
sudo apt remove -y docker docker-engine docker.io containerd runc

# Docker 공식 GPG 키 추가
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Docker 설치 확인
docker --version
```

### 4.2 Docker 권한 설정
```bash
# 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령 실행)
sudo usermod -aG docker $USER

# 변경사항 적용 (재로그인 필요)
newgrp docker

# 확인
docker ps
```

### 4.3 Docker Compose 확인
```bash
# Docker Compose 버전 확인 (V2는 플러그인으로 설치됨)
docker compose version
```

---

## 5. 프로젝트 배포

### 5.1 작업 디렉토리 생성
```bash
# 홈 디렉토리로 이동
cd ~

# 프로젝트 디렉토리 생성
mkdir -p maruni
cd maruni
```

### 5.2 방법 A: Git으로 직접 클론 (권장)
```bash
# Git 저장소에서 클론
git clone https://github.com/yourusername/maruni-server.git
cd maruni-server

# 특정 브랜치로 전환 (필요시)
git checkout main
```

### 5.3 방법 B: MobaXterm으로 파일 전송 (권장 - GUI 방식)
**로컬 PC의 프로젝트 파일을 EC2로 업로드:**

1. **MobaXterm 왼쪽 SFTP 패널 사용**
   - MobaXterm에서 EC2 접속 상태 유지
   - 왼쪽 파일 브라우저에서 `/home/ubuntu/maruni` 폴더로 이동
   - 없으면 우클릭 → "Create new folder" → `maruni` 생성

2. **프로젝트 폴더 업로드**
   - Windows 탐색기에서 `maruni-server` 폴더 전체 선택
   - MobaXterm 왼쪽 패널 `/home/ubuntu/maruni/` 위치로 **드래그 앤 드롭**
   - 업로드 진행 상황 표시됨 (시간 소요: 1~3분)

3. **업로드 완료 확인**
   - 터미널에서 확인:
   ```bash
   cd ~/maruni/maruni-server
   ls -la
   ```

**📌 주의사항:**
- `.git` 폴더, `build/`, `out/`, `node_modules/` 등 불필요한 폴더는 제외하고 업로드 권장
- `.env` 파일은 보안상 업로드하지 말고 서버에서 직접 생성

### 5.4 방법 C: SCP 명령어 (터미널 사용)
```bash
# 로컬 PC에서 실행 (새 터미널)
# 프로젝트 루트 디렉토리로 이동 후
scp -i "your-key.pem" -r maruni-server ubuntu@your-ec2-public-ip:~/maruni/

# 다시 EC2 SSH 세션으로 돌아가서
cd ~/maruni/maruni-server
```

### 5.5 프로젝트 구조 확인
```bash
ls -la

# 예상 파일들
# Dockerfile
# docker-compose.yml
# build.gradle
# .env.example
# src/
```

---

## 6. 환경 변수 설정

### 6.1 .env 파일 생성

#### 방법 A: MobaXterm 내장 에디터 사용 (권장 - GUI)
1. **파일 복사**
   ```bash
   cp .env.example .env
   ```

2. **MobaXterm으로 편집**
   - 왼쪽 SFTP 패널에서 `.env` 파일 더블클릭
   - 내장 에디터 열림
   - 환경 변수 값 수정 (아래 6.2 참조)
   - 저장: `Ctrl+S` 또는 상단 저장 버튼
   - 에디터 닫기

#### 방법 B: nano 에디터 사용 (터미널)
```bash
# .env.example 복사
cp .env.example .env

# nano 에디터로 편집
nano .env

# 편집 후 저장: Ctrl+O → Enter → Ctrl+X
```

### 6.2 필수 환경 변수 설정
```bash
# === 운영 환경 프로파일 ===
SPRING_PROFILES_ACTIVE=prod

# === DB 설정 ===
DB_USERNAME=maruni_admin
DB_PASSWORD=your_strong_db_password_here_123!@#

# === Redis 설정 ===
REDIS_PASSWORD=your_strong_redis_password_here_456!@#

# === JWT 설정 (필수) ===
JWT_SECRET_KEY=your_jwt_secret_key_at_least_32_characters_long_make_it_random_and_secure!
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000

# === OpenAI API (필수) ===
OPENAI_API_KEY=sk-proj-your-actual-openai-api-key-here
OPENAI_MODEL=gpt-4o
OPENAI_MAX_TOKENS=100
OPENAI_TEMPERATURE=0.7

# === Swagger 설정 ===
# EC2 퍼블릭 IP 또는 도메인으로 변경
SWAGGER_SERVER_URL=http://your-ec2-public-ip:8080
# 예: SWAGGER_SERVER_URL=http://43.201.123.45:8080
# 도메인 있을 경우: SWAGGER_SERVER_URL=https://api.maruni.com
```

**중요 보안 사항:**
- 모든 비밀번호는 강력하게 설정 (영문 대소문자, 숫자, 특수문자 조합 16자 이상)
- JWT_SECRET_KEY는 최소 32자 이상의 랜덤 문자열
- OpenAI API Key는 실제 발급받은 키로 교체
- .env 파일은 절대 Git에 커밋하지 않음

### 6.3 파일 저장 및 권한 설정
```bash
# MobaXterm 에디터 사용 시: 그냥 저장하고 닫기
# nano 에디터 사용 시: Ctrl+O → Enter → Ctrl+X

# .env 파일 권한 제한 (소유자만 읽기/쓰기)
chmod 600 .env

# 확인
ls -l .env
# 출력: -rw------- 1 ubuntu ubuntu ... .env
```

### 💡 MobaXterm 팁: 환경 변수 값 생성
**강력한 비밀번호 생성:**
```bash
# 랜덤 32자 문자열 생성 (JWT_SECRET_KEY용)
openssl rand -base64 32

# 랜덤 16자 문자열 생성 (DB/Redis 비밀번호용)
openssl rand -base64 16
```
생성된 값을 복사해서 `.env` 파일에 붙여넣기

---

## 7. 애플리케이션 실행

### 7.1 Docker Compose로 전체 스택 실행
```bash
# 백그라운드 모드로 실행 (-d: detached)
docker compose up -d --build

# 실행 로그 실시간 확인 (Ctrl+C로 종료, 컨테이너는 계속 실행)
docker compose logs -f
```

### 7.2 실행 확인
```bash
# 컨테이너 상태 확인
docker compose ps

# 예상 출력:
# NAME                IMAGE               STATUS              PORTS
# maruni-app          maruni-app:latest   Up 2 minutes        0.0.0.0:8080->8080/tcp
# postgres-db         postgres:15         Up 2 minutes        0.0.0.0:5432->5432/tcp
# redis               redis:7             Up 2 minutes        0.0.0.0:6379->6379/tcp
```

### 7.3 헬스체크
```bash
# 애플리케이션 헬스체크
curl http://localhost:8080/actuator/health

# 예상 응답:
# {"status":"UP"}
```

### 7.4 Swagger UI 접속
브라우저에서 접속:
```
http://your-ec2-public-ip:8080/swagger-ui/index.html
```

---

## 8. 도메인 및 HTTPS 설정 (선택)

### 8.1 도메인 연결
1. 도메인 구입 (가비아, Route53 등)
2. DNS A 레코드 추가: `api.maruni.com` → EC2 퍼블릭 IP

### 8.2 Nginx 리버스 프록시 설치
```bash
# Nginx 설치
sudo apt install -y nginx

# 설정 파일 생성
sudo nano /etc/nginx/sites-available/maruni
```

Nginx 설정 내용:
```nginx
server {
    listen 80;
    server_name api.maruni.com;  # 실제 도메인으로 변경

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# 설정 활성화
sudo ln -s /etc/nginx/sites-available/maruni /etc/nginx/sites-enabled/

# 기본 설정 제거 (필요시)
sudo rm /etc/nginx/sites-enabled/default

# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

### 8.3 Let's Encrypt SSL 인증서 (HTTPS)
```bash
# Certbot 설치
sudo apt install -y certbot python3-certbot-nginx

# SSL 인증서 발급 및 자동 설정
sudo certbot --nginx -d api.maruni.com

# 자동 갱신 테스트
sudo certbot renew --dry-run
```

### 8.4 .env 파일 업데이트
```bash
nano .env

# SWAGGER_SERVER_URL 수정
SWAGGER_SERVER_URL=https://api.maruni.com
```

```bash
# 애플리케이션 재시작
docker compose restart app
```

---

## 9. 모니터링 및 로그 확인

### 9.1 로그 확인
```bash
# 전체 로그 실시간 확인
docker compose logs -f

# 특정 서비스 로그만 확인
docker compose logs -f app
docker compose logs -f db
docker compose logs -f redis

# 최근 100줄만 확인
docker compose logs --tail=100 app
```

### 9.2 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 확인
docker compose ps

# 리소스 사용량 확인
docker stats

# 디스크 사용량 확인
docker system df
```

### 9.3 데이터베이스 접속 (디버깅용)
```bash
# PostgreSQL 컨테이너 내부 접속
docker exec -it postgres-db psql -U maruni_admin -d maruni-db

# SQL 쿼리 예시
\dt  -- 테이블 목록
SELECT * FROM member_entity LIMIT 5;
\q  -- 종료
```

### 9.4 Redis 접속 (디버깅용)
```bash
# Redis 컨테이너 내부 접속
docker exec -it redis redis-cli -a your_redis_password

# Redis 명령 예시
KEYS *  -- 모든 키 확인
GET refresh_token:user123  -- 특정 키 조회
exit  -- 종료
```

---

## 10. 트러블슈팅

### 문제 1: 컨테이너가 시작되지 않음
```bash
# 상세 로그 확인
docker compose logs app

# 일반적인 원인:
# - .env 파일 누락 또는 필수 환경변수 미설정
# - DB/Redis 연결 실패
# - 포트 충돌 (8080, 5432, 6379 이미 사용 중)
```

**해결 방법:**
```bash
# .env 파일 확인
cat .env

# 포트 사용 확인
sudo netstat -tlnp | grep -E '8080|5432|6379'

# 컨테이너 재시작
docker compose down
docker compose up -d --build
```

### 문제 2: DB 연결 실패
```bash
# DB 컨테이너 상태 확인
docker compose ps db

# DB 로그 확인
docker compose logs db

# DB 헬스체크
docker exec postgres-db pg_isready -U maruni_admin
```

**해결 방법:**
```bash
# DB 재시작
docker compose restart db

# DB 볼륨 삭제 후 재생성 (주의: 데이터 삭제됨)
docker compose down -v
docker compose up -d
```

### 문제 3: 메모리 부족 (Java heap space)
```bash
# 메모리 확인
free -h

# 스왑 메모리 추가 (위의 2.4 참조)
```

**해결 방법 - Dockerfile에 Java 메모리 설정:**
```dockerfile
# Dockerfile의 ENTRYPOINT 수정
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

```bash
# 재빌드
docker compose up -d --build
```

### 문제 4: OpenAI API 연결 실패
```bash
# 로그 확인
docker compose logs app | grep -i openai

# 일반적인 원인:
# - API Key 오류 (sk-proj- 형식 확인)
# - API 요금 부족 또는 계정 제한
# - 네트워크 방화벽 차단
```

**해결 방법:**
```bash
# .env 파일의 OPENAI_API_KEY 재확인
nano .env

# API Key 테스트 (로컬에서)
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# 앱 재시작
docker compose restart app
```

### 문제 5: Swagger UI 접속 안됨
```bash
# 애플리케이션 로그 확인
docker compose logs app | grep -i swagger

# 보안그룹 8080 포트 열림 확인
# 방화벽 확인
sudo ufw status
```

**해결 방법:**
```bash
# EC2 보안그룹에서 8080 포트 인바운드 규칙 추가
# Type: Custom TCP
# Port: 8080
# Source: 0.0.0.0/0
```

### 문제 6: 디스크 공간 부족
```bash
# 디스크 사용량 확인
df -h

# Docker 리소스 정리
docker system prune -a --volumes

# 사용하지 않는 이미지/컨테이너 삭제
docker compose down
docker system prune -a
```

---

## 🔄 일상 운영 명령어

### 애플리케이션 관리
```bash
# 전체 재시작
docker compose restart

# 특정 서비스만 재시작
docker compose restart app

# 전체 중지
docker compose stop

# 전체 시작
docker compose start

# 전체 종료 및 삭제 (볼륨 유지)
docker compose down

# 전체 종료 및 볼륨까지 삭제 (데이터 완전 삭제)
docker compose down -v
```

### 업데이트 배포
```bash
# Git에서 최신 코드 받기
git pull origin main

# .env 파일 업데이트 (필요시)
nano .env

# 재빌드 및 재시작
docker compose up -d --build

# 로그 확인
docker compose logs -f app
```

### 백업 (권장)
```bash
# PostgreSQL 백업
docker exec postgres-db pg_dump -U maruni_admin maruni-db > backup_$(date +%Y%m%d).sql

# Redis 백업 (자동 저장됨)
docker exec redis redis-cli -a your_redis_password SAVE
```

---

## 📚 추가 자료

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/actuator/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Nginx 리버스 프록시 가이드](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Let's Encrypt 문서](https://letsencrypt.org/getting-started/)

---

## ✅ 배포 완료 체크리스트

- [ ] EC2 인스턴스 초기 설정 완료
- [ ] Docker & Docker Compose 설치 완료
- [ ] 프로젝트 파일 업로드 완료
- [ ] .env 파일 설정 완료 (모든 필수 환경변수 입력)
- [ ] `docker compose up -d --build` 성공
- [ ] 헬스체크 통과 (`/actuator/health`)
- [ ] Swagger UI 접속 가능
- [ ] (선택) 도메인 연결 완료
- [ ] (선택) HTTPS 설정 완료
- [ ] 로그 모니터링 정상 작동

---

**배포 완료!** 🎉

문제 발생 시 [트러블슈팅](#10-트러블슈팅) 섹션을 참고하거나 `docker compose logs -f`로 로그를 확인하세요.
