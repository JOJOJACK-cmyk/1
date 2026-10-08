<%@page import="java.io.File"%>
<%@page import="com.oreilly.servlet.multipart.DefaultFileRenamePolicy"%>
<%@page import="com.oreilly.servlet.MultipartRequest"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%@page import="shop.dao.ProductIORepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	// 상품 등록 처리
	String root = request.getContextPath();
	
	// 파일 업로드 경로 : /static/img/upload
	String uploadDir = "/static/img/upload";
	String uploadPath = application.getRealPath(uploadDir);
	new File(uploadPath).mkdirs();
	int maxSize = 10 * 1024 * 1024;		// 10MB
	
	MultipartRequest multi = new MultipartRequest(request, uploadPath, maxSize, "UTF-8", new DefaultFileRenamePolicy());
	
	String productId = multi.getParameter("productId");
	String unitPrice = multi.getParameter("unitPrice");
	String unitsInStock = multi.getParameter("unitsInStock");
	
	// 상품 코드 유효성 검사 : P + 숫자 6자리
	if( productId == null || !productId.matches("^P[0-9]{6}$") ) {
		response.sendRedirect(root + "/shop/add.jsp");
		return;
	}
	
	Product product = new Product();
	product.setProductId(productId);
	product.setName( multi.getParameter("name") );
	product.setUnitPrice( (unitPrice == null || unitPrice.isEmpty()) ? 0 : Integer.parseInt(unitPrice) );
	product.setDescription( multi.getParameter("description") );
	product.setManufacturer( multi.getParameter("manufacturer") );
	product.setCategory( multi.getParameter("category") );
	product.setUnitsInStock( (unitsInStock == null || unitsInStock.isEmpty()) ? 0 : Long.parseLong(unitsInStock) );
	product.setCondition( multi.getParameter("condition") );
	
	// 업로드된 파일
	String fileName = multi.getFilesystemName("file");
	if( fileName != null ) {
		product.setFile( uploadDir + "/" + fileName );
	}
	
	ProductRepository productDAO = new ProductRepository();
	int result = productDAO.insert(product);
	productDAO.close();
	
	// 상품 입고(IN) 내역 등록
	if( result > 0 && product.getUnitsInStock() > 0 ) {
		Product io = new Product();
		io.setProductId(productId);
		io.setQuantity( (int) product.getUnitsInStock() );
		io.setType("IN");
		io.setUserId( (String) session.getAttribute("loginId") );
		ProductIORepository productIODAO = new ProductIORepository();
		productIODAO.insert(io);
		productIODAO.close();
	}
	
	if( result > 0 ) {
		response.sendRedirect(root + "/shop/products.jsp");
	} else {
		// 상품 코드 중복 등
		response.sendRedirect(root + "/shop/add.jsp");
	}
%>
