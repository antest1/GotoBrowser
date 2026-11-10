<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 설정 

툴바가 표시된 상태에서 게임 화면 우측 상단의 톱니바퀴 아이콘(<span class="material-symbols-outlined inline-icon">settings</span>)을 눌러 접근 가능

- 설정 화면은 게임 화면 위에 표시되며, 게임은 뒤에서 계속 실행됩니다.
- 상단의 손잡이를 아래로 끌거나 뒤로 가기 버튼을 누르면 닫힙니다.
- 상단 오른쪽의 도움말 버튼(<span class="material-symbols-outlined inline-icon">help</span>)을 누르면 이 매뉴얼이 열립니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/settings_main.webp"  width="560" style="max-width: 100%;" class="mb-3"/>

##### 설정 반영 방식

| 반영 | 설명 |
| --- | --- |
| 즉시 | 값을 바꾸면 바로 적용됩니다. |
| 새로고침 | "새로고침 필요" 안내가 표시됩니다. "지금 새로고침"을 누르면 전체 새로고침 후 적용되고, "새로고침 없이 설정 변경"을 누르면 다음 새로고침 때 적용됩니다. |

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/settings_refresh_required.webp"  width="480" style="max-width: 100%;" class="mb-3"/>

##### 로그인 및 접속

| 그룹 | 이름 | 설명 | 반영 |
| --- | --- | --- | --- |
| 게임 로그인 | 로그인 자동완성 | 로그인 폼에 자동으로 입력할 아이디와 비밀번호. 기기 내에만 저장됨 | 새로고침 |
| 게임 로그인 | 로그아웃 | 게임에서 로그아웃하고 접속기의 로그인 페이지로 돌아감 | |
| 브라우저 설정 | 접속 방법 선택 | DMM direct (기본값) / ooi.moe<br/>비공식 접속기를 처음 선택하면 게임 리소스 관련 고지가 표시됨 | 새로고침 |
| 브라우저 설정 | 음량 OFF로 시작 | 게임 시작 시 BGM/SE/VOICE 볼륨을 0으로 설정 (공식 어플리케이션 기능과 동일)<br/>DMM direct에서만 사용 가능 | 새로고침 |
| 브라우저 설정 | 브로드캐스트 모드 | 칸코레 관련 통신 내용을 시제 깡들리티 등 다른 어플리케이션에서 사용 가능하도록 전송 (기본 켜짐)<br/>깡들리티와 연동하기 위해서 반드시 켜져 있어야 함 | 즉시 |
| Gadget 서버 우회 | Gadget 서버 우회 활성화 | 해외 IP 차단 문제(검은 화면에서 멈춤)가 발생하는 경우 활성화 | 새로고침 |
| Gadget 서버 우회 | 우회 방식 | URL 교체 혹은 KCCacheProxy (remote) 선택 (대부분의 경우 "URL 교체" 사용) | 새로고침 |
| Gadget 서버 우회 | Endpoint 서버 | 우회에 사용할 서버 주소<br/>`https://kcwiki.github.io/cache/` (기본값) | 새로고침 |
| Gadget 서버 우회 | 우회 자동 비활성화 | 차단 없이 정상적으로 접속되면 우회 옵션을 자동으로 끔 (기본 켜짐) | 즉시 |

우회 관련 내용은 <span class="link" data-move="gadgetbypass">해외 IP 차단 우회</span> 페이지 참고. Gadget 서버 우회 활성화가 꺼져 있으면 우회 방식과 Endpoint 서버는 선택할 수 없습니다.

##### 게임 화면 표시

| 이름 | 설명 | 반영 |
| --- | --- | --- |
| 가로 화면 고정 | 기기의 방향과 상관없이 항상 가로 모드로 플레이 (기본 켜짐) | 즉시 |
| 레이아웃 자동 조정 | 게임 화면을 기기 화면 크기에 맞게 자동으로 확대/축소 (권장, 기본 켜짐) | 새로고침 |
| 게임 화면 세로 위치 | 세로 화면, 분할 화면 등 게임 위아래에 여백이 있을 때 게임을 가운데 또는 위쪽에 배치 (기본 가운데) | 즉시 |
| PIP 모드 활성화 | 홈 화면으로 나가거나 게임 화면을 핀치 축소하면 작은 플로팅 창으로 게임 화면 표시 | 즉시 |
| [멀티윈도우] 분할 바로부터 여백 추가 | 멀티윈도우 기능 사용 시 화면이 분할 바에 바로 붙어 나오지 않도록 여백 추가 | 즉시 |
| 레거시 렌더러 사용 | WebGL 렌더링 모드를 Canvas로 변경 (그래픽 문제가 발생하지 않는 경우 해제 권장)<br/>켜면 Kantai3D를 사용할 수 없음 | 새로고침 |

PIP/멀티윈도우 관련 내용은 <span class="link" data-move="pipmode">PIP/멀티윈도우</span> 페이지 참고

##### 조작 및 툴바

| 그룹 | 이름 | 설명 | 반영 |
| --- | --- | --- | --- |
| 툴바 | 시작 시 패널 보이기 | 앱 시작 시 툴바가 표시된 상태로 시작 (기본 켜짐) | 다음 실행 |
| 툴바 | 확인 없이 새로고침 | 새로고침 버튼을 누를 시 확인 경고 없이 즉시 화면이 갱신됨 | 즉시 |
| 툴바 | 툴바 표시 위치 | 가운데(기본) 또는 위쪽 · 클래식. 위쪽 · 클래식은 3.x처럼 툴바를 화면 위쪽에 붙이고 새로고침 버튼을 맨 위에 둠. 가로 화면에 적용 | 즉시 |
| 툴바 | 가장자리 스와이프로 툴바 표시 | 툴바가 숨겨진 화면 가장자리에서 안쪽으로 밀어 툴바를 표시 (기본 켜짐)<br/>끄면 가장자리 스와이프가 시스템 제스처로만 동작<br/>여백이 없는 화면에서는 뒤로 가기 버튼으로 툴바를 표시 | 즉시 |
| 게임 조작 | 키보드 입력 활성화 | 게임 화면에서 키보드 입력을 사용할지 여부 (기본 켜짐) | 즉시 |
| 게임 조작 | 입력 모드 | 게임 플레이 시 입력 방식 선택 (화면 터치 혹은 마우스 사용) | 새로고침 |

##### 캐시 설정

| 이름 | 설명 | 반영 |
| --- | --- | --- |
| 리소스 캐싱 시 외부 저장소 사용 | 게임 리소스를 캐싱할 때 내부 저장소 대신 외부 저장소를 사용 (권장)<br/>(PC를 통해 이미지/보이스 리소스에 접근하거나 편집 가능) | 새로고침 |
| 폰트 파일 미리 로드 | 게임 서버에서 폰트를 불러오는 대신 앱에 내장된 폰트 파일을 사용 (권장, 기본 켜짐) | 새로고침 |
| 게임 에셋 다운로드 재시도 | 사운드/이미지 에셋 다운로드에 실패한 경우 다시 받을지 묻는 창을 표시<br/>불안정한 네트워크에서 발생하는 에러코를 줄일 수 있음. 게임 API 통신에는 적용되지 않음 | 즉시 |
| 캐시 비우기 | 브라우저 내 캐시를 삭제. 이미지나 오디오가 제대로 불러와지지 않거나 로그인 방식을 바꿀 때 사용<br/>삭제 후에는 다시 로그인해야 할 수 있음<br/>삭제가 끝나면 새로고침 안내가 표시됨 | 즉시 |

##### 자막 설정

| 이름 | 설명 | 반영 |
| --- | --- | --- |
| 자막 언어 | English / 한국어 / 日本語 (KC3改), 简中 / 繁中 (kcwiki) 중 선택 | 즉시 |
| 폰트 크기 | 화면 내 보이스 자막의 크기를 조절 (12~24, 기본값 18) | 즉시 |
| 자막 데이터 다운로드 | 데이터 업데이트가 존재하는 경우 다운로드 가능. 받은 직후 적용됨 | 즉시 |

기능 관련 내용은 <span class="link" data-move="voiceline">인게임 대사 자막</span> 페이지 참고

##### 서드파티 모드

<span class="text-danger">서드파티 모드는 자기 책임하에 사용하세요.</span>

| 그룹 | 이름 | 설명 | 반영 | 저자 |
| --- | --- | --- | --- | --- |
| 일반 | FPS 렌더링 모드 | VSYNC 렌더링을 사용하여 원본 게임보다 부드럽고 효율적으로 동작 (높은 화면 재생률 기기 지원) | 새로고침 | [@laplamgor](https://x.com/laplamgor) |
| 일반 | FPS 제한 | 기본 (최대) 혹은 기기가 지원하는 주사율 중 선택. 목표 FPS를 낮추면 배터리가 절약되고 발열이 줄어듦 | 즉시 | |
| 일반 | 진정한 크리티컬 히트 | 하드 코딩된 데미지의 난독화를 제거. CL2가 발생한 경우에만 크리티컬 히트 표시 | 새로고침 | [@Oradimi](https://x.com/oradimi) |
| Kantai3D 시각 모드 | Kantai3D v4.0 | 일부 비서함에 3D 효과 적용. 레거시 렌더러 사용 시 선택 불가 | 새로고침 | [@laplamgor](https://x.com/laplamgor) |
| Kantai3D 시각 모드 | About Kantai3D | [Kantai3D GitHub 페이지](https://github.com/laplamgor/kantai3d) 열기 | | |
| 언어 번역 패치 (KCCP) | KanColle Language Patch (KCCP) | 게임 내 텍스트에 번역 패치 적용 | 새로고침 | [@Oradimi](https://x.com/oradimi) |
| 언어 번역 패치 (KCCP) | 적용할 언어 패치 | [KanColle English Patch](https://github.com/Oradimi/KanColle-English-Patch-KCCP) (@Oradimi) 혹은 [KanColle Indonesia Patch](https://github.com/SLAVUSworks/KanColle-Indonesia-Patch-KCCP) (@SLAVUSworks) | 새로고침 | |
| 언어 번역 패치 (KCCP) | 패치 데이터 다운로드 | 선택한 패치의 데이터를 다운로드 혹은 업데이트. 완료 후 게임을 새로고침해야 적용됨 | 새로고침 | |
| 언어 번역 패치 (KCCP) | 패치 데이터 삭제 | 패치가 예기치 않은 동작을 하거나 다운로드 중 문제가 발생한 경우 사용 | 새로고침 | |
| 언어 번역 패치 (KCCP) | KanColle English/Indonesia Patch에 대해 | 선택한 패치의 GitHub 페이지 열기 | | |

##### 어플리케이션 정보

| 이름 | 설명 | 반영 |
| --- | --- | --- |
| 현재 버전 | 설치된 앱 버전 | |
| 업데이트 확인 | 새 버전이 있는 경우 다운로드 페이지 표시 | |
| 소스 코드 | [GitHub 저장소](https://github.com/antest1/GotoBrowser/) 열기 | |
| DevTools 디버깅 활성화 | Chrome 등의 브라우저에서 [원격 디버깅](https://developer.chrome.com/docs/devtools/remote-debugging?hl=ko)이 가능하도록 설정 (chrome://inspect) | 즉시 |
