package roomescape.utils;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class DtoHelper {
    private DtoHelper() {}


    public static Map<String, Object> getReservationRequest(LocalDate date) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "브라운");
        params.put("date", date.plusDays(1));
        params.put("timeId", 1);
        params.put("themeId", 1);
        params.put("storeId", 1);
        return params;
    }
}
