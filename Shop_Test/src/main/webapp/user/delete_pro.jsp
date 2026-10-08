<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<jsp:useBean id="userDAO" class="shop.dao.UserRepository" />

<%
	// 회원 탈퇴 처리
	String root = request.getContextPath();
	String cookiePath = root.isEmpty() ? "/" : root;
	String loginId = (String) session.getAttribute("loginId");
	
	if( loginId == null || loginId.isEmpty() ) {
		userDAO.close();
		response.sendRedirect("login.jsp");
		return;
	}
	
	userDAO.deleteToken(loginId);			// 자동 로그인 토큰 삭제
	int result = userDAO.delete(loginId);	// 회원 삭제
	userDAO.close();
	
	if( result > 0 ) {
		// 자동 로그인 쿠키 삭제
		Cookie cookieRememberMe = new Cookie("rememberMe", "");
		Cookie cookieToken = new Cookie("token", "");
		cookieRememberMe.setMaxAge(0);
		cookieToken.setMaxAge(0);
		cookieRememberMe.setPath(cookiePath);
		cookieToken.setPath(cookiePath);
		response.addCookie(cookieRememberMe);
		response.addCookie(cookieToken);
		
		// 세션 무효화 (로그아웃)
		session.invalidate();
		response.sendRedirect("complete.jsp?msg=3");
	} else {
		response.sendRedirect("update.jsp");
	}
%>
