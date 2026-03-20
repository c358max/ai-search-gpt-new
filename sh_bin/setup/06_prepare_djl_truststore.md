# 06_prepare_djl_truststore.sh 설명서 (초보 개발자용)

이 문서는 `06_prepare_djl_truststore.sh`가 **무엇을 하는 스크립트인지**를
처음 보는 개발자도 이해할 수 있게 순서대로 설명합니다.

---

## DJL이 왜 필요한가?

이 프로젝트는 Elasticsearch의 벡터 검색을 사용합니다.  
벡터 검색을 하려면 먼저 문장/상품 설명을 숫자 벡터(임베딩)로 바꿔야 합니다.

여기서 DJL(Deep Java Library)이 하는 역할:

- Java(Spring Boot) 코드 안에서 임베딩 모델을 로딩
- 텍스트를 임베딩 벡터로 변환
- 변환된 벡터를 Elasticsearch `dense_vector` 필드에 색인/검색에 사용

즉, **DJL은 “텍스트 -> 벡터”를 담당하는 핵심 라이브러리**입니다.  
그래서 DJL이 모델을 원격에서 내려받을 때 HTTPS 인증서 문제가 있으면,
전체 벡터 검색 테스트가 실패할 수 있습니다.

---

## 0) 이 스크립트가 필요한 이유

우리 프로젝트는 DJL이 원격에서 모델/메타데이터를 내려받을 때 HTTPS를 사용합니다.
그런데 서버 인증서 체인을 Java가 신뢰하지 못하면 아래 같은 오류가 납니다.

- `SSLHandshakeException`
- `PKIX path building failed`

이 문제를 해결하려고, 이 스크립트는 DJL 관련 도메인의 인증서를 모아서
Java가 읽을 수 있는 `truststore(.p12)` 파일을 만듭니다.

---

## 1) 스크립트 시작: 안전 모드

```bash
set -euo pipefail
```

- `-e`: 중간에 에러가 나면 즉시 종료
- `-u`: 선언 안 된 변수를 쓰면 에러
- `-o pipefail`: 파이프라인 중간 단계 실패도 잡음

즉, 조용히 실패하지 않도록 하는 안전 장치입니다.

---

## 2) 경로/변수 준비

- `TRUSTSTORE_PATH="${HOME}/.ai-cert/djl-truststore.p12"`
  - 최종 결과물 경로
  - 여러 인증서를 담는 저장소 파일
- `TRUSTSTORE_PASSWORD="${AI_SEARCH_TRUSTSTORE_PASSWORD:-changeit}"`
  - truststore를 열 때 사용할 비밀번호
  - 환경변수가 없으면 기본값 `changeit`을 사용

- `DJL_HOSTS=("djl.ai" "resources.djl.ai" "mlrepo.djl.ai")`
  - 인증서를 가져올 대상 도메인 목록

---

## 3) 도구 체크

- `openssl`이 있어야 원격 서버 인증서를 읽을 수 있습니다.
- `keytool`이 있어야 Java truststore에 인증서를 넣을 수 있습니다.
- macOS에서는 `/usr/libexec/java_home -v 21`로 Java 21을 자동 선택합니다.

도구가 없으면 즉시 종료합니다.

---

## 4) truststore 파일 초기화

```bash
mkdir -p "$(dirname "${TRUSTSTORE_PATH}")"
rm -f "${TRUSTSTORE_PATH}"
```

- 저장 폴더가 없으면 생성
- 이전 truststore가 있으면 삭제 후 새로 생성

즉, 항상 깨끗한 상태에서 시작합니다.

---

## 5) 핵심 루프: 호스트별 인증서 수집/등록

스크립트는 `DJL_HOSTS`의 각 호스트를 순회하며 아래를 반복합니다.

1. 원격 서버에서 인증서 체인을 가져옴
2. 인증서 1개씩 `.pem` 파일로 분리
3. 분리한 인증서를 truststore에 등록
4. 임시 파일 정리

---

## 6) `openssl` 결과를 어떻게 읽는지 (질문 주신 출력 기준)

사용한 명령:

```bash
openssl s_client -showcerts -servername "djl.ai" -connect "djl.ai:443" </dev/null 2>/dev/null

CONNECTED(00000006)
---
Certificate chain
 0 s:CN=djl.ai
   i:C=KR, O=SOOSAN INT, CN=ePrism SSL
   a:PKEY: RSA, 2048 (bit); sigalg: sha256WithRSAEncryption
   v:NotBefore: Dec 26 11:18:11 2025 GMT; NotAfter: Mar 26 11:18:10 2026 GMT
-----BEGIN CERTIFICATE-----
MIIC5zCCAc+gAwIBAgIQHh1gtVWWA3hf7KLqf8E0FzANBgkqhkiG9w0BAQsFADA3
... 생략
iv/JNt97hCtDsnTO/cZ/8iHaBaGoB6vYN14cmihgWJAB9pp4Ab3f4lcWu8r/FKdc
M7PX2Hmubl2DpGqOWfD38CaA7PCY8bmvfzMR
-----END CERTIFICATE-----
 1 s:C=KR, O=SOOSAN INT, CN=ePrism SSL
   i:C=KR, O=SOOSAN INT, CN=ePrism SSL
   a:PKEY: RSA, 2048 (bit); sigalg: sha256WithRSAEncryption
   v:NotBefore: Dec 21 06:59:48 2021 GMT; NotAfter: Dec 16 06:59:48 2041 GMT
-----BEGIN CERTIFICATE-----
MIIDVDCCAjygAwIBAgIJAPJjxf5ab99tMA0GCSqGSIb3DQEBCwUAMDcxCzAJBgNV
... 생략
f+ktDZI2DcYeFw0IAGsjRsQJSdhm4P4EVqxSknNDZprGvZ50Hy0HOe3Quzuddxi4
TOHlw6RX4+SI8mcUsa160qW/VVLEct5AYsQsCCR63AXaz2aOU5d8iQ==
-----END CERTIFICATE-----
---
Server certificate
subject=CN=djl.ai
issuer=C=KR, O=SOOSAN INT, CN=ePrism SSL
---
No client certificate CA names sent
Peer signing digest: SHA256
Peer signature type: rsa_pss_rsae_sha256
Peer Temp Key: X25519, 253 bits
---
SSL handshake has read 2168 bytes and written 1617 bytes
Verification error: self-signed certificate in certificate chain
---
New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384
Protocol: TLSv1.3
Server public key is 2048 bit
This TLS version forbids renegotiation.
Compression: NONE
Expansion: NONE
No ALPN negotiated
Early data was not sent
Verify return code: 19 (self-signed certificate in certificate chain)
---

```

결과 설명 :
- `-----BEGIN CERTIFICATE-----` / `-----END CERTIFICATE-----` 블록이 **2개**
- `Verification error: self-signed certificate in certificate chain`

해석:

1) 체인 인증서가 2장 있다는 뜻입니다.  
2) 서버가 자체/사설 체인을 쓰는 구조라 기본 Java 신뢰 목록만으로는 검증 실패할 수 있습니다.  
3) 그래서 이 체인을 truststore에 넣어야 DJL HTTPS 요청이 안정적으로 동작합니다.

---

## 7) 인증서 블록만 추출하는 코드

```bash
openssl ... | awk '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/' > "${cert_tmp}"
```

- `openssl` 출력 전체에서
- `BEGIN CERTIFICATE` ~ `END CERTIFICATE` 구간만 남겨서
- 임시 파일(`${cert_tmp}`)에 저장합니다.

중요:
- 구간이 여러 개면 전부 저장됩니다.
- 즉 `${cert_tmp}`에는 인증서 체인이 모두 들어갑니다.

---

## 8) 체인을 개별 PEM 파일로 분리하는 코드

```bash
awk -v prefix="${split_prefix}" '
  /-----BEGIN CERTIFICATE-----/ {n++; file=sprintf("%s.%02d.pem", prefix, n)}
  n > 0 {print > file}
  /-----END CERTIFICATE-----/ {close(file)}
' "${cert_tmp}"
```

동작:

- 첫 번째 인증서 -> `...01.pem`
- 두 번째 인증서 -> `...02.pem`
- ...

질문에서 주신 `djl.ai` 예시(체인 2장)라면
결과적으로 `.pem` 파일이 2개 생깁니다.

---

## 9) truststore에 넣는 코드

```bash
alias="${host}-${idx}"
keytool -importcert -noprompt \
  -storetype PKCS12 \
  -keystore "${TRUSTSTORE_PATH}" \
  -storepass "${TRUSTSTORE_PASSWORD}" \
  -alias "${alias}" \
  -file "${cert_file}"
```

- `alias`는 truststore 내부의 인증서 이름
  - 예: `djl.ai-0`, `djl.ai-1`
- `-noprompt`는 사용자 확인 질문 없이 자동 등록
- 등록 개수는 `idx`로 집계되고,
  - 스크립트는 `[INFO] ${host} certificates imported: ${idx}`를 출력합니다.

---

## 10) 마지막 정리와 완료 로그

- 호스트 처리마다 임시 파일 삭제
- 모든 호스트 처리가 끝나면:

```text
[OK] truststore created: /Users/<USER>/.ai-cert/djl-truststore.p12
```

---

## 11) 실행 후 체크 포인트

1. 파일 존재 확인
   - `~/.ai-cert/djl-truststore.p12`
2. 로그에서 호스트별 등록 개수 확인
   - `djl.ai certificates imported: ...`
3. truststore 내부 항목 확인(선택)
   - `./sh_bin/check/04_check_djl_truststore.sh`
4. 테스트 실행
   - `./gradlew test --tests com.example.aisearch.integration.search.SearchIntegrationTest`

---

## 12) 자주 하는 오해 정리

- Q: truststore는 인증서 하나당 파일 하나인가요?  
  - A: 아니요. truststore 파일 하나에 여러 인증서를 담습니다.

- Q: `TRUSTSTORE_PASSWORD`는 인증서마다 다른 비밀번호인가요?  
  - A: 아니요. truststore 파일 자체의 비밀번호 1개입니다.

- Q: 왜 굳이 이걸 해야 하나요?  
  - A: 기본 Java 신뢰 저장소에서 신뢰하지 못하는 체인을 직접 등록해
    DJL HTTPS 호출 실패를 막기 위해서입니다.
