<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/layout/jstl.jsp" %>
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
		<h1 class="display-5 fw-bold text-body-emphasis">회원 정보</h1>
		
	</div>
	<!-- 회원 가입/수정/탈퇴 완료 -->
	<div class="container mb-5 text-center">
		<c:choose>
			<c:when test="${ param.msg == '0' }">
				<h2 class="mb-5">${ sessionScope.loginId }님 환영 합니다.</h2>
			</c:when>
			<c:when test="${ param.msg == '1' }">
				<h2 class="mb-5">회원 가입이 완료되었습니다.</h2>
			</c:when>
			<c:when test="${ param.msg == '2' }">
				<h2 class="mb-5">회원 정보가 수정되었습니다.</h2>
			</c:when>
			<c:when test="${ param.msg == '3' }">
				<h2 class="mb-5">회원 정보가 삭제되었습니다.</h2>
			</c:when>
		</c:choose>
		<a href="${ root }/" class="btn btn-primary">메인 화면</a>
	</div>
	
	<jsp:include page="/layout/footer.jsp" />
	<jsp:include page="/layout/script.jsp" />
</body>
</html>







