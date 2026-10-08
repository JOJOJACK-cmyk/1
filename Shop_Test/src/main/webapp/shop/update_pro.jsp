<%@page import="java.io.File"%>
<%@page import="com.oreilly.servlet.multipart.DefaultFileRenamePolicy"%>
<%@page import="com.oreilly.servlet.MultipartRequest"%>
<%@page import="shop.dto.Product"%>
<%@page import="shop.dao.ProductRepository"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	// 상품 수정 처리
	String root = request.getContextPath();
	
	String uploadDir = "/static/img/upload";
	String uploadPath = application.getRealPath(uploadDir);
	new File(uploadPath).mkdirs();
	int maxSize = 10 * 1024 * 1024;		// 10MB
	
	MultipartRequest multi = new MultipartRequest(request, uploadPath, maxSize, "UTF-8", new DefaultFileRenamePolicy());
	
	String productId = multi.getParameter("productId");
	String unitPrice = multi.getParameter("unitPrice");
	String unitsInStock = multi.getParameter("unitsInStock");
	
	// 기존 상품 정보
	ProductRepository productDAO = new ProductRepository();
	Product product = productDAO.getProductById(productId);
	if( product == null ) {
		productDAO.close();
		response.sendRedirect(root + "/shop/editProducts.jsp");
		return;
	}
	
	product.setName( multi.getParameter("name") );
	product.setUnitPrice( (unitPrice == null || unitPrice.isEmpty()) ? 0 : Integer.parseInt(unitPrice) );
	product.setDescription( multi.getParameter("description") );
	product.setManufacturer( multi.getParameter("manufacturer") );
	product.setCategory( multi.getParameter("category") );
	product.setUnitsInStock( (unitsInStock == null || unitsInStock.isEmpty()) ? 0 : Long.parseLong(unitsInStock) );
	product.setCondition( multi.getParameter("condition") );
	
	// 새 이미지를 업로드한 경우에만 이미지 변경
	String fileName = multi.getFilesystemName("file");
	if( fileName != null ) {
		product.setFile( uploadDir + "/" + fileName );
	}
	
	int result = productDAO.update(product);
	productDAO.close();
	
	if( result > 0 ) {
		response.sendRedirect(root + "/shop/products.jsp");
	} else {
		response.sendRedirect(root + "/shop/update.jsp?id=" + productId);
	}
%>
