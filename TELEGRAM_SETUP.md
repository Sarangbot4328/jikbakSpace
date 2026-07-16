# 새 봇 활성화 설정

1. Telegram의 BotFather에서 `/newbot`으로 직박구리 스페이스 모바일 전용 봇을 만듭니다.
2. 발급된 토큰을 GitHub Actions secret `TELEGRAM_BOT_TOKEN`으로 저장합니다.
3. 관리자 채팅의 숫자 ID를 `TELEGRAM_ADMIN_CHAT_ID`로 저장합니다.
4. 앱을 빌드하고 설치한 뒤 최초 사용자명을 등록합니다.
5. 관리자 채팅에서 다음 형식으로 명령을 보냅니다.

```text
/우동모바일 사용자명 (비밀번호)
```

6. 앱에 암호 입력란이 나타나면 괄호 안의 비밀번호를 입력합니다.

활성화 정보는 앱 전용 SharedPreferences에 저장됩니다. 같은 applicationId와 서명키의 APK로 덮어 업데이트하면 유지되고, 앱 데이터 삭제나 앱 제거 시 초기화됩니다.
