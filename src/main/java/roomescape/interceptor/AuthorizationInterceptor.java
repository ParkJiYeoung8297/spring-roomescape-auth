package roomescape.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import roomescape.config.AdminOnly;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.exception.CustomException;
import roomescape.exception.ErrorCode;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ){

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean classAnnotated = handlerMethod.getBeanType().isAnnotationPresent(AdminOnly.class);

        if (!classAnnotated) {
            return true;
        }

        Member member = (Member) request.getAttribute("member");

        if (member ==null || member.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return true;
    }
}
