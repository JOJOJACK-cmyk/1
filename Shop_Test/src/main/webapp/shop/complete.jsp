<%@page import="java.net.URLDecoder"%>
<%@page import="java.util.List"%>
<%@page import="shop.dto.Order"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.OrderRepository"%>
<%@page import="shop.dao.ProductIORepository"%>
<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	String root = request.getContextPath();
	String cookiePath = root.isEmpty() ? "/" : root;
	
	// 장바구니가 비어있으면 (새로고침 등) 상품 목록으로 이동
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( !"POST".equalsIgnoreCase(request.getMethod()) || cartList == null || cartList.isEmpty() ) {
		response.sendRedirect(root + "/shop/products.jsp");
		return;
	}
	
	// 쿠키에 저장된 배송 정보
	Order order = new Order();
	Cookie[] cookies = request.getCookies();
	if( cookies != null ) {
		for( Cookie cookie : cookies ) {
			String value = URLDecoder.decode( cookie.getValue(), "UTF-8" );
			switch( cookie.getName() ) {
				case "ship_cartId" : order.setCartId(value); break;
				case "ship_name" : order.setShipName(value); break;
				case "ship_date" : order.setDate(value); break;
				case "ship_country" : order.setCountry(value); break;
				case "ship_zipCode" : order.setZipCode(value); break;
				case "ship_addressName" : order.setAddress(value); break;
				case "ship_phone" : order.setPhone(value); break;
			}
		}
	}
	if( order.getShipName() == null || order.getShipName().isEmpty() ) {
		response.sendRedirect(root + "/shop/ship.jsp");
		return;
	}
	
	// 회원 / 비회원 식별정보
	String loginId = (String) session.getAttribute("loginId");
	String userId = request.getParameter("userId");
	boolean login = loginId != null && !loginId.isEmpty() && loginId.equals(userId);
	if( login ) {
		// 회원 : 회원 아이디(userId) 저장
		order.setUserId(loginId);
		order.setPhone(null);
		order.setOrderPw(null);
	} else {
		// 비회원 : 전화번호(phone), 주문 비밀번호(order_pw) 저장
		String phone = request.getParameter("phone");
		String orderPw = request.getParameter("orderPw");
		if( phone != null && !phone.isEmpty() ) order.setPhone( phone.replaceAll("[^0-9]", "") );
		if( orderPw == null || orderPw.trim().isEmpty() ) {
			response.sendRedirect(root + "/shop/order.jsp");
			return;
		}
		order.setOrderPw(orderPw);
		order.setUserId(null);
	}
	
	// 총 금액
	int totalPrice = 0;
	for( Product product : cartList ) {
		totalPrice += product.getUnitPrice() * product.getQuantity();
	}
	order.setTotalPrice(totalPrice);
	
	// 1. 주문 테이블 [order] 에 주문 내역 1건 추가
	OrderRepository orderDAO = new OrderRepository();
	int result = orderDAO.insert(order);
	int orderNo = order.getOrderNo() > 0 ? order.getOrderNo() : orderDAO.lastOrderNo();
	
	if( result > 0 ) {
		ProductIORepository productIODAO = new ProductIORepository();
		ProductRepository productDAO = new ProductRepository();
		for( Product product : cartList ) {
			// 2. 상품 입출고 테이블 [product_io] 에 출고(OUT) 데이터 추가
			product.setOrderNo(orderNo);
			product.setType("OUT");
			product.setUserId(order.getUserId());
			productIODAO.insert(product);		// amount = 주문 수량(quantity)
			
			// 3. 상품 테이블 [product] 의 재고수를 주문 수만큼 감소
			productDAO.decreaseStock(product.getProductId(), product.getQuantity());
		}
		productIODAO.close();
		productDAO.close();
		
		// 비회원 : 주문내역 바로 조회할 수 있도록 세션에 등록
		if( !login ) {
			session.setAttribute("orderPhone", order.getPhone());
			session.setAttribute("orderList", orderDAO.list(order.getPhone(), order.getOrderPw()));
		}
		
		// 장바구니 비우기
		session.removeAttribute("cartList");
		
		// 배송 정보 쿠키 삭제
		String[] shipCookies = {"ship_cartId", "ship_name", "ship_date", "ship_country", "ship_zipCode", "ship_addressName", "ship_phone"};
		for( String name : shipCookies ) {
			Cookie cookie = new Cookie(name, "");
			cookie.setMaxAge(0);
			cookie.setPath(cookiePath);
			response.addCookie(cookie);
		}
	}
	orderDAO.close();
	
	request.setAttribute("result", result);
	request.setAttribute("orderNo", orderNo);
	request.setAttribute("order", order);
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
		<h1 class="display-5 fw-bold text-body-emphasis">주문 완료</h1>
	</div>
	
	<!-- 주문 완료 영역 -->
	<div class="container order mb-5">
		<c:choose>
			<c:when test="${ result > 0 }">
				<h2 class="text-center mb-4">주문이 완료되었습니다.</h2>
				<table class="table">
					<tr>
						<td>주문번호 :</td>
						<td>${ orderNo }</td>
					</tr>
					<tr>
						<td>배송지 :</td>
						<td><c:out value="${ order.address }" /></td>
					</tr>
				</table>
			</c:when>
			<c:otherwise>
				<h2 class="text-center text-danger mb-4">주문 처리 중 오류가 발생했습니다.</h2>
			</c:otherwise>
		</c:choose>
		<div class="text-center">
			<a href="${ root }/user/order.jsp" class="btn btn-lg btn-primary">주문내역</a>
		</div>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
</body>
</html>
