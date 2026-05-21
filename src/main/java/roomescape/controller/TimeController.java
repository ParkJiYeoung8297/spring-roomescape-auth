package roomescape.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import roomescape.config.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.TimeResponse;
import roomescape.service.TimeService;

@RestController
@RequestMapping("/times")
public class TimeController {
    private final TimeService timeService;

    public TimeController(TimeService timeService) {
        this.timeService = timeService;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<TimeResponse> getTimes(@LoginMember Member member) {
        return timeService.findAll();
    }
}
