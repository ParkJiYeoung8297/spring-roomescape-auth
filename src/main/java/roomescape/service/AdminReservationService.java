package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Store;
import roomescape.domain.Time;
import roomescape.dto.AdminReservationResponse;
import roomescape.dto.ReservationResponse;
import roomescape.dto.ReservationUpdateRequest;
import roomescape.exception.CustomException;
import roomescape.exception.ErrorCode;
import roomescape.repository.ReservationDao;
import roomescape.repository.StoreDao;
import roomescape.repository.TimeDao;

@Service
public class AdminReservationService {

    private final ReservationDao reservationDao;
    private final TimeDao timeDao;

    public AdminReservationService(ReservationDao reservationDao, TimeDao timeDao
    ) {
        this.reservationDao = reservationDao;
        this.timeDao = timeDao;
    }

    public List<AdminReservationResponse> getAllReservations(Member owner) {
        return reservationDao.findAllByOwnerId(owner.getId()).stream()
                .map(r -> AdminReservationResponse.from(r, r.getTheme()))
                .toList();
    }

    public void update(Member owner, Long reservationId, LocalDateTime now, ReservationUpdateRequest request) {
        try {
            Time time = timeDao.findById(request.timeId());
            Reservation reservation = reservationDao.findById(reservationId);
            validateStoreOwner(owner.getId(), reservation.getStore().getMemberId());

            LocalDateTime targetDateTime = LocalDateTime.of(request.targetDate(), time.getStartAt());
            validateDateAndTimeNotPast(now, targetDateTime);
            reservationDao.updateDateAndTimeById(reservationId, request.targetDate(), time.getId());
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION);
        }
    }

    public void delete(Member owner, LocalDateTime now, Long id) {
        Reservation reservation = reservationDao.findById(id);
        validateStoreOwner(owner.getId(), reservation.getStore().getMemberId());

        LocalDateTime localDateTime = LocalDateTime.of(reservation.getDate(), reservation.getTime().getStartAt());
        if (now.isAfter(localDateTime)) {
            throw new CustomException(ErrorCode.UNALLOWED_DELETE_PAST_RESERVATION);
        }
        reservationDao.delete(id);
    }

    private void validateStoreOwner(Long ownerId, Long storeOwnerId) {
        if (!ownerId.equals(storeOwnerId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateDateAndTimeNotPast(LocalDateTime now, LocalDateTime reservationTime) {
        if (now.isAfter(reservationTime)) {
            throw new CustomException(ErrorCode.PAST_DATE_RESERVATION);
        }
    }

}
