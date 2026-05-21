package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.controller.ReservationController;
import roomescape.utils.DtoHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MissionStepTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private String sessionId;

    @BeforeEach
    void setUp() {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", 1L);

        this.sessionId = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/login")
                .then()
                .extract()
                .sessionId();
    }

    @Test
    void 시간_관리_API_권한_없음_예외_테스트() {
        Map<String, String> params = new HashMap<>();
        params.put("startAt", "23:00");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(params)
                .sessionId(sessionId)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(403)
                .extract()
                .header("Location");
    }

    @Test
    void 예약과_시간_연결() {
        LocalDate now = LocalDate.now();
        Map<String, Object> reservation = DtoHelper.getReservationRequest(now);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(reservation)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .when().get("/reservations/mine")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(5));
    }

    @Autowired
    private ReservationController reservationController;

    @Test
    void 계층화_리팩터링() {
        boolean isJdbcTemplateInjected = false;

        for (Field field : reservationController.getClass().getDeclaredFields()) {
            if (field.getType().equals(JdbcTemplate.class)) {
                isJdbcTemplateInjected = true;
                break;
            }
        }
        assertThat(isJdbcTemplateInjected).isFalse();
    }

}
