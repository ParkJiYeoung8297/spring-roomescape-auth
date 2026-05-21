package roomescape.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import roomescape.domain.Member;
import roomescape.domain.repository.MemberRepository;
import roomescape.exception.CustomException;
import roomescape.exception.ErrorCode;
import roomescape.service.AdminReservationService;

public class LoginCheckInterceptor implements HandlerInterceptor {
    private static final String LOGIN_MEMBER_ID = "loginMemberId";
    private final MemberRepository memberRepository;

    public LoginCheckInterceptor(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(LOGIN_MEMBER_ID) == null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long memberId = (Long) session.getAttribute(LOGIN_MEMBER_ID);
        if (memberId == null) {
            throw new CustomException(ErrorCode.AUTH_LOGIN_REQUIRED);
        }

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        request.setAttribute("member", member);

        return true;
    }
}
