package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.Product;

public class ProductRepository extends JDBConnection {
	
	/**
	 * 상품 목록
	 * @return
	 */
	public List<Product> list() {
		List<Product> productList = new ArrayList<Product>();
		String sql = " SELECT * FROM product ORDER BY product_id ";
		try {
			psmt = con.prepareStatement(sql);
			rs = psmt.executeQuery();
			while( rs.next() ) {
				productList.add( map() );
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("상품 목록 조회 중, 에러 발생!");
			e.printStackTrace();
		}
		return productList;
	}
	
	
	/**
	 * 상품 목록 검색
	 * - 상품명, 설명, 제조업체, 카테고리 중 keyword 를 포함하는 상품
	 * @param keyword
	 * @return
	 */
	public List<Product> list(String keyword) {
		if( keyword == null || keyword.trim().isEmpty() ) {
			return list();
		}
		List<Product> productList = new ArrayList<Product>();
		String sql = " SELECT * FROM product "
				   + " WHERE name LIKE CONCAT('%', ?, '%') "
				   + "    OR description LIKE CONCAT('%', ?, '%') "
				   + "    OR manufacturer LIKE CONCAT('%', ?, '%') "
				   + "    OR category LIKE CONCAT('%', ?, '%') "
				   + " ORDER BY product_id ";
		try {
			psmt = con.prepareStatement(sql);
			keyword = keyword.trim();
			for (int i = 1; i <= 4; i++) {
				psmt.setString(i, keyword);
			}
			rs = psmt.executeQuery();
			while( rs.next() ) {
				productList.add( map() );
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("상품 목록 검색 중, 에러 발생!");
			e.printStackTrace();
		}
		return productList;
	}
	
	/**
	 * 상품 조회
	 * @param productId
	 * @return
	 */
	public Product getProductById(String productId) {
		Product product = null;
		String sql = " SELECT * FROM product WHERE product_id = ? ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, productId);
			rs = psmt.executeQuery();
			if( rs.next() ) {
				product = map();
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("상품 조회 중, 에러 발생!");
			e.printStackTrace();
		}
		return product;
	}
	
	
	/**
	 * 상품 등록
	 * @param product
	 * @return
	 */
	public int insert(Product product) {
		int result = 0;
		String sql = " INSERT INTO product "
				   + " (product_id, name, unit_price, description, manufacturer, category, units_in_stock, `condition`, file) "
				   + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, product.getProductId());
			psmt.setString(2, product.getName());
			psmt.setInt(3, product.getUnitPrice() == null ? 0 : product.getUnitPrice());
			psmt.setString(4, product.getDescription());
			psmt.setString(5, product.getManufacturer());
			psmt.setString(6, product.getCategory());
			psmt.setLong(7, product.getUnitsInStock());
			psmt.setString(8, product.getCondition());
			psmt.setString(9, product.getFile());
			result = psmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("상품 등록 중, 에러 발생!");
			e.printStackTrace();
		}
		System.out.println("상품 " + result + "개가 등록되었습니다.");
		return result;
	}
	
	
	/**
	 * 상품 수정
	 * @param product
	 * @return
	 */
	public int update(Product product) {
		int result = 0;
		String sql = " UPDATE product "
				   + " SET name = ?, unit_price = ?, description = ?, manufacturer = ?, "
				   + "     category = ?, units_in_stock = ?, `condition` = ?, file = ? "
				   + " WHERE product_id = ? ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, product.getName());
			psmt.setInt(2, product.getUnitPrice() == null ? 0 : product.getUnitPrice());
			psmt.setString(3, product.getDescription());
			psmt.setString(4, product.getManufacturer());
			psmt.setString(5, product.getCategory());
			psmt.setLong(6, product.getUnitsInStock());
			psmt.setString(7, product.getCondition());
			psmt.setString(8, product.getFile());
			psmt.setString(9, product.getProductId());
			result = psmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("상품 수정 중, 에러 발생!");
			e.printStackTrace();
		}
		System.out.println("상품 " + result + "개가 수정되었습니다.");
		return result;
	}
	
	
	/**
	 * 상품 재고 감소 (주문 시)
	 * @param productId
	 * @param amount
	 * @return
	 */
	public int decreaseStock(String productId, int amount) {
		int result = 0;
		String sql = " UPDATE product SET units_in_stock = units_in_stock - ? WHERE product_id = ? ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setInt(1, amount);
			psmt.setString(2, productId);
			result = psmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("상품 재고 감소 중, 에러 발생!");
			e.printStackTrace();
		}
		return result;
	}
	
	
	/**
	 * 상품 삭제
	 * - 해당 상품의 입출고 내역을 먼저 삭제한 뒤 상품 삭제
	 * @param productId
	 * @return
	 */
	public int delete(String productId) {
		int result = 0;
		try {
			psmt = con.prepareStatement(" DELETE FROM product_io WHERE product_id = ? ");
			psmt.setString(1, productId);
			psmt.executeUpdate();
			
			psmt = con.prepareStatement(" DELETE FROM product WHERE product_id = ? ");
			psmt.setString(1, productId);
			result = psmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("상품 삭제 중, 에러 발생!");
			e.printStackTrace();
		}
		System.out.println("상품 " + result + "개가 삭제되었습니다.");
		return result;
	}
	
	
	/**
	 * ResultSet 의 현재 행 → Product 객체
	 * @return
	 * @throws SQLException
	 */
	private Product map() throws SQLException {
		Product product = new Product();
		product.setProductId( rs.getString("product_id") );
		product.setName( rs.getString("name") );
		product.setUnitPrice( rs.getInt("unit_price") );
		product.setDescription( rs.getString("description") );
		product.setManufacturer( rs.getString("manufacturer") );
		product.setCategory( rs.getString("category") );
		product.setUnitsInStock( rs.getLong("units_in_stock") );
		product.setCondition( rs.getString("condition") );
		product.setFile( rs.getString("file") );
		product.setQuantity( rs.getInt("quantity") );
		return product;
	}

}
