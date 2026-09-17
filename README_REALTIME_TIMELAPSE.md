# BlueMap - Real-Time Block Tracker & Chronological Timelapse

이 브랜치는 **BlueMap (NeoForge 1.21.1)**에 **실시간 블록 설치 감지(Real-Time Block Placement Tracking)** 기능과 웹 브라우저에서 서버 시작부터 현재까지의 건축 과정을 시간순으로 되돌려보고 재생할 수 있는 **인터랙티브 타임랩스(Chronological Timelapse UI)** 기능을 추가한 확장 버전입니다.

---

## 🌟 핵심 신규 기능

### 1. 실시간 블록 트래커 (Real-Time Block Tracker)
- **이벤트 캡처**: NeoForge의 `BlockEvent.EntityPlaceEvent` 및 `BlockEvent.BreakEvent`를 수신하여 플레이어가 설치하거나 파괴한 블록 정보를 실시간 캡처합니다.
- **메모리 링 버퍼**: `BlockTracker`를 통해 서버 시작 시각, 단조 증가 시퀀스 번호(`seq`), 밀리초 타임스탬프(`t`), 좌표(`x, y, z`), 블록 ID(`b`), 플레이어 이름(`p`), 액션(`place/break`)을 고성능 링 버퍼에 저장합니다.
- **REST API 엔드포인트**:
  - `GET /maps/{mapId}/live/blocks.json?since={seq}`: 마지막으로 수신한 시퀀스 번호 이후의 새 블록 이벤트만 효율적으로 폴링합니다.
  - `GET /maps/{mapId}/live/blocks.json?sim={count}`: 인게임 클라이언트 접속 없이도 실시간 블록 설치 시뮬레이션을 테스트할 수 있습니다.

### 2. 고성능 3D 블록 렌더링 (Three.js InstancedMesh)
- **단일 드로우 콜(Single Draw Call)**: 최대 50,000개의 블록을 단 하나의 `InstancedMesh`로 렌더링하여 모바일 환경에서도 60fps 이상의 부드러운 성능을 유지합니다.
- **선명한 고유 색상 (`MeshBasicMaterial`)**:
  - BlueMap의 지형 셰이더 특성상 기본 씬 조명이 없는 환경에 맞춰, 조명 연산에 의존하지 않는 `MeshBasicMaterial`을 적용했습니다.
  - 다이아몬드, 에메랄드, 금, 레드스톤, 청금석, 구리, 유리 등 마인크래프트 주요 블록의 고유 색상 매핑을 내장하여 어둡지 않고 선명한 색상으로 표시됩니다.
- **네온 펄스 애니메이션 (`spawnPulse`)**: 블록이 새로 설치될 때 해당 위치에 밝은 청록색 와이어프레임 박스가 생성되어 부드럽게 확대되며 페이드아웃됩니다.

### 3. 페이지 새로고침 없는 실시간 동기화 (Auto-Redraw & Sync)
- **자동 렌더러 깨우기(Auto-Redraw)**: BlueMap의 절전 렌더 루프를 새 블록 수신 시 자동으로 깨워, 사용자가 화면을 터치하거나 브라우저를 새로고침하지 않아도 3D 지형 위에 블록이 즉시 나타납니다.
- **원터치 `🔄 동기화` 버튼**:
  - 타임랩스 패널의 `🔄 동기화` 버튼을 누르면 **타일 캐시 해시 무효화 + 지형 타일 재로딩 + 영토 마커 갱신 + 블록 기록 갱신**이 웹페이지 새로고침 없이 1초 만에 일괄 수행됩니다.

### 4. 모바일 최적화 타임랩스 컨트롤러 (`TimelapseBar.vue`)
- **안전 영역(Safe Area) 대응**: 스마트폰 하단 제스처 바에 가려지지 않도록 `calc(14px + env(safe-area-inset-bottom))` 적용.
- **모바일 반응형 2열 컨트롤 레이아웃**:
  - 1열: `⏮ 처음으로`, `▶ 재생 / ⏸ 일시정지` (대형 엄지 터치 버튼), `🔴 실시간`, `🎯 시점 추적`
  - 2열: `1x`, `2x`, `5x`, `10x`, `25x` 균등 배속 셀렉터
- **터치 친화적 슬라이더**: 20px 대형 조절 썸(Thumb)과 네온 프로그레스 바를 통해 서버 시작부터 현재까지 원하는 시점으로 정밀하게 스크러빙(Scrubbing)할 수 있습니다.
- **미니 플로팅 위젯**: 패널을 닫아도 `⏱ 타임랩스 [블록수] 🔴`와 `🔄` 미니 동기화 버튼이 우측 하단에 상시 표시됩니다.

---

## 📡 HTTP REST API 사양

### `GET /maps/{mapId}/live/blocks.json`

#### 요청 파라미터
| 파라미터 | 타입 | 설명 |
| :--- | :--- | :--- |
| `since` | integer | (선택) 지정한 시퀀스 번호 이후의 새 이벤트만 반환 (폴링용) |
| `sim` | integer | (선택) 1~500개의 랜덤 블록 설치 이벤트를 시뮬레이션 생성 |

#### 응답 예시
```json
{
  "serverStartTime": 1789609615885,
  "currentTime": 1789610285843,
  "latestSeq": 70,
  "events": [
    {
      "seq": 68,
      "t": 1789610688120,
      "x": -10,
      "y": 64,
      "z": -1,
      "b": "minecraft:gold_block",
      "a": "place",
      "p": "Hero"
    },
    {
      "seq": 69,
      "t": 1789610689150,
      "x": -16,
      "y": 64,
      "z": -9,
      "b": "minecraft:glass",
      "a": "place",
      "p": "Admin"
    }
  ]
}
```

---

## 🛠️ 빌드 및 설치 방법

### 1. 사전 요구사항
- Java 21 (JDK 21)
- Node.js 20+ (웹앱 빌드용)

### 2. 빌드
```bash
# 1. 웹앱 빌드
cd common/webapp
npm ci
npm run build
cd ../..

# 2. NeoForge 모드 JAR 빌드
./gradlew spotlessApply --no-daemon
./gradlew :neoforge:mergeShadowAndJarJar :neoforge:release --no-daemon
```

컴파일된 JAR 파일은 `build/release/bluemap-neoforge-1.21.1.jar`에 생성됩니다.

### 3. 실시간 블록 스트리밍 테스트 스크립트
PC 마인크래프트 클라이언트가 없는 환경에서도 서버의 RCON 및 BlueMap 엔드포인트를 통해 실시간 블록 설치를 테스트할 수 있습니다:
```bash
python3 /root/server/stream_blocks.py [블록수] [간격(초)]
# 예: 20개 블록을 1.5초마다 1개씩 설치
python3 /root/server/stream_blocks.py 20 1.5
```
