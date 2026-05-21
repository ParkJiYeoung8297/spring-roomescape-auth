package roomescape.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import roomescape.utils.LoginHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminTimeControllerTest {
    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = LoginHelper.loginMember(10L);
    }

    @DisplayName("예약 시간 등록 API")
    @Test
    void 예약_시간_등록_API() {
        final String createStartAt = "23:00";
        final Map<String,Object> params = new HashMap<>();
        params.put("startAt", createStartAt);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(201);
    }

    @DisplayName("예약 시간 등록 API - 이상값 예외 테스트")
    @Test
    void 예약_시간_등록_API_예외_테스트() {
        final String createStartAt = "230";
        final Map<String,Object> params = new HashMap<>();
        params.put("startAt", createStartAt);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(400);
    }

    @DisplayName("예약 시간 등록 API - 관리자가 아니면 등록할 수 없다")
    @Test
    void 예약_시간_등록_API_예외() {
        sessionId = LoginHelper.loginMember(1L);
        final String createStartAt = "23:00";
        final Map<String,Object> params = new HashMap<>();
        params.put("startAt", createStartAt);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(403);
    }

    @Test
    void 시간_관리_관리자_API() {
        Map<String, Object> params = new HashMap<>();
        params.put("startAt", "23:00");

        final String location = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .body(params)
                .sessionId(sessionId)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(201)
                .extract()
                .header("Location");
        final long id = Long.parseLong(location.split("/")[2]);

        RestAssured.given().log().all()
                .sessionId(sessionId)
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", is(14));

        RestAssured.given().log().all()
                .pathParam("id", id)
                .sessionId(sessionId)
                .when().delete("/admin/times/{id}")
                .then().log().all()
                .statusCode(204);
    }

    @DisplayName("API - 예약 시간 삭제")
    @Test
    void API_예약_시간_삭제() {
        final String createStartAt = "23:00";
        final Map<String,Object> params = new HashMap<>();
        params.put("startAt", createStartAt);

        final long id = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .body(params)
                .when().post("/admin/times")
                .then().log().all()
                .statusCode(201)
                .body("startAt", equalTo(createStartAt))
                .extract()
                .jsonPath()
                .getLong("id");

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .when().delete("/admin/times/" + id)
                .then().log().all()
                .statusCode(204);

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(13));
    }



}