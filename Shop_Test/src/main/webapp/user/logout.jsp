<%@page import="shop.dao.UserRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%	
	String root = request.getContextPath();
	String cookiePath = root.isEmpty() ? "/" : root;
	
	// 자동 로그인 토큰 삭제 (DB)
	String loginId = (String) session.getAttribute("loginId");
	if( loginId != null ) {
		UserRepository userDAO = new UserRepository();
		userDAO.deleteToken(loginId);
		userDAO.close();
	}

	// 자동 로그인, 토큰 쿠키 삭제
	Cookie cookieRememberMe = new Cookie("rememberMe", "");
	Cookie cookieToken = new Cookie("token", "");
	cookieRememberMe.setMaxAge(0);
	cookieToken.setMaxAge(0);
	cookieRememberMe.setPath(cookiePath);
	cookieToken.setPath(cookiePath);
	
	// 세션 무효화
	session.invalidate();
	
	// 쿠키 전달
	response.addCookie(cookieRememberMe);
	response.addCookie(cookieToken);
	
	response.sendRedirect(root + "/");
%>
