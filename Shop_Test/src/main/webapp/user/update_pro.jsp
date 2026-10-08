<%@page import="shop.dto.User"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<jsp:useBean id="userDAO" class="shop.dao.UserRepository" />
<%

	// 회원 정보 수정 처리
	String loginId = (String) session.getAttribute("loginId");
	if( loginId == null || loginId.isEmpty() ) {
		userDAO.close();
		response.sendRedirect("login.jsp");
		return;
	}
	
	String year = request.getParameter("year");
	String month = request.getParameter("month");
	String day = request.getParameter("day");
	String email1 = request.getParameter("email1");
	String email2 = request.getParameter("email2");
	
	User user = new User();
	user.setId(loginId);			// 로그인한 회원의 정보만 수정
	user.setName( request.getParameter("name") );
	user.setGender( request.getParameter("gender") );
	user.setBirth( (year != null && !year.isEmpty() && day != null && !day.isEmpty()) ? year + "/" + month + "/" + day : "" );
	user.setMail( (email1 != null && !email1.isEmpty()) ? email1 + "@" + email2 : "" );
	user.setPhone( request.getParameter("phone") );
	user.setAddress( request.getParameter("address") );

    int result = userDAO.update(user);
    userDAO.close();
    if (result >0 ){
        response.sendRedirect("complete.jsp?msg=2");
    } else {
        response.sendRedirect("update.jsp");
    }

%>
