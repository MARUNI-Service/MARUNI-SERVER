# GitHub Actions CI/CD 설정 가이드

> **🚀 자동 배포**: `main` 브랜치에 push 시 자동으로 Docker Hub 빌드 및 EC2 배포

## 📋 목차
1. [CI/CD 플로우 개요](#1-cicd-플로우-개요)
2. [GitHub Secrets 설정](#2-github-secrets-설정)
3. [워크플로우 파일 설명](#3-워크플로우-파일-설명)
4. [배포 프로세스](#4-배포-프로세스)
5. [트러블슈팅](#5-트러블슈팅)

---

## 1. CI/CD 플로우 개요

### **전체 자동화 플로우**
```
Pull Request 생성
    ↓
CI: 테스트 실행 (.github/workflows/ci.yml)
    ↓
코드 리뷰 및 승인
    ↓
main 브랜치로 merge
    ↓
CD: 자동 배포 시작 (.github/workflows/deploy.yml)
    ├── 1. Gradle 빌드
    ├── 2. Docker 이미지 빌드
    ├── 3. Docker Hub에 푸시
    ├── 4. EC2 SSH 접속
    ├── 5. Docker 이미지 Pull
    ├── 6. 컨테이너 재시작
    └── 7. 헬스체크 확인
    ↓
배포 완료! 🎉
```

### **워크플로우 파일 구조**
```
.github/workflows/
├── ci.yml      # CI: Pull Request 시 테스트 실행
└── deploy.yml  # CD: main 브랜치 push 시 자동 배포
```

---

## 2. GitHub Secrets 설정

### **2.1 GitHub Repository Secrets 추가**

1. **GitHub 저장소 접속**
   ```
   https://github.com/Kimgyuilli/maruni
   ```

2. **Settings → Secrets and variables → Actions 이동**
   - 상단 메뉴에서 **"Settings"** 클릭
   - 왼쪽 사이드바에서 **"Secrets and variables"** → **"Actions"** 클릭

3. **New repository secret 클릭**

---

### **2.2 필수 Secrets 등록 (5개)**

#### **Secret 1: DOCKERHUB_USERNAME**
```
Name: DOCKERHUB_USERNAME
Secret: kimgyuill
```
- Docker Hub 사용자 이름

---

#### **Secret 2: DOCKERHUB_TOKEN**
```
Name: DOCKERHUB_TOKEN
Secret: [Docker Hub Access Token]
```

**Docker Hub Access Token 생성 방법:**
1. https://hub.docker.com 로그인
2. 우측 상단 프로필 → **Account Settings** 클릭
3. 좌측 메뉴에서 **Security** 클릭
4. **New Access Token** 클릭
5. 토큰 정보 입력:
   ```
   Access Token Description: GitHub Actions MARUNI
   Access permissions: Read, Write, Delete
   ```
6. **Generate** 클릭
7. **생성된 토큰 복사** (한 번만 표시됨!)
8. GitHub Secret에 붙여넣기

⚠️ **중요**: 비밀번호가 아닌 **Access Token**을 사용해야 합니다!

---

#### **Secret 3: EC2_HOST**
```
Name: EC2_HOST
Secret: api.maruni.kro.kr
```
- EC2 서버 도메인 또는 Public IP 주소
- 현재 설정: `api.maruni.kro.kr`

---

#### **Secret 4: EC2_USERNAME**
```
Name: EC2_USERNAME
Secret: ubuntu
```
- EC2 SSH 접속 사용자 이름 (Ubuntu는 기본적으로 `ubuntu`)

---

#### **Secret 5: EC2_SSH_KEY**
```
Name: EC2_SSH_KEY
Secret: [SSH Private Key 전체 내용]
```

**SSH Private Key (.pem 파일) 등록 방법:**

1. **로컬 PC에서 .pem 파일 열기**
   ```bash
   # Windows PowerShell에서
   notepad C:\path\to\your-key.pem

   # 또는 Git Bash에서
   cat /c/path/to/your-key.pem
   ```

2. **전체 내용 복사**
   ```
   -----BEGIN RSA PRIVATE KEY-----
   MIIEpAIBAAKCAQEA...
   (전체 내용)
   ...
   -----END RSA PRIVATE KEY-----
   ```

3. **GitHub Secret에 붙여넣기**
   - **처음부터 끝까지 전체 내용 복사** (공백, 줄바꿈 포함)
   - `-----BEGIN RSA PRIVATE KEY-----`부터 `-----END RSA PRIVATE KEY-----`까지 모두 포함

⚠️ **주의사항**:
- 앞뒤 공백 없이 복사
- 줄바꿈 그대로 유지
- BEGIN/END 라인 포함

---

### **2.3 Secrets 등록 완료 확인**

등록 후 다음과 같이 5개 Secret이 표시되어야 합니다:

```
Repository secrets (5)
├── DOCKERHUB_USERNAME
├── DOCKERHUB_TOKEN
├── EC2_HOST
├── EC2_USERNAME
└── EC2_SSH_KEY
```

---

## 3. 워크플로우 파일 설명

### **3.1 CI 워크플로우 (ci.yml)**

```yaml
# Pull Request 시 자동 실행
on:
  pull_request:
    branches: [ main, dev ]

# 작업 내용
jobs:
  test:
    - JDK 21 설정
    - Gradle 테스트 실행
    - 테스트 리포트 생성
    - 빌드 실행
```

**트리거**: Pull Request 생성 시

---

### **3.2 CD 워크플로우 (deploy.yml)**

```yaml
# main 브랜치 push 시 자동 실행
on:
  push:
    branches: [ main ]
  workflow_dispatch:  # 수동 실행 옵션

# 배포 작업
jobs:
  deploy:
    - Gradle 빌드 (테스트 제외)
    - Docker 이미지 빌드
    - Docker Hub에 푸시 (latest + commit SHA 태그)
    - EC2 SSH 접속
    - Docker 이미지 Pull
    - 컨테이너 재시작
    - 헬스체크 (60초 대기)
    - 이미지 정리
```

**트리거**:
- `main` 브랜치로 push/merge
- 수동 실행 (Actions 탭에서)

---

## 4. 배포 프로세스

### **4.1 자동 배포 (권장)**

#### **Step 1: 개발 브랜치에서 작업**
```bash
# 새 브랜치 생성
git checkout -b feature/new-feature

# 코드 수정 후 커밋
git add .
git commit -m "feat: 새 기능 추가"
git push origin feature/new-feature
```

#### **Step 2: Pull Request 생성**
1. GitHub에서 Pull Request 생성
2. **CI 자동 실행** (테스트)
3. 테스트 통과 확인
4. 코드 리뷰 진행

#### **Step 3: main 브랜치로 Merge**
```bash
# Pull Request 승인 후 Merge 버튼 클릭
# 또는 로컬에서 merge
git checkout main
git pull origin main
git merge feature/new-feature
git push origin main
```

#### **Step 4: 자동 배포 확인**
1. GitHub Actions 탭에서 배포 진행 상황 확인
   ```
   https://github.com/Kimgyuilli/maruni/actions
   ```
2. **CD - Deploy to EC2** 워크플로우 실행 확인
3. 각 단계 로그 확인
4. 배포 완료 확인 (약 5-10분 소요)

#### **Step 5: 배포 검증**
```bash
# 로컬에서 API 헬스체크
curl https://api.maruni.kro.kr/actuator/health

# 예상 응답
{"status":"UP"}

# Swagger UI 접속
https://api.maruni.kro.kr/swagger-ui/index.html
```

---

### **4.2 수동 배포 (긴급 배포)**

1. **GitHub Actions 탭 이동**
   ```
   https://github.com/Kimgyuilli/maruni/actions
   ```

2. **CD - Deploy to EC2 워크플로우 선택**

3. **Run workflow 버튼 클릭**
   - Branch: `main` 선택
   - **Run workflow** 클릭

4. **배포 진행 상황 확인**

---

### **4.3 배포 단계별 세부 내용**

#### **1. Gradle 빌드**
```bash
./gradlew clean build -x test --no-daemon
```
- 테스트 제외 빌드 (CI에서 이미 테스트 완료)
- 빌드 시간: 약 2-3분

#### **2. Docker 이미지 빌드 및 푸시**
```bash
docker build -t kimgyuill/maruni-server:latest .
docker tag kimgyuill/maruni-server:latest kimgyuill/maruni-server:<commit-sha>
docker push kimgyuill/maruni-server:latest
docker push kimgyuill/maruni-server:<commit-sha>
```
- 두 개 태그 푸시: `latest`, `<commit-sha>`
- 푸시 시간: 약 3-5분

#### **3. EC2 배포**
```bash
# SSH 접속
ssh ubuntu@api.maruni.kro.kr

# Docker 이미지 Pull
docker pull kimgyuill/maruni-server:latest

# 기존 컨테이너 중지
docker compose -f docker-compose.prod.yml down app

# 새 컨테이너 시작
docker compose -f docker-compose.prod.yml up -d app

# 헬스체크 (60초 대기)
# ...

# 이미지 정리
docker image prune -f
```
- 배포 시간: 약 3-5분

---

## 5. 트러블슈팅

### **문제 1: Docker Hub 로그인 실패**
```
Error: Cannot perform an interactive login from a non TTY device
```

**해결 방법:**
1. `DOCKERHUB_TOKEN`이 올바르게 설정되었는지 확인
2. Docker Hub에서 Access Token 재생성
3. GitHub Secret 업데이트

---

### **문제 2: SSH 접속 실패**
```
Error: ssh: connect to host api.maruni.kro.kr port 22: Connection refused
```

**원인:**
- EC2 보안 그룹에서 SSH(22) 포트 차단
- SSH Key 오류
- 도메인 또는 IP 주소 오류

**해결 방법:**
1. **EC2 보안 그룹 확인**
   - AWS Console → EC2 → Security Groups
   - Inbound Rules에 SSH(22) 포트 허용 확인
   - GitHub Actions IP 허용 (0.0.0.0/0 권장, 보안상 제한 가능)

2. **SSH Key 확인**
   ```bash
   # 로컬에서 SSH 접속 테스트
   ssh -i your-key.pem ubuntu@api.maruni.kro.kr
   ```

3. **EC2_HOST Secret 확인**
   - 도메인: `api.maruni.kro.kr`
   - 또는 Public IP: `43.201.xxx.xxx`

---

### **문제 3: Docker Compose 파일 없음**
```
Error: no configuration file provided: not found
```

**해결 방법:**
```bash
# EC2에 SSH 접속
ssh ubuntu@api.maruni.kro.kr

# 파일 확인
cd ~/maruni
ls -la

# docker-compose.prod.yml 파일이 없으면 생성
# (AWS_EC2_DEPLOYMENT.md 문서 참조)
```

---

### **문제 4: 환경 변수 (.env) 오류**
```
Error: environment variable not set
```

**해결 방법:**
```bash
# EC2에서 .env 파일 확인
cat ~/maruni/.env

# 필수 환경 변수 누락 시 추가
nano ~/maruni/.env

# 컨테이너 재시작
cd ~/maruni
docker compose -f docker-compose.prod.yml restart app
```

---

### **문제 5: 헬스체크 실패**
```
Error: Application health check failed
```

**해결 방법:**
```bash
# EC2에서 로그 확인
cd ~/maruni
docker compose -f docker-compose.prod.yml logs -f app

# 일반적인 원인:
# - DB 연결 실패
# - Redis 연결 실패
# - OpenAI API Key 오류
# - 메모리 부족

# DB/Redis 상태 확인
docker compose -f docker-compose.prod.yml ps

# 전체 재시작
docker compose -f docker-compose.prod.yml restart
```

---

### **문제 6: 이미지 Pull 실패**
```
Error: manifest for kimgyuill/maruni-server:latest not found
```

**해결 방법:**
1. **Docker Hub에서 이미지 확인**
   - https://hub.docker.com/r/kimgyuill/maruni-server
   - `latest` 태그 존재 확인

2. **GitHub Actions 로그 확인**
   - Docker 이미지 빌드 단계 확인
   - Push 성공 여부 확인

3. **수동으로 이미지 Push**
   ```bash
   # 로컬 PC에서
   cd C:\Users\rlarb\coding\maruni\maruni-server
   docker build -t kimgyuill/maruni-server:latest .
   docker push kimgyuill/maruni-server:latest
   ```

---

## 6. 배포 모니터링

### **6.1 GitHub Actions 로그 확인**
```
https://github.com/Kimgyuilli/maruni/actions
```
- 각 단계별 실행 시간 확인
- 에러 로그 확인
- 배포 히스토리 추적

### **6.2 EC2 서버 로그 확인**
```bash
# SSH 접속
ssh ubuntu@api.maruni.kro.kr

# 애플리케이션 로그
cd ~/maruni
docker compose -f docker-compose.prod.yml logs -f app

# 컨테이너 상태
docker compose -f docker-compose.prod.yml ps

# 리소스 사용량
docker stats
```

### **6.3 헬스체크 모니터링**
```bash
# API 헬스체크
curl https://api.maruni.kro.kr/actuator/health

# Swagger UI
https://api.maruni.kro.kr/swagger-ui/index.html

# 실제 API 테스트 (예: 회원 가입)
curl -X POST https://api.maruni.kro.kr/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234","name":"테스트"}'
```

---

## 7. 보안 권장 사항

### **7.1 GitHub Secrets 보안**
- ✅ Secrets는 암호화되어 저장됨
- ✅ Secrets는 GitHub Actions에서만 접근 가능
- ✅ 로그에 Secrets 값이 자동으로 마스킹됨
- ❌ Secrets를 코드나 커밋 메시지에 포함하지 않기

### **7.2 SSH Key 보안**
- ✅ Private Key는 절대 공개하지 않기
- ✅ EC2 보안 그룹에서 SSH 포트 제한 (필요시)
- ✅ SSH Key 주기적 교체 권장 (6개월마다)

### **7.3 Docker Hub Token 보안**
- ✅ Access Token은 비밀번호보다 안전
- ✅ Token 만료 시 재생성 필요
- ✅ Read/Write/Delete 권한만 부여 (Admin 불필요)

---

## 8. 자주 사용하는 명령어

### **8.1 로컬에서 수동 배포**
```bash
# 로컬 PC에서 이미지 빌드 및 푸시
cd C:\Users\rlarb\coding\maruni\maruni-server
docker build -t kimgyuill/maruni-server:latest .
docker push kimgyuill/maruni-server:latest

# EC2에서 배포
ssh ubuntu@api.maruni.kro.kr
cd ~/maruni
docker pull kimgyuill/maruni-server:latest
docker compose -f docker-compose.prod.yml up -d app
```

### **8.2 롤백 (이전 버전으로 복구)**
```bash
# EC2에서 특정 커밋 SHA 이미지로 롤백
ssh ubuntu@api.maruni.kro.kr
cd ~/maruni

# 이전 커밋 SHA 확인 (GitHub Commit 히스토리)
docker pull kimgyuill/maruni-server:<previous-commit-sha>

# docker-compose.prod.yml 임시 수정
nano docker-compose.prod.yml
# app > image: kimgyuill/maruni-server:<previous-commit-sha>

# 재시작
docker compose -f docker-compose.prod.yml up -d app

# 확인 후 원래대로 복구
```

### **8.3 긴급 중지**
```bash
# EC2에서 애플리케이션 중지
ssh ubuntu@api.maruni.kro.kr
cd ~/maruni
docker compose -f docker-compose.prod.yml stop app

# 재시작
docker compose -f docker-compose.prod.yml start app
```

---

## ✅ CI/CD 설정 완료 체크리스트

### **GitHub Secrets 설정**
- [ ] `DOCKERHUB_USERNAME` 등록 완료
- [ ] `DOCKERHUB_TOKEN` 등록 완료 (Access Token)
- [ ] `EC2_HOST` 등록 완료 (`api.maruni.kro.kr`)
- [ ] `EC2_USERNAME` 등록 완료 (`ubuntu`)
- [ ] `EC2_SSH_KEY` 등록 완료 (Private Key 전체 내용)

### **워크플로우 파일**
- [ ] `.github/workflows/ci.yml` 존재 확인
- [ ] `.github/workflows/deploy.yml` 생성 완료

### **EC2 서버 준비**
- [ ] `~/maruni/docker-compose.prod.yml` 존재
- [ ] `~/maruni/.env` 파일 설정 완료
- [ ] Docker 및 Docker Compose 설치 완료
- [ ] SSH 접속 가능 확인

### **배포 테스트**
- [ ] Pull Request 생성 → CI 테스트 통과 확인
- [ ] main 브랜치 merge → CD 자동 배포 확인
- [ ] 헬스체크 통과 (`/actuator/health`)
- [ ] Swagger UI 접속 가능 (`/swagger-ui/index.html`)
- [ ] 실제 API 동작 확인

---

**CI/CD 구축 완료!** 🎉

이제 `main` 브랜치에 push할 때마다 자동으로 테스트, 빌드, 배포가 진행됩니다.
