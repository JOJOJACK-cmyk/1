<%@page import="java.util.List"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	// 상품 목록 조회 (검색어 keyword 가 있으면 검색)
	String keyword = request.getParameter("keyword");
	ProductRepository productDAO = new ProductRepository();
	List<Product> productList = productDAO.list(keyword);
	productDAO.close();
	request.setAttribute("productList", productList);
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
		<h1 class="display-5 fw-bold text-body-emphasis">상품 목록</h1>
		<div class="col-lg-6 mx-auto">
			<p class="lead mb-4">쇼핑몰 상품 목록 입니다.</p>
			<div class="d-grid gap-2 d-sm-flex justify-content-sm-center">
				<a href="${ root }/shop/add.jsp" class="btn btn-primary btn-lg px-4 gap-3">상품 등록</a>
				<a href="${ root }/shop/editProducts.jsp" class="btn btn-success btn-lg px-4 gap-3">상품 편집</a>
				<a href="${ root }/shop/cart.jsp" class="btn btn-warning btn-lg px-4 gap-3">장바구니</a>
			</div>
		</div>
	</div>
	
	<!-- 상품 목록 영역 -->
	<div class="container mb-5">
		<c:if test="${ not empty param.keyword }">
			<p class="text-center text-secondary">'<c:out value="${ param.keyword }" />' 검색 결과 : ${ fn:length(productList) }건</p>
		</c:if>
		<div class="row gy-4">
			<c:forEach var="product" items="${ productList }">
			<div class="col-md-6 col-xl-4 col-xxl-3">
				<div class="card p-3">
					<!-- 이미지 영역 -->
					<div class="img-content">
						<img src="${ root }/shop/img?id=${ product.productId }" class="w-100 p-2" alt="${ product.name }">
					</div>
					<!-- 컨텐츠 영역 -->
					<div class="content">
						<h3 class="text-center"><c:out value="${ product.name }" /></h3>
						<p><c:out value="${ product.description }" /></p>
						<p class="text-end price">₩ ${ product.unitPrice }</p>
						<p class="d-flex justify-content-between">
							<a href="${ root }/shop/cart_pro.jsp?id=${ product.productId }" class="btn btn-outline-primary" title="장바구니 담기">
								<i class="material-symbols-outlined">shopping_bag</i>
							</a>
							<a href="${ root }/shop/product.jsp?id=${ product.productId }" class="btn btn-outline-primary">상세 정보</a>
						</p>
					</div>
				</div>
			</div>
			</c:forEach>
			<c:if test="${ empty productList }">
				<div class="col-12 text-center py-5">
					<p class="lead">조회된 상품이 없습니다.</p>
				</div>
			</c:if>
		</div>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
</body>
</html>
