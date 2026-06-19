# Spring Plus

Todo 도메인을 바탕으로 기존 기능을 개선하고 도전 기능을 확장한 Spring Boot 과제 프로젝트입니다.

Spring Data JPA, QueryDSL, Spring Security, JWT, WebSocket/STOMP 등을 실제 기능에 적용하며 설계 기준과 동작 원리를 학습했습니다.

## 주요 구현 내용

- Todo 목록·검색 조건을 목적별 DTO로 분리하고 날짜 범위 및 페이징 검증 개선
- QueryDSL 기반 제목, 생성일, 담당자 닉네임 동적 검색과 집계 결과 조회
- fetch join을 활용한 Todo 단건 조회의 N+1 문제 개선
- 사용자 닉네임 추가와 JWT 인증 정보 확장
- Spring Security 필터 체인과 `SecurityContext` 기반 인증·인가 적용
- AOP를 활용한 매니저 등록 성공·실패 감사 로그 기록
- 독립 트랜잭션(`REQUIRES_NEW`)을 통한 실패 요청 로그 보존
- Todo와 Manager·Comment의 생명주기에 따른 JPA Cascade 설정
- WebSocket/STOMP 및 Redis Pub/Sub 기반 Todo 참여자 실시간 채팅
- 닉네임 인덱스 적용 전후 검색 성능 검증

## 검색 성능 개선 결과

사용자 닉네임 검색의 병목을 확인하기 위해 MySQL에 사용자 100만 건을 생성하고, 인덱스 구성과 조회 방식을 단계별로 비교했습니다.

### 테스트 데이터 생성

| 생성 건수 | Batch Size | 삽입 시간 | 닉네임 중복 |
| ---: | ---: | ---: | ---: |
| 1,000,000건 | 10,000건 | 51,051ms | 0건 |

### 인덱스 적용 전후 비교

| 방식 | 평균 | 최소 | 최대 | 실행 계획 |
| --- | ---: | ---: | ---: | --- |
| 인덱스 없음 | 792.936ms | 633.646ms | 1,267.231ms | 100만 행 Table Scan |
| 일반 인덱스 | 3.014ms | 2.411ms | 3.769ms | Index Lookup |
| 커버링 인덱스 | 3.084ms | 2.538ms | 3.719ms | Covering Index Lookup |

일반 인덱스를 적용해 평균 조회 속도가 **약 263.07배 개선**되었습니다.

```text
인덱스 없음   ████████████████████████████████████████  792.936ms
일반 인덱스   ▏                                           3.014ms
커버링 인덱스 ▏                                           3.084ms
```

### 조회 방식 비교

| 방식 | 평균 | 최소 | 최대 |
| --- | ---: | ---: | ---: |
| JPA 엔티티 전체 조회 | 8.163ms | 5.215ms | 13.326ms |
| DTO Projection | 4.344ms | 3.424ms | 6.785ms |

필요한 컬럼만 조회하는 DTO Projection을 적용해 평균 조회 속도가 **약 1.88배 개선**되었습니다.

### 최종 판단

| 방법 | 결정 | 이유 |
| --- | --- | --- |
| 닉네임 일반 인덱스 | 적용 | 전체 스캔을 단일 인덱스 조회로 개선 |
| DTO Projection | 적용 | 필요한 컬럼만 조회 |
| 커버링 인덱스 | 제외 | 일반 인덱스와 성능 차이가 거의 없음 |
| Redis 캐시 | 제외 | 반복 조회 가능성이 낮고 관리 비용이 큼 |
| Unique 인덱스 | 보류 | 닉네임 중복 허용 정책이 확정되지 않음 |

## 기술 스택

- Java 17
- Spring Boot 3.3.3
- Spring Data JPA
- QueryDSL
- Spring Security
- JWT
- H2 / MySQL
- WebSocket / STOMP
- Redis
- Gradle

## 문서

- [GitHub Wiki](https://github.com/w00lam/spring-plus/wiki)
- [Spring Boot Todo 과제 필수 기능 구현 중 고민한 부분](https://w00lam.github.io/posts/spring-todo-retrospective/)
- [Spring Boot 도전 기능 구현 과정에서 고민한 설계 포인트](https://w00lam.github.io/posts/spring-challenge-feature-design-retrospective/)

README는 프로젝트를 빠르게 파악하기 위한 요약 문서입니다. 주요 설계 판단과 학습 내용은 Wiki에서, 자세한 구현 회고는 블로그에서 확인할 수 있습니다.

## 실행 방법

### macOS / Linux

```bash
./gradlew clean build
./gradlew bootRun
```

### Windows

```powershell
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

애플리케이션 실행과 전체 테스트에는 Redis가 필요합니다. MySQL 기반 `local` 프로필을 사용할 때는 필요한 환경 변수를 설정한 뒤 다음 명령으로 인프라와 애플리케이션을 실행합니다.

```bash
docker compose up -d
./gradlew bootRun --args="--spring.profiles.active=local"
```
