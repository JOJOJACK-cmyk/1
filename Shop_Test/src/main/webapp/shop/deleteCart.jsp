<%@page import="shop.dto.Product"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	// 장바구니 상품 삭제 처리
	String root = request.getContextPath();
	String productId = request.getParameter("id");
	String cartId = request.getParameter("cartId");

	// 장바구니 전체 삭제 (요청 파라미터 cartId : 세션ID)
	if( productId == null && cartId != null && cartId.equals(session.getId()) ) {
		session.removeAttribute("cartList");
		response.sendRedirect(root + "/shop/cart.jsp");
		return;
	}

	// 장바구니 개별 상품 삭제 (요청 파라미터 id : 상품ID)
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( cartList != null && productId != null ) {
		for( int i = 0 ; i < cartList.size() ; i++ ) {
			if( cartList.get(i).getProductId().equals(productId) ) {
				cartList.remove(i);
				break;
			}
		}
	}
	response.sendRedirect(root + "/shop/cart.jsp");
%>
