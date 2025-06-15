# 🎯 AI 체크리스트 생성기

Spring AI를 활용한 목표 기반 체크리스트 자동 생성 시스템입니다.

## 🚀 기능

- **AI 기반 체크리스트 생성**: OpenAI GPT를 사용하여 목표에 맞는 구체적인 체크리스트 생성
- **구조화된 응답**: JSON 형식으로 일관된 체크리스트 구조 제공
- **우선순위 설정**: HIGH/MEDIUM/LOW 우선순위 자동 분류
- **예상 시간 계산**: 각 작업별 예상 소요 시간 제공
- **예외 처리**: AI 응답 실패 시 폴백 체크리스트 제공
- **웹 인터페이스**: 사용자 친화적인 웹 UI 제공
- **테스트 모드**: 인증 없이 테스트할 수 있는 별도 API 제공

## 🧪 빠른 테스트

### 웹 인터페이스로 테스트
```
http://localhost:8080/test/checklist-page
```
- 인증 불필요
- 빠른 테스트 버튼으로 즉시 체크리스트 생성
- 다양한 목표 템플릿 제공

### API로 테스트
```bash
# 샘플 체크리스트
curl http://localhost:8080/test/checklist/sample

# 빠른 테스트 (목표 파라미터 포함)
curl "http://localhost:8080/test/checklist/quick-test?goal=영어공부하기"

# 커스텀 체크리스트 생성
curl -X POST http://localhost:8080/test/checklist/generate \
  -H "Content-Type: application/json" \
  -d '{"date": "2024-01-15", "goal": "운동 루틴 만들기"}'
```

## 📋 API 엔드포인트

### 🧪 테스트용 API (인증 불필요)

#### 1. 빠른 테스트
```http
GET /test/checklist/quick-test?goal={목표}
```

#### 2. 샘플 체크리스트
```http
GET /test/checklist/sample
```

#### 3. 체크리스트 생성
```http
POST /test/checklist/generate
Content-Type: application/json

{
  "date": "2024-01-15",
  "goal": "목표 내용"
}
```

### 🔒 프로덕션 API (인증 필요)

#### 1. 체크리스트 생성
```http
POST /api/checklist/generate
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}

{
  "date": "2024-01-15",
  "goal": "Spring Boot 프로젝트 완성하기"
}
```

**응답 예시:**
```json
{
  "date": "2024-01-15",
  "goal": "Spring Boot 프로젝트 완성하기",
  "items": [
    {
      "task": "프로젝트 요구사항 정리",
      "description": "기능 명세서 작성 및 우선순위 설정",
      "priority": "HIGH",
      "estimatedTime": "1시간"
    },
    {
      "task": "데이터베이스 설계",
      "description": "ERD 작성 및 테이블 구조 설계",
      "priority": "HIGH",
      "estimatedTime": "2시간"
    }
  ],
  "totalTasks": 2,
  "estimatedTotalTime": "총 예상 시간: 1시간, 2시간"
}
```

#### 2. 샘플 체크리스트
```http
GET /api/checklist/sample
Authorization: Bearer {JWT_TOKEN}
```

## 🛠️ 설정

### 1. 의존성 추가
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0-M4")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0-M4")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}
```

### 2. OpenAI API 키 설정
```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-openai-api-key-here}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1000
```

### 3. 환경변수 설정
```bash
export OPENAI_API_KEY=your-actual-openai-api-key
```

### 4. 보안 설정
```kotlin
// SecurityConfig.kt
.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/test/**").permitAll() // 테스트용 API는 인증 불필요
        .anyRequest().authenticated()
}
```

## 🎨 웹 인터페이스

### 테스트용 페이지
- **URL**: `http://localhost:8080/test/checklist-page`
- **특징**: 인증 불필요, 즉시 테스트 가능
- **기능**:
  - 빠른 테스트 (사전 정의된 목표 선택)
  - 샘플 체크리스트 생성
  - 커스텀 체크리스트 생성
  - 실시간 API 호출 및 결과 표시

### 프로덕션 페이지
- **URL**: `http://localhost:8080/checklist`
- **특징**: 인증 필요
- **기능**: 완전한 체크리스트 생성 기능

## 🔧 주요 컴포넌트

### 1. ChecklistAiService
- AI 프롬프트 생성 및 관리
- OpenAI API 호출
- 응답 파싱 및 검증
- 폴백 처리

### 2. 컨트롤러 구조
- **TestChecklistController**: 테스트용 API (`/test/checklist/*`)
- **ChecklistController**: 프로덕션 API (`/api/checklist/*`)
- **ChecklistViewController**: 웹 페이지 제공

### 3. 예외 처리
- `ChecklistGenerationException`: AI 생성 실패
- `InvalidChecklistFormatException`: 형식 오류
- 전역 예외 처리기를 통한 일관된 오류 응답

### 4. 프롬프트 엔지니어링
```kotlin
private fun createPrompt(request: ChecklistRequest): String {
    return """
        당신은 목표 달성을 위한 체크리스트 생성 전문가입니다.
        
        주어진 정보:
        - 날짜: ${dateStr}
        - 목표: ${request.goal}
        
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

## 🧪 테스트 시나리오

### 1. 기본 테스트
```bash
# 1. 애플리케이션 실행
./gradlew bootRun

# 2. 샘플 테스트
curl http://localhost:8080/test/checklist/sample

# 3. 빠른 테스트
curl "http://localhost:8080/test/checklist/quick-test?goal=독서계획세우기"

# 4. 웹 인터페이스 테스트
# 브라우저에서 http://localhost:8080/test/checklist-page 접속
```

### 2. 다양한 목표 테스트
```bash
# 학습 관련
curl "http://localhost:8080/test/checklist/quick-test?goal=영어공부하기"

# 건강 관련
curl "http://localhost:8080/test/checklist/quick-test?goal=운동루틴만들기"

# 업무 관련
curl "http://localhost:8080/test/checklist/quick-test?goal=프로젝트완성하기"
```

### 3. 커스텀 테스트
```bash
curl -X POST http://localhost:8080/test/checklist/generate \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2024-01-15",
    "goal": "새로운 기술 스택 학습하기"
  }'
```

## 🔍 로깅

체크리스트 생성 과정의 상세한 로그를 확인할 수 있습니다:

```yaml
logging:
  level:
    org.project.discipline.service.ChecklistAiService: DEBUG
    org.project.discipline.controller.TestChecklistController: INFO
```

## 🚨 주의사항

1. **OpenAI API 키**: 실제 OpenAI API 키가 필요합니다
2. **비용**: API 호출 시 비용이 발생할 수 있습니다
3. **응답 시간**: AI 응답에 따라 처리 시간이 달라질 수 있습니다
4. **폴백 처리**: AI 실패 시 기본 체크리스트가 제공됩니다
5. **테스트 환경**: `/test/**` 경로는 보안이 적용되지 않으므로 프로덕션에서 주의

## 🔄 확장 가능성

- **다양한 AI 모델 지원**: Claude, Gemini 등 추가 가능
- **템플릿 시스템**: 목표 유형별 맞춤 템플릿
- **사용자 맞춤화**: 개인 선호도 반영
- **히스토리 관리**: 생성된 체크리스트 저장 및 관리
- **협업 기능**: 팀 체크리스트 공유
- **A/B 테스트**: 다양한 프롬프트 전략 비교 