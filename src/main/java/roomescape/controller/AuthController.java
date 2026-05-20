package roomescape.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import roomescape.dto.LoginRequest;

@RestController
public class AuthController {

    @PostMapping("/login")
    public void login(
            @RequestBody LoginRequest request,
            HttpSession session) {
        session.setAttribute("loginMemberId", request.memberId());
        System.out.println(session.getId());
        System.out.println(session.getAttribute("loginMemberId"));
    }

    @PostMapping("/logout")
    public void logout(
            HttpSession session) {
        session.invalidate();
    }
}
