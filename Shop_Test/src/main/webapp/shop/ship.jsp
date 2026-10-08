<%@page import="java.net.URLDecoder"%>
<%@page import="java.time.LocalDate"%>
<%@page import="shop.dto.User"%>
<%@page import="shop.dao.UserRepository"%>
<%@page import="shop.dto.Product"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	String root = request.getContextPath();
	
	// 장바구니가 비어있으면 장바구니로 이동
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( cartList == null || cartList.isEmpty() ) {
		response.sendRedirect(root + "/shop/cart.jsp");
		return;
	}
	
	// 기본값 : 이전에 입력한 배송 정보(쿠키) → 회원 정보
	String shipName = "", shipDate = "", country = "대한민국", zipCode = "", address = "", phone = "";
	
	String loginId = (String) session.getAttribute("loginId");
	if( loginId != null && !loginId.isEmpty() ) {
		UserRepository userDAO = new UserRepository();
		User user = userDAO.getUserById(loginId);
		userDAO.close();
		if( user != null ) {
			shipName = user.getName() != null ? user.getName() : "";
			address = user.getAddress() != null ? user.getAddress() : "";
			phone = user.getPhone() != null ? user.getPhone() : "";
		}
	}
	
	Cookie[] cookies = request.getCookies();
	if( cookies != null ) {
		for( Cookie cookie : cookies ) {
			String value = URLDecoder.decode( cookie.getValue(), "UTF-8" );
			switch( cookie.getName() ) {
				case "ship_name" : shipName = value; break;
				case "ship_date" : shipDate = value; break;
				case "ship_country" : country = value; break;
				case "ship_zipCode" : zipCode = value; break;
				case "ship_addressName" : address = value; break;
				case "ship_phone" : phone = value; break;
			}
		}
	}
	
	pageContext.setAttribute("shipName", shipName);
	pageContext.setAttribute("shipDate", shipDate);
	pageContext.setAttribute("country", country);
	pageContext.setAttribute("zipCode", zipCode);
	pageContext.setAttribute("address", address);
	pageContext.setAttribute("phone", phone);
	pageContext.setAttribute("minDate", LocalDate.now().toString());
%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Shop</title>
	<jsp:include page="/layout/meta.jsp" /> <jsp:include page="/layout/link.jsp" />
</head>
<body>   
	<jsp:include page="/layout/header.jsp" />
	<div class="px-4 py-5 my-5 text-center">
		<h1 class="display-5 fw-bold text-body-emphasis">배송 정보</h1>
	</div>
	
	<!-- 배송 정보 입력 영역 -->
	<div class="container shop mb-5">
		<form action="${ root }/shop/ship_pro.jsp" name="shipForm" method="post" onsubmit="return checkShip()">
			<input type="hidden" name="cartId" value="${ empty param.cartId ? pageContext.session.id : param.cartId }" />
			
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">성명</label>
				<input type="text" class="form-control col-md-10" name="name" value="${ fn:escapeXml(shipName) }" required>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">배송일</label>
				<input type="date" class="form-control col-md-10" name="shippingDate" value="${ shipDate }" min="${ minDate }" required>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">국가명</label>
				<input type="text" class="form-control col-md-10" name="country" value="${ fn:escapeXml(country) }" required>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">우편번호</label>
				<input type="text" class="form-control col-md-10" name="zipCode" value="${ fn:escapeXml(zipCode) }" required>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">주소</label>
				<input type="text" class="form-control col-md-10" name="addressName" value="${ fn:escapeXml(address) }" required>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">전화번호</label>
				<input type="text" class="form-control col-md-10" name="phone" value="${ fn:escapeXml(phone) }"
					   placeholder="- 생략하고 숫자만 입력해주세요." required>
			</div>
			
			<!-- 버튼 영역 -->
			<div class="d-flex justify-content-between mt-5 mb-5">
				<div class="item">
					<a href="javascript: history.back()" class="btn btn-lg btn-success">이전</a>
					<a href="${ root }/" class="btn btn-lg btn-danger">취소</a>
				</div>
				<div class="item">
					<input type="submit" class="btn btn-lg btn-primary" value="등록" />
				</div>
			</div>
		</form>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
	
	<script>
		// 배송 정보 유효성 검사
		function checkShip() {
			let form = document.shipForm
			
			if( !check(/^.{1,100}$/, form.name, '성명을 입력해주세요.') ) return false
			if( form.shippingDate.value == '' ) {
				alert('배송일을 선택해주세요.')
				form.shippingDate.focus()
				return false
			}
			if( !check(/^[0-9]{5,6}$/, form.zipCode, '우편번호는 숫자 5~6자리로 입력해주세요.') ) return false
			if( !check(/^.{1,200}$/, form.addressName, '주소를 입력해주세요.') ) return false
			if( !check(/^[0-9-]{9,13}$/, form.phone, '전화번호를 올바르게 입력해주세요.') ) return false
			return true
		}
	</script>
</body>
</html>
