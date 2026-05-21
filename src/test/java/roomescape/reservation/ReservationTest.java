package roomescape.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import roomescape.domain.Reservation;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.domain.Time;
import roomescape.exception.CustomException;

class ReservationTest {

    private final Time time;
    private final Theme theme;
    private final Store store;

    public ReservationTest() {
        this.time = new Time(1L, LocalTime.of(15, 40));
        this.theme = new Theme(1L, "공포의 저택", "버려진 저택에서 탈출하라! 어둠 속에 숨겨진 비밀을 밝혀야 살 수 있다.",
                "https://picsum.photos/seed/haunted/400/250");
        this.store = new Store(1L, "더 크라임씬", 2L);
    }

    @Test
    void 예약_생성() {
        Reservation reservation = new Reservation(1L, 2L, "브라운", LocalDate.of(2023, 8, 5), time, theme, store);
        assertThat(reservation.getId()).isEqualTo(1L);
        assertThat(reservation.getName()).isEqualTo("브라운");
        assertThat(reservation.getDate()).isEqualTo(LocalDate.of(2023, 8, 5));
        assertThat(reservation.getTime()).isEqualTo(time);
    }

    @Test
    void 이름이_null이면_예외() {
        Time time = new Time(1L, LocalTime.of(15, 40));

        assertThatThrownBy(() -> new Reservation(1L, 2L, null, LocalDate.of(2023, 8, 5), time, theme, store))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 이름이_공백이면_예외() {
        Time time = new Time(1L, LocalTime.of(15, 40));

        assertThatThrownBy(() -> new Reservation(1L, 2L, "   ", LocalDate.of(2023, 8, 5), time, theme, store))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 날짜가_null이면_예외() {
        Time time = new Time(1L, LocalTime.of(15, 40));

        assertThatThrownBy(() -> new Reservation(1L, 2L, "브라운", null, time, theme, store))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 시간이_null이면_예외() {
        assertThatThrownBy(() -> new Reservation(1L, 2L, "브라운", LocalDate.of(2023, 8, 5), null, theme, store))
                .isInstanceOf(CustomException.class);
    }
}
