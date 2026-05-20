package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;

import roomescape.domain.Member;
import roomescape.dto.AdminReservationResponse;
import roomescape.dto.ReservationResponse;
import roomescape.repository.ReservationDao;

@Service
public class AdminReservationService {

    private final ReservationDao reservationDao;

    public AdminReservationService(ReservationDao reservationDao) {
        this.reservationDao = reservationDao;
    }

    public List<AdminReservationResponse> getAllReservations() {
        // todo : 관리자 권한 검증
        return reservationDao.findAll().stream()
                .map(r -> AdminReservationResponse.from(r, r.getTheme()))
                .toList();
    }

}
