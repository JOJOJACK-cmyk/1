<%@page import="shop.dto.Product"%>
<%@page import="java.util.List"%>
<%@page import="shop.dao.OrderRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	String root = request.getContextPath();
	
	// 비회원 주문 내역 세션에 등록 처리
	String phone = request.getParameter("phone");
	String orderPw = request.getParameter("orderPw");
	
	phone = phone == null ? "" : phone.replaceAll("[^0-9]", "");
	orderPw = orderPw == null ? "" : orderPw;
	
	OrderRepository orderDAO = new OrderRepository();
	List<Product> orderList = orderDAO.list(phone, orderPw);
	orderDAO.close();
	
	session.setAttribute("orderPhone", phone);
	session.setAttribute("orderList", orderList);
		
	response.sendRedirect(root + "/user/order.jsp");

%>
