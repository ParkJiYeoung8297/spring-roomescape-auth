package roomescape.controller;

import static org.hamcrest.core.Is.is;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationControllerTest {
    @Autowired
    private ReservationController reservationController;
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

    @DisplayName("사용자 예약 추가 API")
    @Test
    void 사용자_예약_추가_API() {
        LocalDate date = LocalDate.now();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", date.plusDays(1));
        params.put("timeId", 1);
        params.put("themeId", 1);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201);
    }

    @DisplayName("사용자 예약 추가 API - 이상값 예외 테스트")
    @Test
    void API_사용자_예약_추가_예외_테스트() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", "2025");
        params.put("timeId", 1);
        params.put("themeId", 1);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @DisplayName("사용자 예약 삭제 API")
    @Test
    void 사용자_예약_삭제_API() {
        LocalDate date = LocalDate.now();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", date.plusDays(1));
        params.put("timeId", 1);
        params.put("themeId", 1);

        final long id = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .pathParam("id", id)
                .when().delete("/reservations/{id}")
                .then().log().all()
                .statusCode(204);
    }

    @DisplayName("사용자 예약 추가 - 날짜 형식 예외 테스트")
    @Test
    void 사용자_예약_추가_날짜_형식_예외_테스트() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", "2024-95-05");
        params.put("timeId", 1);
        params.put("themeId", 1);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @DisplayName("사용자 예약 추가 - 이름 형식 예외 테스트")
    @Test
    void 사용자_예약_추가_이름_형식_예외_테스트() {
        String longName = "a".repeat(256);

        Map<String, Object> params = new HashMap<>();
        params.put("name", longName);
        params.put("date", "2024-95-05");
        params.put("timeId", 1);
        params.put("themeId", 1);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    @DisplayName("나의 예약 조회 API")
    @Test
    void 나의_예약_조회_API() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .queryParam("userId", "1")
                .when().get("/reservations/mine")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    void 로그인하지_않으면_예약할_수_없다() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", LocalDate.now().plusDays(1));
        params.put("timeId", 1);
        params.put("themeId", 1);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when().post("/reservations")
                .then()
                .statusCode(401);
    }

}
