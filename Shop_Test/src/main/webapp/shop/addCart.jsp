<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	// 장바구니 등록 처리 - 등록 후, 상품 정보(으)로 이동
	String root = request.getContextPath();
	String productId = request.getParameter("id");
	
	// 상품 정보 조회
	ProductRepository productDAO = new ProductRepository();
	Product product = productDAO.getProductById(productId);
	productDAO.close();
	
	// 존재하지 않는 상품일 때
	if( product == null ) {
		response.sendRedirect(root + "/shop/products.jsp");
		return;
	}
	
	// 장바구니 목록 (세션)
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( cartList == null ) {
		cartList = new ArrayList<Product>();
		session.setAttribute("cartList", cartList);
	}
	
	// 이미 담긴 상품이면 개수 누적, 아니면 신규 추가
	boolean exist = false;
	for( Product item : cartList ) {
		if( item.getProductId().equals(productId) ) {
			item.setQuantity( item.getQuantity() + 1 );
			exist = true;
			break;
		}
	}
	if( !exist ) {
		product.setQuantity(1);
		cartList.add(product);
	}
	
	response.sendRedirect(root + "/shop/product.jsp?id=" + productId);
%>
