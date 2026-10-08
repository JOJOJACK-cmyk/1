<!-- 
	회원 가입 처리
 -->
<%@page import="shop.dao.UserRepository"%>
<%@page import="shop.dto.User"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%
	User user = new User();
	
	String id = request.getParameter("id");
	String pw = request.getParameter("pw");
	String pwConfirm = request.getParameter("pw_confirm");
	String name = request.getParameter("name");
	String gender = request.getParameter("gender");
	String year = request.getParameter("year");
	String month = request.getParameter("month");
	String day = request.getParameter("day");
	String email1 = request.getParameter("email1");
	String email2 = request.getParameter("email2");
	String phone = request.getParameter("phone");
	String address = request.getParameter("address");
	
	// 서버측 유효성 검사
	boolean valid = id != null && id.matches("^[a-zA-Z가-힣].*")
				 && pw != null && pw.matches("^(?=.*[^a-zA-Z0-9])[a-zA-Z0-9!-/:-@\\[-`{-~]{6,}$")
				 && pw.equals(pwConfirm)
				 && name != null && name.matches("^[가-힣]+$");
	if( !valid ) {
		response.sendRedirect("join.jsp?error=1");
		return;
	}
	
	// 생일 : 연/월/일
	String birth = "";
	if( year != null && !year.isEmpty() && day != null && !day.isEmpty() ) {
		birth = year + "/" + month + "/" + day;
	}
	// 이메일 : 아이디@도메인
	String mail = "";
	if( email1 != null && !email1.isEmpty() ) {
		mail = email1 + "@" + email2;
	}
	
	user.setId(id);
	user.setPassword(pw);
	user.setName(name);
	user.setGender(gender);
	user.setBirth(birth);
	user.setMail(mail);
	user.setPhone(phone);
	user.setAddress(address);
	
	UserRepository userDAO = new UserRepository();
	int result = userDAO.insert(user);
	userDAO.close();
	
	if( result > 0 ) {
		response.sendRedirect("complete.jsp?msg=1");
	} else {
		// 아이디 중복 등으로 가입 실패
		response.sendRedirect("join.jsp?error=0");
	}
%>
