<%@page import="java.net.URLEncoder"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%!
	// 요청 파라미터 → 쿠키 (URL 인코딩)
	Cookie createCookie(String name, String value, String path) throws Exception {
		Cookie cookie = new Cookie(name, URLEncoder.encode(value == null ? "" : value.trim(), "UTF-8"));
		cookie.setMaxAge(24 * 60 * 60);		// 24 시간(1일)
		cookie.setPath(path);
		return cookie;
	}
%>
<%
	// 배송 정보 등록 처리 - 쿠키에 배송 정보를 저장
	String root = request.getContextPath();
	String cookiePath = root.isEmpty() ? "/" : root;
	
	String phone = request.getParameter("phone");
	phone = phone == null ? "" : phone.replaceAll("[^0-9]", "");		// 숫자만 저장
	
	response.addCookie( createCookie("ship_cartId", request.getParameter("cartId"), cookiePath) );
	response.addCookie( createCookie("ship_name", request.getParameter("name"), cookiePath) );
	response.addCookie( createCookie("ship_date", request.getParameter("shippingDate"), cookiePath) );
	response.addCookie( createCookie("ship_country", request.getParameter("country"), cookiePath) );
	response.addCookie( createCookie("ship_zipCode", request.getParameter("zipCode"), cookiePath) );
	response.addCookie( createCookie("ship_addressName", request.getParameter("addressName"), cookiePath) );
	response.addCookie( createCookie("ship_phone", phone, cookiePath) );
	
	// 주문 정보 페이지로 이동
	response.sendRedirect(root + "/shop/order.jsp");
%>
