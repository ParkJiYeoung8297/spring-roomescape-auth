package roomescape.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
public class AuthController {

    @PostMapping("/login")
    public void login(HttpSession session) {
        session.setAttribute("loginMemberId", 1L);
    }
}
