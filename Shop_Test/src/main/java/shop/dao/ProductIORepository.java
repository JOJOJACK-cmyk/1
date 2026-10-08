package shop.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import shop.dto.Product;

public class ProductIORepository extends JDBConnection {

	/**
	 * 상품 입출고 등록
	 * - product 의 productId, orderNo, quantity(입출고량), type(IN/OUT), userId 를 사용
	 * @param product
	 * @return
	 */
	public int insert(Product product) {
		int result = 0;
		String sql = " INSERT INTO product_io (product_id, order_no, amount, type, user_id) "
				   + " VALUES (?, ?, ?, ?, ?) ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, product.getProductId());
			if( product.getOrderNo() > 0 ) {
				psmt.setInt(2, product.getOrderNo());
			} else {
				psmt.setNull(2, java.sql.Types.INTEGER);		// 입고(IN) 시, 주문번호 없음
			}
			psmt.setInt(3, product.getQuantity());
			psmt.setString(4, product.getType());
			psmt.setString(5, product.getUserId());
			result = psmt.executeUpdate();
		} catch (SQLException e) {
			System.err.println("상품 입출고 등록 중, 에러 발생!");
			e.printStackTrace();
		}
		System.out.println("상품 입출고 " + result + "건이 등록되었습니다.");
		return result;
	}
	
	
	/**
	 * 상품 입출고 내역 조회
	 * @param productId
	 * @return
	 */
	public List<Product> list(String productId) {
		List<Product> ioList = new ArrayList<Product>();
		String sql = " SELECT * FROM product_io WHERE product_id = ? ORDER BY io_no DESC ";
		try {
			psmt = con.prepareStatement(sql);
			psmt.setString(1, productId);
			rs = psmt.executeQuery();
			while( rs.next() ) {
				Product product = new Product();
				product.setProductId( rs.getString("product_id") );
				product.setOrderNo( rs.getInt("order_no") );
				product.setQuantity( rs.getInt("amount") );
				product.setType( rs.getString("type") );
				product.setUserId( rs.getString("user_id") );
				ioList.add(product);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("상품 입출고 조회 중, 에러 발생!");
			e.printStackTrace();
		}
		return ioList;
	}
	

}
