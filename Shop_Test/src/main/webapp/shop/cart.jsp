<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="shop.dto.Product"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
<%
	// 세션에서 장바구니 목록 가져오기
	List<Product> cartList = (List<Product>) session.getAttribute("cartList");
	if( cartList == null ) cartList = new ArrayList<Product>();
	
	int sum = 0;
	for( Product product : cartList ) {
		sum += product.getUnitPrice() * product.getQuantity();
	}
	request.setAttribute("cartList", cartList);
	request.setAttribute("sum", sum);
	request.setAttribute("cartId", session.getId());
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
		<h1 class="display-5 fw-bold text-body-emphasis">장바구니</h1>
		<div class="col-lg-6 mx-auto">
			<p class="lead mb-4">장바구니 목록 입니다.</p>
		</div>
	</div>
	
	<!-- 장바구니 영역 -->
	<div class="container order">
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
					<td>
						<a href="${ root }/shop/deleteCart.jsp?id=${ product.productId }" class="btn btn-danger">삭제</a>
					</td>
				</tr>
				</c:forEach>
			</tbody>
			<tfoot>
				<c:choose>
					<c:when test="${ empty cartList }">
						<tr>
							<td colspan="5">추가된 상품이 없습니다.</td>
						</tr>
					</c:when>
					<c:otherwise>
						<tr>
							<td></td>
							<td></td>
							<td>총액</td>
							<td id="cart-sum">${ sum }</td>
							<td></td>
						</tr>
					</c:otherwise>
				</c:choose>
			</tfoot>
		</table>
		
		<!-- 버튼 영역 -->
		<div class="d-flex justify-content-between align-items-center p-3">
			<a href="${ root }/shop/deleteCart.jsp?cartId=${ cartId }" class="btn btn-lg btn-danger"
			   onclick="return confirm('장바구니를 비우시겠습니까?')">전체삭제</a>
			<a href="javascript:;" class="btn btn-lg btn-primary" onclick="order()">주문하기</a>
		</div>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
	
	<script>
		let cartCount = ${ fn:length(cartList) }
		
		// 주문하기 - 장바구니가 비어있으면 유효성 검사
		function order() {
			if( cartCount == 0 ) {
				alert('장바구니에 담긴 상품이 없습니다.')
				return
			}
			location.href = '${ root }/shop/ship.jsp?cartId=${ cartId }'
		}
	</script>
</body>
</html>
