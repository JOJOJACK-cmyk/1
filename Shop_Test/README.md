# Shop_Test - JSP 쇼핑몰 (2차 포트폴리오 평가)

JSP & Servlet / EL & JSTL / MySQL 쇼핑몰 프로젝트 (JDK 11+, Tomcat 9.0, `javax.servlet`)

## 실행 방법
1. **DB 준비** (MySQL)
   - `SQL/00_create_user.sql` : root 계정으로 1회 실행 → `joeun` DB / `joeun`(123456) 계정 생성
   - `SQL/DDL.sql` : `joeun` DB 에서 실행 → 테이블 + 기본 상품/회원 데이터 생성 (재실행 가능)
2. **Eclipse** : `File > Import > Existing Projects into Workspace` → `Shop_Test` 선택
   - Tomcat 버전이 다르면 `Properties > Targeted Runtimes` 에서 변경
3. 서버 실행 후 `http://localhost:8080/Shop/` 접속
   - 테스트 계정 : `joeun` / `123456`
   - DB 접속 정보 : `src/main/java/db.properties`

## 구현 내용
| 구분 | 파일 | 내용 |
|---|---|---|
| 필터 | `filter/EncodingFilter` | web.xml 초기 파라미터 `encoding`(UTF-8) 로 요청/응답 인코딩 설정 |
| | `filter/LoginFilter` | 쿠키 `rememberMe`, `token` → `persistent_logins` 조회 → 세션 `loginId` 등록 (자동 로그인) |
| DAO | `JDBConnection` | `db.properties` 를 읽어 DB 연결 |
| | `UserRepository` | 회원 등록/로그인/조회/수정/삭제 + 자동 로그인 토큰 |
| | `ProductRepository` | 상품 목록/검색(상품명·설명·제조사·카테고리)/조회/등록/수정/삭제/재고 감소 |
| | `ProductIORepository` | 입출고 등록(IN/OUT)/조회 |
| | `OrderRepository` | 주문 등록, 최근 주문번호, 주문내역(회원/비회원) |
| 회원 | `user/*.jsp` | 로그인(아이디 저장, 자동 로그인), 회원가입(유효성 검사), 마이페이지, 정보수정, 탈퇴, 로그아웃, 주문내역(회원/비회원) |
| 상품 | `shop/*.jsp` | 상품 목록/검색/상세, 상품 편집/등록(이미지 업로드)/수정/삭제, 장바구니, 배송정보, 주문, 주문완료 |

### 주문 처리 (`shop/complete.jsp`)
1. `order` 테이블에 주문 1건 등록 (회원: `user_id` / 비회원: `phone`, `order_pw`)
2. 주문한 모든 상품에 대해 `product_io` 에 출고(`OUT`) 데이터 등록 (`amount` = 주문 수량)
3. `product` 재고 수를 주문 수량만큼 감소

## 라이브러리 (`WEB-INF/lib`)
mysql-connector-j, cos (파일 업로드), commons-fileupload/io, **jstl-1.2** (EL/JSTL 사용을 위해 추가)
