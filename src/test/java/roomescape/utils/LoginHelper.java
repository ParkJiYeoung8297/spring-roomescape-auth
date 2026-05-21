package roomescape.utils;

import java.util.HashMap;
import java.util.Map;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class LoginHelper {

    public static String loginMember(Long memberId) {

        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(params)
                .when()
                .post("/login")
                .then()
                .extract()
                .sessionId();
    }
}