package roomescape.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import roomescape.config.AdminOnly;
import roomescape.config.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.AdminReservationResponse;
import roomescape.dto.ReservationUpdateRequest;
import roomescape.service.AdminReservationService;

@RequestMapping("/admin/reservations")
@RestController
@AdminOnly
public class AdminReservationController {
    private final AdminReservationService adminReservationService;

    public AdminReservationController(AdminReservationService adminReservationService) {
        this.adminReservationService = adminReservationService;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<AdminReservationResponse> getAllReservations(@LoginMember Member member) {
        return adminReservationService.getAllReservations(member);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{reservationId}")
    public void updateReservation(
            @LoginMember Member member,
            @PathVariable long reservationId,
            @Valid @RequestBody ReservationUpdateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();
        adminReservationService.update(member, reservationId, now, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteReservation(
            @LoginMember Member member,
            @PathVariable Long id) {
        LocalDateTime now = LocalDateTime.now();
        adminReservationService.delete(member, now, id);
    }

}
