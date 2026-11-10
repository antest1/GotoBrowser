## 브라우저 UI
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@40,400,0,0&icon_names=closed_caption,crop_free,frame_reload,light_mode,photo_camera,refresh,screen_lock_rotation,settings,volume_off" />


<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/game_screen.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

게임 화면 가장자리의 툴바를 통해 브라우저 상에서의 새로고침이나 스크린샷 등 다양한 기능을 수행할 수 있습니다.  
- 세로 모드에서는 화면 하단에 가로로, 가로 모드에서는 화면 왼쪽에 세로로 표시됩니다.
- 버튼을 길게 누르면 버튼의 이름이 표시됩니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/game_screen_portrait.webp"  width="300" style="max-width: 100%;" class="mb-3"/>

##### 툴바 보이기/숨기기
- 툴바를 나타나게 하기 위한 방법은 다음과 같습니다.
  - 게임 화면 바깥의 검은색 여백을 터치 (권장)
  - 툴바가 숨겨진 화면 가장자리(세로 모드: 하단, 가로 모드: 왼쪽)에서 안쪽으로 밀기 (스와이프) (설정 → 조작 및 툴바 → "가장자리 스와이프로 툴바 표시"로 끌 수 있음)
  - 게임 화면 바깥에 여백이 없으면 뒤로 가기 버튼(또는 뒤로 가기 제스처)을 눌러 표시
  - 설정 → 조작 및 툴바 → "시작 시 패널 보이기"로 앱 시작 시 표시되도록 설정
- 반대로 툴바를 없애기 위한 방법은 다음과 같습니다.
  - 게임 화면 바깥의 검은색 여백을 다시 터치
  - 툴바를 화면 가장자리 방향으로 밀기 (스와이프)

##### 새로고침
툴바 옆의 새로고침 버튼은 두 가지 방식을 제공합니다.
가로 화면에서는 설정 → 조작 및 툴바 → "툴바 표시 위치"를 "위쪽 · 클래식"으로 바꾸면, 3.x처럼 툴바가 화면 위쪽에 붙고 새로고침 버튼이 맨 위에 옵니다.

|  | 기능 | 설명 |
| --- | --- | --- |
| <span class="material-symbols-outlined">frame_reload</span> | 빠른 새로고침 | 전체 웹 페이지 대신 게임 프레임만 다시 불러옵니다.<br/>빠르지만 항상 성공하는 것은 아니며, 네트워크 오류가 발생하면 전체 새로고침을 사용하세요. |
| <span class="material-symbols-outlined">refresh</span> | 전체 새로고침 | 전체 웹 페이지를 다시 불러옵니다.<br/>가장 확실한 새로고침 방법이지만 빠른 새로고침보다 오래 걸립니다. |

- 버튼을 누른 채로 메뉴가 펼쳐지는 쪽으로 끌면 다른 방식을 선택할 수 있습니다. 툴바 표시 위치가 "가운데"이면 위쪽, "위쪽 · 클래식"이면 아래쪽으로 펼쳐집니다. 마지막에 사용한 방식이 버튼의 기본 동작으로 기억됩니다.
- 설정 → 조작 및 툴바 → "확인 없이 새로고침"을 켜면 새로고침 전 확인 창이 나타나지 않습니다.
- 설정 변경 후 "새로고침 필요" 안내에서 "새로고침 없이 설정 변경"을 선택한 경우에도, 이 버튼으로 새로고침하면 변경한 설정이 적용됩니다.
- 로딩 중 연결 오류나 게임 중 서버 오류가 발생한 뒤에는, 저장된 방식과 상관없이 버튼이 전체 새로고침으로 동작하며 버튼 옆에 "전체 새로고침"이 표시됩니다. 저장된 방식은 바뀌지 않습니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/refresh_menu.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 툴바 버튼 설명
|  | 기능 | 설명 |
| --- | --- | --- |
| <span class="material-symbols-outlined">volume_off</span>  | 음소거 | 브라우저에서 재생되고 있는 소리를 음소거합니다. 음성 파일은 계속 로딩됩니다.<br/>(소리 없이 자막만 보고 싶은 경우 등에 사용) |
| <span class="material-symbols-outlined">photo_camera</span> | 인게임 스크린샷 | 스크린샷 모드를 켜거나 끕니다. 켜면 화면 우측 상단의 설정 버튼 아래에 스크린샷 버튼이 표시됩니다. |
| <span class="material-symbols-outlined">screen_lock_rotation</span> | 회전 잠금 | 현재 화면 상태에서 기기 설정에 상관없이 회전되지 않도록 설정합니다. |
| <span class="material-symbols-outlined">light_mode</span> | 화면 상시 켜기 | 대기 상태에서 화면이 어두워지지 않도록 설정합니다. |
| <span class="material-symbols-outlined">closed_caption</span> | 자막 | 화면 하단에 인게임 자막을 표시하거나 숨깁니다. 표시된 자막을 터치하면 다음 대사가 나올 때까지 숨겨집니다. |

##### 우측 상단 버튼
|  | 기능 | 설명 |
| --- | --- | --- |
| <span class="material-symbols-outlined">settings</span> | 설정 | <span class="link" data-move="settings">설정 화면</span>을 엽니다. 게임은 설정 화면 뒤에서 계속 실행됩니다. |
| <span class="material-symbols-outlined">crop_free</span> | 스크린샷 | 현재 게임 화면을 저장합니다. 스크린샷 모드가 켜져 있을 때만 표시됩니다. (<span class="link" data-move="screenshot">스크린샷 저장</span> 참고) |

##### 게임 중 오류 안내
게임 중 서버와의 통신에 문제가 생기면 게임 화면 가운데에 안내가 표시됩니다. 게임 자체의 오류 문구를 가리지 않는 위치입니다.

| 안내 | 설명 |
| --- | --- |
| 통신 오류 | 연결이 끊기거나 응답이 없을 때 표시됩니다. 연결을 확인한 뒤 새로고침하세요. |
| 서버 응답 오류 | 서버가 HTTP 오류로 응답했을 때 표시됩니다. 잠시 후 새로고침하세요. |
| 서버 오류 | 서버가 요청을 거부했을 때(api_result) 표시됩니다. 빠른 새로고침으로는 복구되지 않으므로 새로고침 버튼이 자동으로 전체 새로고침으로 동작합니다. |

- 안내를 터치하면 닫힙니다.
- 안내를 길게 누르면 코드·API·메시지가 복사됩니다. 문의할 때 함께 보내 주세요.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/game_notice.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 로그아웃
- 로그아웃은 설정 → 로그인 및 접속 → 로그아웃에서 할 수 있습니다.
- 로그아웃하면 접속기의 로그인 페이지가 다시 열립니다.
