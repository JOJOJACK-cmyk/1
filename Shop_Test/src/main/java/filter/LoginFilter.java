package filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import shop.dao.UserRepository;
import shop.dto.PersistentLogin;

/**
 * 로그인 필터 (자동 로그인)
 * - 쿠키 정보와 DB 정보를 확인하여 자동 로그인 기능을 구현
 * - 필터 매핑은 web.xml 에서 설정 (/*)
 */
public class LoginFilter extends HttpFilter implements Filter {
	
	private static final long serialVersionUID = 1L;

	@Override
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpSession session = request.getSession();
		String loginId = (String) session.getAttribute("loginId");
		
		// 이미 로그인된 경우, 바로 다음 필터 호출
		if( loginId != null && !loginId.isEmpty() ) {
			chain.doFilter(request, response);
			return;
		}
		
		// 쿠키 정보 "rememberMe", "token" 가져오기
		String rememberMe = null;
		String token = null;
		Cookie[] cookies = request.getCookies();
		if( cookies != null ) {
			for (Cookie cookie : cookies) {
				switch (cookie.getName()) {
					case "rememberMe" : rememberMe = cookie.getValue(); break;
					case "token" : token = cookie.getValue(); break;
				}
			}
		}
		
		// "rememberMe", "token" 이 모두 존재하는 경우 → 자동 로그인 설정한 경우
		if( rememberMe != null && token != null && !token.isEmpty() ) {
			UserRepository userDAO = new UserRepository();
			// persistent_logins 테이블에서 token 으로 login_id 조회
			PersistentLogin persistentLogin = userDAO.selectTokenByToken(token);
			if( persistentLogin != null ) {
				// 세션에 "loginId" 로 등록
				session.setAttribute("loginId", persistentLogin.getUserId());
				System.out.println("자동 로그인 : " + persistentLogin.getUserId());
			}
			userDAO.close();
		}
		
		// 다음 필터 호출
		chain.doFilter(request, response);
	}
	
}
