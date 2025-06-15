# 🎯 Discipline - AI 기반 목표 관리 시스템

Spring Boot와 AI를 활용한 개인 목표 관리 및 체크리스트 자동 생성 플랫폼입니다.

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [설치 및 실행](#-설치-및-실행)
- [API 문서](#-api-문서)
- [인증 시스템](#-인증-시스템)
- [AI 체크리스트 생성](#-ai-체크리스트-생성)
- [데이터베이스 설계](#-데이터베이스-설계)
- [환경 설정](#-환경-설정)
- [테스트](#-테스트)
- [배포](#-배포)

## 🎯 프로젝트 개요

Discipline은 개인의 목표 달성을 돕는 AI 기반 플랫폼입니다. 사용자가 설정한 목표를 바탕으로 OpenAI GPT를 활용하여 구체적이고 실행 가능한 체크리스트를 자동 생성합니다.

### 핵심 가치
- **AI 기반 개인화**: 각 사용자의 목표에 맞춤화된 체크리스트 생성
- **실행 가능성**: 하루 안에 완료 가능한 현실적인 작업 제안
- **체계적 관리**: 우선순위와 예상 시간을 포함한 구조화된 계획
- **사용자 경험**: 직관적인 웹 인터페이스와 RESTful API 제공

## 🚀 주요 기능

### 🤖 AI 체크리스트 생성
- **OpenAI GPT 연동**: 목표 기반 맞춤형 체크리스트 자동 생성
- **구조화된 응답**: JSON 형식의 일관된 체크리스트 구조
- **우선순위 분류**: HIGH/MEDIUM/LOW 자동 우선순위 설정
- **시간 예측**: 각 작업별 예상 소요 시간 제공
- **폴백 시스템**: AI 실패 시 기본 체크리스트 제공

### 🔐 인증 및 보안
- **OAuth2 소셜 로그인**: Google 계정 연동
- **JWT 토큰 인증**: 안전한 API 접근 관리
- **사용자 컨텍스트**: 요청별 사용자 정보 자동 주입
- **권한 관리**: 역할 기반 접근 제어

### 📊 데이터 관리
- **체크리스트 히스토리**: 생성된 모든 체크리스트 추적
- **사용자별 통계**: 개인 활동 분석
- **실시간 상태 관리**: 체크리스트 생성 과정 모니터링
- **Discord 알림**: 시스템 이벤트 실시간 알림

### 🌐 웹 인터페이스
- **테스트 페이지**: 인증 없이 즉시 테스트 가능
- **프로덕션 UI**: 완전한 기능을 갖춘 사용자 인터페이스
- **반응형 디자인**: 모바일 및 데스크톱 지원
- **실시간 피드백**: API 호출 결과 즉시 표시

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.0
- **Language**: Kotlin 1.9.25
- **JVM**: Java 21
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA + Hibernate
- **Security**: Spring Security + OAuth2 + JWT
- **AI**: Spring AI + OpenAI GPT-3.5-turbo

### Frontend
- **Template Engine**: Thymeleaf
- **Styling**: CSS3 + Bootstrap
- **JavaScript**: Vanilla JS (ES6+)
- **API Documentation**: Swagger/OpenAPI 3

### Infrastructure
- **Build Tool**: Gradle (Kotlin DSL)
- **Container**: Docker + Docker Compose
- **Monitoring**: Spring Boot Actuator
- **Logging**: SLF4J + Logback
- **Notification**: Discord Webhook

## 📁 프로젝트 구조

```
src/main/kotlin/org/project/discipline/
├── 📁 annotation/              # 커스텀 어노테이션
│   └── CurrentUserInfo.kt      # 사용자 정보 주입 어노테이션
├── 📁 aspect/                  # AOP 관련
│   └── UserContextAspect.kt    # 사용자 컨텍스트 처리
├── 📁 config/                  # 설정 클래스
│   ├── CurrentUserArgumentResolver.kt  # 사용자 정보 리졸버
│   ├── JwtConfig.kt           # JWT 설정
│   ├── SecurityConfig.kt      # 보안 설정
│   └── WebConfig.kt           # 웹 설정
├── 📁 controller/              # REST 컨트롤러
│   ├── AuthController.kt      # 인증 관련 API
│   ├── ChecklistController.kt # 체크리스트 API (인증 필요)
│   ├── TestChecklistController.kt # 테스트 API (인증 불필요)
│   └── UserController.kt      # 사용자 관리 API
├── 📁 domain/                  # 도메인 모델
│   ├── 📁 checklist/          # 체크리스트 도메인
│   │   ├── 📁 dto/            # 데이터 전송 객체
│   │   │   ├── ChecklistItem.kt
│   │   │   ├── ChecklistRequest.kt
│   │   │   └── ChecklistResponse.kt
│   │   ├── 📁 entity/         # JPA 엔티티
│   │   │   └── ChecklistEntity.kt
│   │   ├── 📁 repository/     # 데이터 접근 계층
│   │   │   └── ChecklistRepository.kt
│   │   └── 📁 service/        # 비즈니스 로직
│   │       └── ChecklistService.kt
│   ├── 📁 common/             # 공통 도메인
│   │   └── 📁 entity/
│   │       ├── BaseAuditEntity.kt
│   │       └── BaseTimeEntity.kt
│   └── 📁 user/               # 사용자 도메인
│       ├── 📁 dto/
│       │   └── CurrentUser.kt
│       ├── 📁 entity/
│       │   └── User.kt
│       ├── 📁 repository/
│       │   └── UserRepository.kt
│       └── 📁 service/
│           ├── CustomUserDetailsService.kt
│           └── UserContextService.kt
├── 📁 exception/              # 예외 처리
│   ├── ChecklistException.kt  # 체크리스트 관련 예외
│   └── GlobalExceptionHandler.kt # 전역 예외 처리기
├── 📁 notification/           # 알림 시스템
│   └── DiscordNotificationService.kt
├── 📁 security/               # 보안 관련
│   ├── JwtAuthenticationFilter.kt
│   ├── OAuth2LoginSuccessHandler.kt
│   └── 📁 service/
│       ├── JwtService.kt
│       └── OAuth2JwtService.kt
└── DisciplineApplication.kt   # 메인 애플리케이션
```

### 리소스 구조
```
src/main/resources/
├── 📁 static/                 # 정적 리소스
├── 📁 templates/              # Thymeleaf 템플릿
├── application.yml            # 기본 설정
└── application-local.yml      # 로컬 환경 설정
```

## 🚀 설치 및 실행

### 사전 요구사항
- Java 21+
- PostgreSQL 12+
- OpenAI API Key
- Google OAuth2 클라이언트 ID/Secret (선택사항)

### 1. 저장소 클론
```bash
git clone https://github.com/your-username/discipline.git
cd discipline
```

### 2. 데이터베이스 설정
```bash
# Docker Compose로 PostgreSQL 실행
docker-compose up -d

# 또는 직접 PostgreSQL 설치 후 데이터베이스 생성
createdb discipline-db
```

### 3. 환경 변수 설정
```bash
# .env 파일 생성 또는 환경 변수 설정
export OPENAI_API_KEY=your-openai-api-key
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export DISCORD_WEBHOOK_URL=your-discord-webhook-url
```

### 4. 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar build/libs/discipline-0.0.1-SNAPSHOT.jar
```

### 5. 접속 확인
- **애플리케이션**: http://localhost:8080
- **API 문서**: http://localhost:8080/swagger-ui.html
- **테스트 페이지**: http://localhost:8080/test/checklist-page

## 📚 API 문서

### 🧪 테스트 API (인증 불필요)

#### 빠른 체크리스트 생성
```http
POST /test/checklist/generate
Content-Type: application/json

{
  "goal": "영어 공부하기",
  "context": "토익 점수 향상을 위한 하루 계획",
  "date": "2024-01-15"
}
```

#### 샘플 체크리스트
```http
GET /test/checklist/generate/sample
```

#### 목표 템플릿 조회
```http
GET /api/checklist/templates
```

### 🔒 프로덕션 API (JWT 인증 필요)

#### 체크리스트 생성
```http
POST /api/checklist/generate
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "goal": "Spring Boot 프로젝트 완성하기",
  "context": "REST API 개발 및 테스트",
  "date": "2024-01-15"
}
```

#### 사용자 정보 조회
```http
GET /api/auth/me
Authorization: Bearer {JWT_TOKEN}
```

### 응답 예시
```json
{
  "date": "2024-01-15",
  "goal": "영어 공부하기",
  "items": [
    {
      "task": "단어 암기",
      "description": "토익에서 자주 나오는 어휘 위주로 단어 암기",
      "priority": "HIGH",
      "estimatedTime": "30분"
    },
    {
      "task": "문법 학습",
      "description": "토익 문법 문제 해석 및 학습",
      "priority": "HIGH",
      "estimatedTime": "1시간"
    }
  ],
  "totalTasks": 2,
  "estimatedTotalTime": "총 예상 시간: 30분, 1시간"
}
```

## 🔐 인증 시스템

### OAuth2 소셜 로그인
1. **Google 로그인**: `/oauth2/authorization/google`
2. **JWT 토큰 발급**: 로그인 성공 시 자동 발급
3. **사용자 정보 저장**: 데이터베이스에 사용자 정보 자동 저장

### JWT 토큰 사용
```bash
# 헤더에 토큰 포함
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 사용자 컨텍스트 주입
```kotlin
@PostMapping("/api/checklist/generate")
fun generateChecklist(
    @RequestBody request: ChecklistRequest,
    @CurrentUserInfo currentUser: CurrentUser  // 자동 주입
): ResponseEntity<ChecklistResponse>
```

## 🤖 AI 체크리스트 생성

### 프롬프트 엔지니어링
```kotlin
private fun createPrompt(request: ChecklistRequest, targetDate: LocalDate): String {
    return """
        당신은 목표 달성을 위한 체크리스트 생성 전문가입니다.
        주어진 정보:
        - 날짜: ${targetDate.format(dateFormatter)}
        - 목표: ${request.goal}
        - 추가 정보: ${request.context ?: ""}
        
        규칙:
        1. 3-7개의 실행 가능한 작업으로 구성
        2. 우선순위를 명확히 설정 (HIGH: 필수, MEDIUM: 중요, LOW: 선택)
        3. 각 작업은 구체적이고 측정 가능해야 함
        4. 하루 안에 완료 가능한 현실적인 작업들로 구성
        5. 반드시 유효한 JSON 배열 형식으로만 응답
        
        목표에 맞는 체크리스트를 JSON 형식으로만 응답해주세요:
    """.trimIndent()
}
```

### AI 모델 설정
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1500
```

### 폴백 시스템
AI 응답 실패 시 기본 체크리스트를 제공하여 서비스 연속성을 보장합니다.

## 🗄️ 데이터베이스 설계

### 주요 테이블

#### users (사용자)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    picture VARCHAR(500),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### checklists (체크리스트)
```sql
CREATE TABLE checklists (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    target_date DATE NOT NULL,
    goal VARCHAR(1000) NOT NULL,
    checklist_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500),
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    modified_by VARCHAR(255)
);
```

### 엔티티 관계
- **User** ↔ **ChecklistEntity**: 1:N 관계
- **BaseAuditEntity**: 생성/수정 정보 자동 관리
- **BaseTimeEntity**: 시간 정보 자동 관리

## ⚙️ 환경 설정

### application-local.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/discipline-db
    username: test1234
    password: test1234
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1500
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

discord:
  webhook:
    url: ${DISCORD_WEBHOOK_URL}
    enabled: true

logging:
  level:
    org.project.discipline: INFO
    org.hibernate.SQL: DEBUG
```

### Docker Compose
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: discipline-db
      POSTGRES_USER: test1234
      POSTGRES_PASSWORD: test1234
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## 🧪 테스트

### 단위 테스트
```bash
./gradlew test
```

### API 테스트
```bash
# 테스트 API (인증 불필요)
curl -X POST http://localhost:8080/test/checklist/generate \
  -H "Content-Type: application/json" \
  -d '{"goal": "영어 공부하기", "context": "토익 준비"}'

# 샘플 테스트
curl http://localhost:8080/test/checklist/generate/sample
```

### 웹 인터페이스 테스트
1. http://localhost:8080/test/checklist-page 접속
2. 빠른 테스트 버튼 클릭
3. 커스텀 목표 입력 후 생성

## 🚀 배포

### JAR 빌드
```bash
./gradlew build
java -jar build/libs/discipline-0.0.1-SNAPSHOT.jar
```

### Docker 빌드
```bash
# Dockerfile 생성 후
docker build -t discipline-app .
docker run -p 8080:8080 discipline-app
```

### 환경별 설정
- **개발**: `application-local.yml`
- **테스트**: `application-test.yml`
- **운영**: `application-prod.yml`

## 📊 모니터링

### Spring Boot Actuator
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`

### Discord 알림
시스템 이벤트 및 오류를 Discord 채널로 실시간 알림

### 로깅
- **애플리케이션 로그**: SLF4J + Logback
- **SQL 로그**: Hibernate SQL 로깅
- **보안 로그**: Spring Security 이벤트

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 연락처

프로젝트 관련 문의사항이나 버그 리포트는 GitHub Issues를 통해 제출해 주세요.

---

**Discipline** - AI와 함께하는 스마트한 목표 관리 🎯 