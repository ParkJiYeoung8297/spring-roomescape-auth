package roomescape.controller;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import roomescape.utils.LoginHelper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class TimeControllerTest {

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = LoginHelper.loginMember(1L);
    }

    @DisplayName("예약 시간 조회 API")
    @Test
    void 예약_시간_조회_API() {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .sessionId(sessionId)
                .when().get("/times")
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(13))
                .body("size()", equalTo(13))
                .body("[0].id", equalTo(1))
                .body("[0].startAt", equalTo("10:00"))
                .body("[12].id", equalTo(13))
                .body("[12].startAt", equalTo("22:00"));
    }

}
