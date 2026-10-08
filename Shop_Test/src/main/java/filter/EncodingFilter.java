package filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 인코딩 필터
 * - 요청 및 응답에 대한 문자 인코딩 타입을 UTF-8 로 설정
 */
public class EncodingFilter implements Filter {
	
	private String encoding; 
	
	/**
	 * 필터 초기화
	 * - web.xml 에서 설정한 초기 파라미터 "encoding" 을 가져와 멤버변수에 초기화
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		encoding = filterConfig.getInitParameter("encoding");
		if( encoding == null || encoding.isEmpty() ) {
			encoding = "UTF-8";
		}
		System.out.println("EncodingFilter 초기화 - encoding : " + encoding);
	}
	
	/**
	 * 필터 실행
	 * - 요청 객체와 응답 객체에 문자 인코딩을 설정하고 다음 필터를 호출
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		request.setCharacterEncoding(encoding);
		response.setCharacterEncoding(encoding);
		
		// 다음 필터 호출
		chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {
	}
	
}
