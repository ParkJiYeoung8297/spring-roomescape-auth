package roomescape.controller;

import static org.hamcrest.core.Is.is;

import java.util.HashMap;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import roomescape.utils.LoginHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminReservationControllerTest {

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = LoginHelper.loginMember(10L);
    }

    @DisplayName("본인 매장의 예약 내역이 모두 조회되어야한다.")
    @Test
    void 관리자_예약_조회_API() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .when().get("/admin/reservations")
                .then().log().all()
                           .statusCode(200);
    }


    @DisplayName("다른 관리자의 예약은 조회되지 않는다")
    @Test
    void 다른_관리자의_예약은_조회되지_않는다() {
        String anotherOwnerSession = LoginHelper.loginMember(11L);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(anotherOwnerSession)

                .when()
                .get("/admin/reservations")

                .then().log().all()
                .statusCode(200);
    }

    @DisplayName("본인 매장의 예약은 수정할 수 있다.")
    @Test
    void 관리자_예약_수정_API() {

        sessionId = LoginHelper.loginMember(10L);

        Map<String, Object> params = new HashMap<>();
        params.put("targetDate", "2026-05-01");
        params.put("timeId", 2L);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .pathParam("id", 1L)

                .when()
                .patch("/admin/reservations/{id}")

                .then().log().all()
                .statusCode(204);
    }

    @DisplayName("다른 매장의 예약은 수정할 수 없다.")
    @Test
    void 관리자_예약_수정_API_예외() {

        sessionId = LoginHelper.loginMember(11L);

        Map<String, Object> params = new HashMap<>();
        params.put("targetDate", "2026-05-01");
        params.put("timeId", 2L);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .pathParam("id", 1L)

                .when()
                .patch("/admin/reservations/{id}")

                .then().log().all()
                .statusCode(403);
    }

    @DisplayName("본인 매장의 예약은 삭제할 수 있다.")
    @Test
    void 관리자_예약_삭제_API() {
        sessionId = LoginHelper.loginMember(10L);
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .pathParam("id", 1L)
                .when()
                .delete("/admin/reservations/{id}")
                .then().log().all()
                .statusCode(204);
    }

    @DisplayName("다른 매장의 예약은 삭제할 수 없다.")
    @Test
    void 관리자_예약_삭제_API_예외() {
        sessionId = LoginHelper.loginMember(11L);
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .pathParam("id", 1L)

                .when()
                .delete("/admin/reservations/{id}")

                .then().log().all()
                .statusCode(403);
    }

}
