<%@page import="java.net.URLDecoder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dto.Ship"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	String root = request.getContextPath();
	
	// 쿠키에 저장된 배송 정보 불러오기
	Ship ship = new Ship();
	Cookie[] cookies = request.getCookies();
	if( cookies != null ) {
		for( Cookie cookie : cookies ) {
			String value = URLDecoder.decode( cookie.getValue(), "UTF-8" );
			switch( cookie.getName() ) {
				case "ship_cartId" : ship.setCartId(value); break;
				case "ship_name" : ship.setShipName(value); break;
				case "ship_date" : ship.setDate(value); break;
				case "ship_country" : ship.setCountry(value); break;
				case "ship_zipCode" : ship.setZipCode(value); break;
				case "ship_addressName" : ship.setAddress(value); break;
				case "ship_phone" : ship.setPhone(value); break;
			}
		}
	}
	
	// 세션에서 장바구니 목록 가져오기
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( cartList == null || cartList.isEmpty() ) {
		response.sendRedirect(root + "/shop/cart.jsp");
		return;
	}
	// 배송 정보가 없으면 배송 정보 입력으로 이동
	if( ship.getShipName() == null || ship.getShipName().isEmpty() ) {
		response.sendRedirect(root + "/shop/ship.jsp");
		return;
	}
	
	int sum = 0;
	for( Product product : cartList ) {
		sum += product.getUnitPrice() * product.getQuantity();
	}
	
	// 로그인 여부 확인
	String loginId = (String) session.getAttribute("loginId");
	boolean login = loginId != null && !loginId.isEmpty();
	
	request.setAttribute("ship", ship);
	request.setAttribute("cartList", cartList);
	request.setAttribute("sum", sum);
	request.setAttribute("login", login);
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
		<h1 class="display-5 fw-bold text-body-emphasis">주문 정보</h1>
	</div>
	
	<!-- 주문 확인 영역 -->
	<div class="container order mb-5">
		<form action="${ root }/shop/complete.jsp" name="orderForm" method="post" onsubmit="return checkOrder()">
		<!-- 배송정보 -->
		<div class="ship-box">
			<table class="table">
				<tr>
					<td>주문 형태 :</td>
					<td>${ login ? '회원 주문' : '비회원 주문' }</td>
				</tr>
				<tr>
					<td>성 명 :</td>
					<td><c:out value="${ ship.shipName }" /></td>
				</tr>
				<tr>
					<td>우편번호 :</td>
					<td><c:out value="${ ship.zipCode }" /></td>
				</tr>
				<tr>
					<td>주소 :</td>
					<td><c:out value="${ ship.address }" /></td>
				</tr>
				<tr>
					<td>배송일 :</td>
					<td><c:out value="${ ship.date }" /></td>
				</tr>
				<tr>
					<td>전화번호 :</td>
					<td><c:out value="${ ship.phone }" /></td>
				</tr>
				<c:if test="${ !login }">
				<tr>
					<td>주문 비밀번호 :</td>
					<td>
						<input type="password" class="form-control" name="orderPw" maxlength="20"
							   placeholder="비회원 주문내역 조회 시 사용할 비밀번호">
					</td>
				</tr>
				</c:if>
			</table>
		</div>
		
		<!-- 주문목록 -->
		<div class="cart-box">
			<table class="table table-striped table-hover table-bordered text-center align-middle">
				<thead>
					<tr class="table-primary">
						<th>상품</th>
						<th>가격</th>
						<th>수량</th>
						<th>소계</th>
						<th>비고</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="product" items="${ cartList }">
					<tr>
						<td><c:out value="${ product.name }" /></td>
						<td>${ product.unitPrice }</td>
						<td>${ product.quantity }</td>
						<td>${ product.unitPrice * product.quantity }</td>
						<td></td>
					</tr>
					</c:forEach>
				</tbody>
				<tfoot>
					<tr>
						<td></td>
						<td></td>
						<td>총액</td>
						<td>${ sum }</td>
						<td></td>
					</tr>
				</tfoot>
			</table>
		</div>
		
		<!-- 버튼 영역 -->
		<div class="d-flex justify-content-between mt-5 mb-5">
			<div class="item">
				<a href="javascript: history.back()" class="btn btn-lg btn-success">이전</a>
				<a href="${ root }/" class="btn btn-lg btn-danger">취소</a>
			</div>
			<div class="item">
				<c:choose>
					<%-- 회원 : userId 를 식별정보로 전달 --%>
					<c:when test="${ login }">
						<input type="hidden" name="userId" value="${ sessionScope.loginId }" />
					</c:when>
					<%-- 비회원 : phone, orderPw 를 식별정보로 전달 --%>
					<c:otherwise>
						<input type="hidden" name="phone" value="${ fn:escapeXml(ship.phone) }" />
					</c:otherwise>
				</c:choose>
				<input type="hidden" name="cartId" value="${ fn:escapeXml(ship.cartId) }" />
				<input type="hidden" name="totalPrice" value="${ sum }" />
				<input type="submit" class="btn btn-lg btn-primary" value="주문완료" />
			</div>
		</div>
		</form>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
	
	<script>
		// 비회원 주문 비밀번호 체크
		function checkOrder() {
			let form = document.orderForm
			if( form.orderPw && form.orderPw.value.trim() == '' ) {
				alert('주문 비밀번호를 입력해주세요.')
				form.orderPw.focus()
				return false
			}
			return true
		}
	</script>
</body>
</html>
