<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	// 상품 정보 조회
	String productId = request.getParameter("id");
	ProductRepository productDAO = new ProductRepository();
	Product product = productDAO.getProductById(productId);
	productDAO.close();
	
	// 존재하지 않는 상품
	if( product == null ) {
		response.sendRedirect(request.getContextPath() + "/shop/products.jsp");
		return;
	}
	request.setAttribute("product", product);
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
		<h1 class="display-5 fw-bold text-body-emphasis">상품 정보</h1>
		<div class="col-lg-6 mx-auto">
			<p class="lead mb-4">Shop 쇼핑몰 입니다.</p>
			<div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
				<a href="${ root }/shop/products.jsp" class="btn btn-primary btn-lg px-4 gap-3">상품목록</a>
			</div>
		</div>
	</div>
	
	<!-- 상품 정보 영역 -->
	<div class="container mb-5">
		<div class="row">
			<div class="col-md-6">
				<img src="${ root }/shop/img?id=${ product.productId }" class="w-100 p-2" alt="${ product.name }">
			</div>
			<div class="col-md-6">
				<h3 class="mb-5"><c:out value="${ product.name }" /></h3>
				<table class="table">
					<tr>
						<td>상품ID :</td>
						<td>${ product.productId }</td>
					</tr>
					<tr>
						<td>제조사 :</td>
						<td><c:out value="${ product.manufacturer }" /></td>
					</tr>
					<tr>
						<td>분류 :</td>
						<td><c:out value="${ product.category }" /></td>
					</tr>
					<tr>
						<td>상태 :</td>
						<td>${ product.condition }</td>
					</tr>
					<tr>
						<td>재고 수 :</td>
						<td>${ product.unitsInStock }</td>
					</tr>
					<tr>
						<td>가격 :</td>
						<td>${ product.unitPrice }</td>
					</tr>
				</table>
				<div class="mt-4 p-2 d-flex justify-content-end">
					<a href="${ root }/shop/cart.jsp" class="btn btn-lg btn-warning mx-3">장바구니</a>
					<a href="${ root }/shop/cart_pro.jsp?id=${ product.productId }" class="btn btn-lg btn-success">주문하기</a>
				</div>
			</div>
		</div>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
</body>
</html>
