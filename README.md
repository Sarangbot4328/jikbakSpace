# 직박구리 스페이스 모바일 1.0

참조 앱과 같은 크기의 하단 3탭 UI를 사용하는 Android 앱입니다.

- `AVSee`: 설정한 사이트를 WebView로 탐색하고 영상 요청을 감지해 다운로드합니다.
- `다운로드`: 저장한 영상의 썸네일·용량·메타데이터를 보고 재생하거나 삭제합니다.
- `설정`: 바뀐 AVSee 주소, 저장 위치, 활성화 사용자명과 버전을 확인합니다.

## 사용 순서

1. 앱을 빌드할 때 새 Telegram 봇 토큰과 관리자 채팅 ID를 설정합니다.
2. 첫 실행에서 사용자명을 한 번 등록합니다.
3. 관리자 채팅에서 `/우동모바일 사용자명 (비밀번호)` 명령을 보냅니다.
4. 앱에 나타난 암호 입력란에 같은 비밀번호를 입력합니다.
5. AVSee 탭에서 영상 게시물을 열고 영상을 재생합니다.
6. `영상 다운로드`를 누르면 앱 전용 Movies/avsee 폴더에 저장됩니다.

MP4·WebM·M4V·MOV 직접 영상 요청을 감지합니다. 현재 1.0에서는 HLS 전용 `.m3u8` 영상 다운로드를 지원하지 않습니다. 영상 주소가 보이지 않으면 재생 버튼을 누르고 2~3초 후 다시 시도하세요.

## 새 Telegram 봇 설정

BotFather에서 새 봇을 만든 뒤 아래 값을 GitHub 저장소의 `Settings → Secrets and variables → Actions`에 Repository secret으로 저장합니다.

- `TELEGRAM_BOT_TOKEN`: 새 봇 토큰
- `TELEGRAM_ADMIN_CHAT_ID`: 활성화 명령을 보낼 관리자 채팅의 숫자 ID
- `ANDROID_KEYSTORE_BASE64`: 업데이트에 계속 사용할 고정 PKCS12 서명키의 Base64 내용

봇 토큰을 소스 파일에 직접 적지 마세요. 이 앱은 Telegram `getUpdates`를 사용하므로 활성화할 때 같은 봇을 폴링하는 다른 앱이 동시에 실행되지 않아야 합니다.

## GitHub Actions에서 APK 만들기

이 `APK` 폴더의 내용이 GitHub 저장소 최상위에 오도록 업로드합니다. Actions의 `Build DDMJ Space Android APK`를 실행하면 `ddmj-space-mobile-apk` 아티팩트가 만들어집니다.

업데이트 조건은 다음 세 가지입니다.

- applicationId `com.ddmjspace.mobile` 유지
- 기존과 같은 `ANDROID_KEYSTORE_BASE64` 사용
- `app/build.gradle.kts`의 `versionCode` 증가

조건을 지킨 새 APK를 기존 앱 위에 설치하면 활성화 정보와 다운로드 데이터가 유지됩니다. 앱을 삭제한 뒤 재설치하면 앱 전용 저장소의 영상이 사라질 수 있습니다.

## 프로젝트 핵심 구조

```text
app/src/main/java/com/webtoonmap/mobile/
├─ MainActivity.java
├─ ui/AvseeChannelView.java
├─ ui/AvseeDownloadChannelView.java
├─ ui/AvseeSettingsChannelView.java
├─ ui/VideoPlayerActivity.java
├─ download/AvseeDownloadService.java
├─ data/AvseeLibraryDatabase.java
├─ data/AvseeVideo.java
├─ storage/AvseeSettings.java
├─ storage/AvseeStorage.java
└─ activation/
```

참조 프로젝트의 기존 웹툰 관련 클래스들은 UI·기능 비교를 위해 소스에 남아 있지만 새 앱의 화면이나 매니페스트에서는 사용하지 않습니다.
