## 해외 IP 차단 우회

<div class="alert alert-danger" role="alert">2024/12/03 이후 해외 IP 차단이 해제되어 이 기능을 더이상 사용하지 않아도 됩니다.</div>

2020년 2월 이후, 칸코레 운영진 (C2기관) 측에서 디도스 공격 등을 이유로 칸코레 로그인 서버에 대하여 해외 IP를 차단한 상태입니다.  
따라서 현재 시점에서 일본 외 국가에서 정상적으로 칸코레를 플레이하기 위해서는 위의 차단을 우회할 필요가 있습니다.

IP 차단이 확인된 경우에는 게임 시작 시 아래와 같은 알림이 나타납니다.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/bypass_required.png"  width="480" style="max-width: 100%;"/>

##### 설정 방법

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/ko/gadget_options.png"  width="840" style="max-width: 100%;" class="mb-3"/>

1. 메인 화면 혹은 설정에서 "Gadget 서버 우회 활성화" 옵션을 활성화합니다.  
   (초기 설치 시 일본어 이외의 언어로 설정된 경우 기본적으로 켜져 있습니다)
2. 우회 방식은 "URL 교체"를, Endpoint 서버는 아래의 두 서버 중 하나로 설정하시면 됩니다. (오탈자 유의!)

| Endpoint 서버 주소 | 비고 |
| --- | --- |
| `https://kcwiki.github.io/cache/` | 기본값 |
| `https://luckyjervis.com/` | | 