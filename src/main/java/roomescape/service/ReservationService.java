package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Role;
import roomescape.domain.Time;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.dto.ReservationUpdateRequest;
import roomescape.exception.CustomException;
import roomescape.exception.ErrorCode;
import roomescape.repository.ReservationDao;
import roomescape.repository.TimeDao;

@Service
public class ReservationService {
    private final ReservationDao reservationDao;
    private final TimeDao timeDao;

    public ReservationService(ReservationDao reservationDao, TimeDao timeDao) {
        this.reservationDao = reservationDao;
        this.timeDao = timeDao;
    }


    public ReservationResponse save(long memberId, LocalDateTime now, ReservationRequest request) {
        Time reservationTime = timeDao.findById(request.timeId());
        LocalDateTime time = LocalDateTime.of(request.date(), reservationTime.getStartAt());
        validateDateAndTimeNotPast(now, time);

        try {
            Long id = reservationDao.save(memberId, request.name(), request.date(), request.timeId(), request.themeId(), request.storeId());
            Reservation reservation = reservationDao.findById(id);
            return ReservationResponse.from(reservation);
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION);
        }
    }

    public ReservationResponse findById(Member member, long reservationId) {
        Reservation reservation = reservationDao.findById(reservationId);
        validateOwner(member.getId(), reservation.getMemberId());
        return ReservationResponse.from(reservation);
    }


    public List<ReservationResponse> findMyReservations(long userId) {
        return reservationDao.findAllByUserId(userId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public void update(Member member, Long reservationId, LocalDateTime now, ReservationUpdateRequest request) {
        try {
            Time time = timeDao.findById(request.timeId());
            Reservation reservation = reservationDao.findById(reservationId);
            validateOwner(member.getId(), reservation.getMemberId());

            LocalDateTime targetDateTime = LocalDateTime.of(request.targetDate(), time.getStartAt());
            validateDateAndTimeNotPast(now, targetDateTime);
            reservationDao.updateDateAndTimeById(reservationId, request.targetDate(), time.getId());
        } catch (DuplicateKeyException e) {
            throw new CustomException(ErrorCode.DUPLICATE_RESERVATION);
        }
    }

    public void delete(Member member, LocalDateTime now, Long id) {
        Reservation reservation = reservationDao.findById(id);
        validateOwner(member.getId(), reservation.getMemberId());

        LocalDateTime localDateTime = LocalDateTime.of(reservation.getDate(), reservation.getTime().getStartAt());
        if (now.isAfter(localDateTime)) {
            throw new CustomException(ErrorCode.UNALLOWED_DELETE_PAST_RESERVATION);
        }
        reservationDao.delete(id);
    }

    private void validateDateAndTimeNotPast(LocalDateTime now, LocalDateTime reservationTime) {
        if (now.isAfter(reservationTime)) {
            throw new CustomException(ErrorCode.PAST_DATE_RESERVATION);
        }
    }

    private void validateOwner(long memberId, long reservationId) {
        if (memberId != reservationId) {
            throw new CustomException(ErrorCode.AUTH_REQUIRED);
        }
    }

}
