<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20,400,0,0&icon_names=help,settings" />

## 게임 시작

앱을 실행하면 별도의 메인 화면 없이 바로 접속기의 시작 페이지(기본값: DMM direct)가 열립니다.  
4.0 이전 버전의 메인 화면에 있던 항목은 모두 <span class="link" data-move="settings">설정 화면</span>으로 이동했습니다.

##### 실행 흐름
1. 접속기의 시작 페이지가 열립니다. 로그인 페이지에서는 설정에 저장해 둔 계정 정보가 자동으로 입력됩니다.
2. 로그인하면 게임 화면이 기기 화면에 맞춰 표시됩니다. (설정 → 게임 화면 표시 → 레이아웃 자동 조정)
3. 게임을 불러오는 동안 게임 화면에 로딩 안내가 표시되며, 게임이 시작되면 사라집니다. (아래 로딩 안내 참고)
4. 게임 화면 바깥의 검은색 여백을 터치하면 <span class="link" data-move="browserui">툴바</span>가 나타나고, 우측 상단의 설정 버튼(<span class="material-symbols-outlined inline-icon">settings</span>)으로 설정 화면을 열 수 있습니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/first_launch.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 로딩 안내
게임을 불러오는 동안 게임 화면 가운데에 진행 상황이 표시됩니다. 빠르게 불러온 경우에는 표시되지 않습니다.
- 준비 → 서버 연결 → 리소스 다운로드 순으로 진행되며, 오른쪽 위에 현재 단계(1/3~3/3)가 표시됩니다.
- 리소스를 받는 동안에는 게임 자체의 로딩 화면을 가리지 않도록 화면 하단에 작게 표시됩니다.
- 로그인 페이지 등 일반 웹 페이지 위에는 표시되지 않습니다.

| 상태 | 설명 | 대처 방법 |
| --- | --- | --- |
| 진행 중 | 현재 단계와 진행 상황을 표시합니다. | 그대로 기다립니다. |
| 응답 없음 (노란색) | 20초 이상 아무 응답이 없을 때 표시됩니다. | 노란색으로 강조된 새로고침 버튼으로 다시 시도합니다. |
| 연결 오류 (빨간색) | 페이지를 불러오지 못했을 때 원인과 함께 표시됩니다. 아래에 오류 코드와 주소가 표시됩니다. | 인터넷 연결을 확인한 뒤 새로고침 버튼을 누릅니다. 이때는 자동으로 전체 새로고침이 실행됩니다. |

- 툴바가 숨겨져 있었다면 새로고침 버튼이 보이도록 툴바가 자동으로 나타납니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/loading_status.webp"  width="840" style="max-width: 100%;" class="mb-3"/>
<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/loading_error.webp"  width="840" style="max-width: 100%;" class="mb-3"/>

##### 접속 방법 선택
- 일반적인 경우 DMM direct로 설정된 값을 바꾸지 않아도 됩니다.
- OOI 기반 접속기([ooi.moe](https://ooi.moe/))를 사용하고자 하는 경우 설정 → 로그인 및 접속 → 접속 방법 선택에서 변경할 수 있습니다.
  - 비공식 접속기를 처음 선택하면 게임 리소스가 원본과 다를 수 있다는 고지가 표시됩니다.
- 접속기를 바꾸면 새로고침 후 적용됩니다.

##### 로그인 자동완성
- 설정 → 로그인 및 접속 → 로그인 자동완성에서 로그인 폼에 입력할 아이디 및 비밀번호를 저장할 수 있습니다.
- 해당 정보는 기기 내에만 저장되며, 자동완성 외의 용도로 사용되지 않습니다.

##### 시작 시 표시될 수 있는 안내
| 안내 | 설명 |
| --- | --- |
| 업데이트 안내 | 새 버전이 있는 경우 다운로드 여부를 묻습니다. (설정 → 어플리케이션 정보 → 업데이트 확인과 동일) |
| 시제 깡들리티 설치 감지됨 | 시제 깡들리티가 설치되어 있는데 브로드캐스트 모드가 꺼져 있는 경우 표시됩니다. 확인을 누르면 브로드캐스트 모드가 켜집니다. (<span class="link" data-move="kcanotify">시제 깡들리티 연동</span> 참고) |
| 해외 IP 차단이 감지됨 | Gadget 서버 우회가 꺼진 상태에서 해외 IP 차단이 확인된 경우 표시됩니다. (<span class="link" data-move="gadgetbypass">해외 IP 차단 우회</span> 참고) |

##### 이전 버전과의 차이
| 이전 버전 (메인 화면) | 4.0 이후 |
| --- | --- |
| 접속 방법 선택 | 설정 → 로그인 및 접속 → 접속 방법 선택 |
| 로그인 자동완성 | 설정 → 로그인 및 접속 → 로그인 자동완성 |
| 음량 OFF로 시작 / 브로드캐스트 모드 / Gadget 서버 우회 활성화 | 설정 → 로그인 및 접속 |
| 패널 보이기 / 키보드 입력 활성화 | 설정 → 조작 및 툴바 |
| 캐시 비우기 | 설정 → 캐시 설정 → 캐시 비우기 |
| 매뉴얼 사이트 (<span class="material-symbols-outlined inline-icon">help</span>) | 설정 화면 상단의 도움말 버튼 |
| 설정 화면 (<span class="material-symbols-outlined inline-icon">settings</span>) | 게임 화면 우측 상단의 설정 버튼 |
