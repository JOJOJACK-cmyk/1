<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	// 상품 삭제 처리 - 삭제 후, 상품 편집으로 이동
	String root = request.getContextPath();
	String productId = request.getParameter("id");
	
	if( productId != null && !productId.isEmpty() ) {
		ProductRepository productDAO = new ProductRepository();
		productDAO.delete(productId);
		productDAO.close();
	}
	
	response.sendRedirect(root + "/shop/editProducts.jsp");
%>
