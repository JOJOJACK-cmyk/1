package shop.dao;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class JDBConnection {
	
	public Connection con;				// 연결된 드라이버에 SQL을 요청할 객체를 생성하는 클래스
	public Statement stmt;				// SQL 실행 요청을 하는 클래스
	public PreparedStatement psmt;		// Statement 에서 ? 파라미터 확장기능을 추가로 제공하는 클래스
	public ResultSet rs;				// SQL 실행 결과를 받아오는 클래스
	
	// 기본 생성자
	public JDBConnection() {
		
		// db.properties 파일에 저장된 데이터 소스 정보를 읽어들여 DB 드라이버와 연결
		try {
			// 클래스패스(WEB-INF/classes)의 db.properties 읽기
			ClassLoader classLoader = JDBConnection.class.getClassLoader();
			Properties properties = new Properties();
			try (InputStream is = classLoader.getResourceAsStream("db.properties");
				 Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				properties.load(reader);
			}
			
			String driver = properties.getProperty("driver");
			String url = properties.getProperty("url");
			String id = properties.getProperty("id");
			String pw = properties.getProperty("pw");
			
			// mysql-connector-j.xxx.jar 드라이버의 클래스를 로드한다.
			Class.forName(driver);		 
			
			// DB에 연결
			// URL : jdbc:mysql://도메인:[PORT]/[스키마]?옵션파라미터
			con = DriverManager.getConnection(url, id, pw);
			
			System.out.println("DB 연결 성공");
		} catch (Exception e) {
			System.err.println("DB 연결 실패");
			e.printStackTrace();
		}
		
	}
	
	/**
	 * DB 자원 해제
	 */
	public void close() {
		try {
			if( rs != null ) rs.close();
			if( psmt != null ) psmt.close();
			if( stmt != null ) stmt.close();
			if( con != null ) con.close();
		} catch (Exception e) {
			System.err.println("DB 자원 해제 중, 에러 발생!");
		}
	}

}
