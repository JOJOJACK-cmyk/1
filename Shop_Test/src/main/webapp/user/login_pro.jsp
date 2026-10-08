<!-- 로그인 처리 -->
<%@page import="java.util.UUID"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="shop.dto.User"%>
<%@page import="shop.dao.UserRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%
	String root = request.getContextPath();
	String cookiePath = root.isEmpty() ? "/" : root;
	
	String id = request.getParameter("id");
	String pw = request.getParameter("pw");
	String rememberId = request.getParameter("remember-id");		// 아이디 저장 (on / null)
	String rememberMe = request.getParameter("remember-me");		// 자동 로그인 (on / null)
	
	UserRepository userDAO = new  UserRepository();
	User loginUser = userDAO.login(id, pw);
	
	// 로그인 실패
	if( loginUser == null ) {
		userDAO.close();
		response.sendRedirect("login.jsp?error=0");
		return;
	}
	
	// 로그인 성공
	// - 세션에 아이디 등록
	session.setAttribute("loginId", loginUser.getId());
	
	// 아이디 저장
	Cookie cookieRememberId = new Cookie("rememberId", "");
	Cookie cookieUserId = new Cookie("userId", "");
	if( rememberId != null && rememberId.equals("on") ) {
		cookieRememberId.setValue("on");
		cookieUserId.setValue( URLEncoder.encode(loginUser.getId(), "UTF-8") );
		cookieRememberId.setMaxAge(60 * 60 * 24 * 7);		// 7일
		cookieUserId.setMaxAge(60 * 60 * 24 * 7);
	} else {
		cookieRememberId.setMaxAge(0);						// 쿠키 삭제
		cookieUserId.setMaxAge(0);
	}
	
	// 자동 로그인
	Cookie cookieRememberMe = new Cookie("rememberMe", "");
	Cookie cookieToken = new Cookie("token", "");
	if( rememberMe != null && rememberMe.equals("on") ) {
		// 토큰 발행 (persistent_logins 테이블에 등록 또는 갱신)
		String token = userDAO.refreshToken(loginUser.getId());
		cookieRememberMe.setValue("on");
		cookieToken.setValue(token);
		cookieRememberMe.setMaxAge(60 * 60 * 24 * 7);		// 7일
		cookieToken.setMaxAge(60 * 60 * 24 * 7);
	} else {
		userDAO.deleteToken(loginUser.getId());
		cookieRememberMe.setMaxAge(0);
		cookieToken.setMaxAge(0);
	}
	userDAO.close();
	
	// 쿠키 전달
	cookieRememberId.setPath(cookiePath);
	cookieUserId.setPath(cookiePath);
	cookieRememberMe.setPath(cookiePath);
	cookieToken.setPath(cookiePath);
	response.addCookie(cookieRememberId);
	response.addCookie(cookieUserId);
	response.addCookie(cookieRememberMe);
	response.addCookie(cookieToken);
	
	// 로그인 성공 페이지로 이동
	response.sendRedirect("complete.jsp?msg=0");		

%>
