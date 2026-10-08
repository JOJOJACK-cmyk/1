package shop.dao;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import shop.dto.Order;
import shop.dto.Product;

public class OrderRepository extends JDBConnection {
	
	/**
	 * 주문 등록
	 * - 등록 후, 생성된 주문번호를 order.orderNo 에 저장
	 * @param order
	 * @return
	 */
	public int insert(Order order) {
		int result = 0;
		String sql = " INSERT INTO `order` "
				   + " (ship_name, zip_code, country, address, date, order_pw, user_id, total_price, phone) "
				   + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ";
		try {
			psmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			psmt.setString(1, order.getShipName());
			psmt.setString(2, order.getZipCode());
			psmt.setString(3, order.getCountry());
			psmt.setString(4, order.getAddress());
			psmt.setString(5, order.getDate());
			psmt.setString(6, order.getOrderPw());
			psmt.setString(7, order.getUserId());
			psmt.setInt(8, order.getTotalPrice());
			psmt.setString(9, order.getPhone());
			result = psmt.executeUpdate();
			
			rs = psmt.getGeneratedKeys();
			if( rs.next() ) {
				order.setOrderNo( rs.getInt(1) );
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("주문 등록 중, 에러 발생!");
			e.printStackTrace();
		}
		System.out.println("주문 " + result + "건이 등록되었습니다.");
		return result;
	}

	/**
	 * 최근 등록한 orderNo 
	 * @return
	 */
	public int lastOrderNo() {
		int orderNo = 0;
		String sql = " SELECT MAX(order_no) FROM `order` ";
		try {
			psmt = con.prepareStatement(sql);
			rs = psmt.executeQuery();
			if( rs.next() ) {
				orderNo = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("최근 주문번호 조회 중, 에러 발생!");
			e.printStackTrace();
		}
		return orderNo;
	}

	
	/**
	 * 주문 내역 조회 - 회원
	 * @param userId
	 * @return
	 */
	public List<Product> list(String userId) {
		String sql = " SELECT o.order_no, p.product_id, p.name, p.unit_price, io.amount "
				   + " FROM `order` o "
				   + "      JOIN product_io io ON o.order_no = io.order_no "
				   + "      JOIN product p ON io.product_id = p.product_id "
				   + " WHERE o.user_id = ? "
				   + "   AND io.type = 'OUT' "
				   + " ORDER BY o.order_no, io.io_no ";
		return selectOrderList(sql, userId);
	}
	
	/**
	 * 주문 내역 조회 - 비회원
	 * @param phone
	 * @param orderPw
	 * @return
	 */
	public List<Product> list(String phone, String orderPw) {
		String sql = " SELECT o.order_no, p.product_id, p.name, p.unit_price, io.amount "
				   + " FROM `order` o "
				   + "      JOIN product_io io ON o.order_no = io.order_no "
				   + "      JOIN product p ON io.product_id = p.product_id "
				   + " WHERE o.phone = ? AND o.order_pw = ? "
				   + "   AND o.user_id IS NULL "
				   + "   AND io.type = 'OUT' "
				   + " ORDER BY o.order_no, io.io_no ";
		return selectOrderList(sql, phone, orderPw);
	}
	
	
	/**
	 * 주문 내역 조회 공통
	 * @param sql
	 * @param params
	 * @return
	 */
	private List<Product> selectOrderList(String sql, String... params) {
		List<Product> orderList = new ArrayList<Product>();
		try {
			psmt = con.prepareStatement(sql);
			for (int i = 0; i < params.length; i++) {
				psmt.setString(i + 1, params[i]);
			}
			rs = psmt.executeQuery();
			while( rs.next() ) {
				Product product = new Product();
				product.setOrderNo( rs.getInt("order_no") );
				product.setProductId( rs.getString("product_id") );
				product.setName( rs.getString("name") );
				product.setUnitPrice( rs.getInt("unit_price") );
				product.setQuantity( rs.getInt("amount") );
				orderList.add(product);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("주문 내역 조회 중, 에러 발생!");
			e.printStackTrace();
		}
		return orderList;
	}
	
}
