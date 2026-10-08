<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%
	// 수정할 상품 정보 조회
	ProductRepository productDAO = new ProductRepository();
	Product product = productDAO.getProductById( request.getParameter("id") );
	productDAO.close();
	if( product == null ) {
		response.sendRedirect(request.getContextPath() + "/shop/editProducts.jsp");
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
		<h1 class="display-5 fw-bold text-body-emphasis">상품 수정</h1>
		<div class="col-lg-6 mx-auto">
			<p class="lead mb-4">Shop 쇼핑몰 입니다.</p>
		</div>
	</div>
	
	<!-- 상품 수정 영역 -->
	<div class="container shop mb-5">
		<!-- 기존 이미지 -->
		<div class="text-center mb-4">
			<img src="${ root }/shop/img?id=${ product.productId }" class="w-100 p-2" alt="${ product.name }">
		</div>
		<form action="${ root }/shop/update_pro.jsp" name="product" method="post" enctype="multipart/form-data"
			  onsubmit="return checkProduct()">
			<input type="hidden" id="temp-condition" value="${ product.condition }" />
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">상품 이미지</label>
				<input type="file" class="form-control col-md-10" name="file" accept="image/*">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">상품 코드</label>
				<input type="text" class="form-control col-md-10" name="productId" placeholder="P + 숫자 6자리 (예: P100005)" maxlength="7" readonly value="${ product.productId }">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">상품명</label>
				<input type="text" class="form-control col-md-10" name="name" required value="${ fn:escapeXml(product.name) }">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">가격</label>
				<input type="number" class="form-control col-md-10" name="unitPrice" min="0" required value="${ product.unitPrice }">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text w-100">상세 정보</label>
				<textarea class="form-control w-100" name="description" style="height: 200px !important;">${ fn:escapeXml(product.description) }</textarea>
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">제조사</label>
				<input type="text" class="form-control col-md-10" name="manufacturer" value="${ fn:escapeXml(product.manufacturer) }">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">분류</label>
				<input type="text" class="form-control col-md-10" name="category" value="${ fn:escapeXml(product.category) }">
			</div>
			<div class="input-group mb-3 row">
				<label class="input-group-text col-md-2">재고 수</label>
				<input type="number" class="form-control col-md-10" name="unitsInStock" min="0" required value="${ product.unitsInStock }">
			</div>
			<div class="input-group mb-3 row">
				<div class="col-md-2 p-0">
					<label class="input-group-text">상태</label>
				</div>
				<div class="col-md-10 d-flex align-items-center">
					<div class="radio-box d-flex">
						<div class="radio-item mx-4">
							<input type="radio" class="form-check-input" name="condition" value="NEW" id="condition-new">
							<label for="condition-new">신규 제품</label>
						</div>
						<div class="radio-item mx-4">
							<input type="radio" class="form-check-input" name="condition" value="OLD" id="condition-old">
							<label for="condition-old">중고 제품</label>
						</div>
						<div class="radio-item mx-4">
							<input type="radio" class="form-check-input" name="condition" value="RE" id="condition-re">
							<label for="condition-re">재생 제품</label>
						</div>
					</div>
				</div>
			</div>
			<div class="d-flex justify-content-between mt-5 mb-5">
				<a href="${ root }/shop/products.jsp" class="btn btn-lg btn-secondary">목록</a>
				<input type="submit" class="btn btn-lg btn-success" value="수정" />
			</div>
		</form>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
	
	<script>
		// 상태 정보 체크 (자바스크립트로 처리)
		let tempCondition = document.getElementById('temp-condition').value
		let radios = document.product.condition
		for( let i = 0 ; i < radios.length ; i++ ) {
			if( radios[i].value == tempCondition ) {
				radios[i].checked = true
			}
		}
	</script>
</body>
</html>
