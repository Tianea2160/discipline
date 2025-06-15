# Discord 오류 알림 설정 가이드

## 1. Discord 웹훅 생성

### 1.1 Discord 서버에서 웹훅 생성
1. Discord 서버의 채널에서 우클릭 → "채널 편집"
2. "연동" 탭 → "웹훅" → "웹훅 만들기"
3. 웹훅 이름 설정 (예: "서버 오류 알림")
4. "웹훅 URL 복사" 클릭하여 URL 복사

### 1.2 환경 변수 설정
```bash
# 환경 변수로 설정
export DISCORD_WEBHOOK_URL="https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
export DISCORD_WEBHOOK_ENABLED=true

# 또는 application.yml에 직접 설정
discord:
  webhook:
    url: "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL"
    enabled: true
```

## 2. 기능 설명

### 2.1 자동 알림 대상
- `RuntimeException` 및 하위 예외들
- `Exception` (일반적인 예외)
- HTTP 500 Internal Server Error 발생 시

### 2.2 알림에 포함되는 정보
- 🔥 오류 타입 및 메시지
- 📅 발생 시간
- 🌐 요청 URI
- 👤 사용자 ID (로그인된 경우)
- 🖥️ User Agent
- 📋 스택 트레이스 (상위 5줄)

### 2.3 테스트 엔드포인트
```
GET /api/test/error          - RuntimeException 테스트
GET /api/test/null-pointer   - NullPointerException 테스트
GET /api/test/illegal-argument - IllegalArgumentException 테스트 (400 오류)
GET /api/test/success        - 정상 응답 테스트
```

## 3. 사용 방법

### 3.1 개발 환경에서 테스트
```bash
# 애플리케이션 실행
./gradlew bootRun

# 오류 테스트
curl http://localhost:8080/api/test/error
```

### 3.2 프로덕션 환경 설정
```bash
# Docker 환경 변수
docker run -e DISCORD_WEBHOOK_URL="your_webhook_url" -e DISCORD_WEBHOOK_ENABLED=true your-app

# Kubernetes ConfigMap/Secret
apiVersion: v1
kind: Secret
metadata:
  name: discord-config
data:
  webhook-url: <base64-encoded-webhook-url>
```

## 4. 보안 고려사항

### 4.1 웹훅 URL 보안
- 웹훅 URL은 민감한 정보이므로 환경 변수나 Secret으로 관리
- 코드에 하드코딩하지 않기
- 로그에 웹훅 URL이 출력되지 않도록 주의

### 4.2 알림 제한
- 스택 트레이스는 상위 5줄만 포함 (보안상 민감한 정보 노출 방지)
- 오류 메시지는 500자로 제한
- User Agent는 100자로 제한

## 5. 문제 해결

### 5.1 알림이 전송되지 않는 경우
1. `DISCORD_WEBHOOK_ENABLED=true` 설정 확인
2. 웹훅 URL이 올바른지 확인
3. Discord 서버에서 웹훅이 활성화되어 있는지 확인
4. 애플리케이션 로그에서 오류 메시지 확인

### 5.2 로그 확인
```yaml
logging:
  level:
    org.project.discipline.service.DiscordNotificationService: DEBUG
    org.project.discipline.exception.GlobalExceptionHandler: DEBUG
```

## 6. 커스터마이징

### 6.1 알림 메시지 수정
`DiscordNotificationService.createErrorEmbed()` 메서드에서 메시지 형식 변경 가능

### 6.2 추가 예외 타입 처리
`GlobalExceptionHandler`에 새로운 `@ExceptionHandler` 메서드 추가

### 6.3 알림 조건 변경
특정 조건에서만 알림을 보내도록 `sendErrorNotification()` 메서드 수정 가능 