package com.roomescape.mobile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ThemeItem> themes = new ArrayList<>();
    private final List<TimeItem> times = new ArrayList<>();

    private LinearLayout root;
    private EditText serverInput;
    private EditText memberInput;
    private String baseUrl = "http://10.0.2.2:8080";
    private String memberName = "";
    private ThemeItem selectedTheme;
    private TimeItem selectedTime;
    private String selectedDate = tomorrow();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CookieHandler.setDefault(new CookieManager());
        showLogin();
    }

    private void showLogin() {
        root = baseRoot();
        title("RoomEscape", 30);
        label("백엔드 주소");
        serverInput = input(baseUrl);
        label("사용자 ID");
        memberInput = input("1");
        button("로그인", this::login);
        note("에뮬레이터는 10.0.2.2:8080, 실제 휴대폰은 노트북 IP:8080을 입력하세요.");
        setContentView(wrap(root));
    }

    private void showHome() {
        root = baseRoot();
        horizontalHeader();
        button("인기 테마", this::loadPopularThemes);
        button("예약하기", this::loadThemesForBooking);
        button("내 예약", this::loadMyReservations);
        setContentView(wrap(root));
        loadPopularThemes();
    }

    private void login() {
        baseUrl = trimSlash(serverInput.getText().toString());
        String memberId = memberInput.getText().toString().trim();
        if (memberId.isEmpty()) {
            toast("사용자 ID를 입력하세요.");
            return;
        }
        memberName = memberNameOf(memberId);

        request("POST", "/login", "{\"memberId\":" + memberId + "}", response -> {
            toast(memberName + "님, 환영합니다.");
            showHome();
        });
    }

    private void logout() {
        request("POST", "/logout", null, response -> {
            memberName = "";
            selectedTheme = null;
            selectedTime = null;
            showLogin();
        });
    }

    private void loadPopularThemes() {
        section("인기 테마");
        request("GET", "/themes/popular", null, response -> {
            renderThemes(response, false);
        });
    }

    private void loadThemesForBooking() {
        section("테마 선택");
        request("GET", "/themes", null, response -> {
            renderThemes(response, true);
        });
    }

    private void renderThemes(String response, boolean selectable) throws Exception {
        themes.clear();
        JSONArray array = new JSONArray(response);
        runOnUiThread(() -> {
            section(selectable ? "테마 선택" : "인기 테마");
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    ThemeItem theme = new ThemeItem(
                            object.getLong("id"),
                            object.getString("name"),
                            object.optString("description")
                    );
                    themes.add(theme);
                    card(theme.name, theme.description, selectable ? () -> selectTheme(theme) : null);
                } catch (Exception e) {
                    toast("테마를 표시할 수 없습니다.");
                }
            }
        });
    }

    private void selectTheme(ThemeItem theme) {
        selectedTheme = theme;
        selectedTime = null;
        section("날짜 선택");
        card("선택한 테마", theme.name, null);
        button("날짜: " + selectedDate, this::pickDate);
        button("시간 조회", this::loadTimes);
    }

    private void pickDate() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            loadTimes();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadTimes() {
        if (selectedTheme == null) {
            toast("테마를 먼저 선택하세요.");
            return;
        }
        request("GET", "/themes/" + selectedTheme.id + "/available-times?date=" + selectedDate, null, response -> {
            times.clear();
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("시간 선택");
                card(selectedTheme.name, selectedDate, null);
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        TimeItem time = new TimeItem(
                                object.getLong("id"),
                                object.getString("startAt"),
                                object.getBoolean("isAvailable")
                        );
                        times.add(time);
                        Button button = button(time.startAt + (time.available ? "" : " 마감"), () -> selectTime(time));
                        button.setEnabled(time.available);
                    } catch (Exception e) {
                        toast("시간을 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void selectTime(TimeItem time) {
        selectedTime = time;
        section("예약 확인");
        card("테마", selectedTheme.name, null);
        card("날짜", selectedDate, null);
        card("시간", selectedTime.startAt, null);
        card("예약자", memberName, null);
        button("예약 확정", this::createReservation);
    }

    private void createReservation() {
        if (selectedTheme == null || selectedTime == null) {
            toast("테마와 시간을 선택하세요.");
            return;
        }
        String body = "{"
                + "\"name\":\"" + escape(memberName) + "\","
                + "\"date\":\"" + selectedDate + "\","
                + "\"timeId\":" + selectedTime.id + ","
                + "\"themeId\":" + selectedTheme.id
                + "}";
        request("POST", "/reservations", body, response -> {
            toast("예약이 완료되었습니다.");
            loadMyReservations();
        });
    }

    private void loadMyReservations() {
        section("내 예약");
        request("GET", "/reservations/mine", null, response -> {
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("내 예약");
                if (array.length() == 0) {
                    note("예약 내역이 없습니다.");
                    return;
                }
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        card(
                                object.getString("themeName"),
                                object.getString("date") + " " + object.getString("time"),
                                null
                        );
                    } catch (Exception e) {
                        toast("예약을 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void request(String method, String path, String body, ResponseHandler handler) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "application/json");

                if (body != null) {
                    connection.setDoOutput(true);
                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(body.getBytes());
                    }
                }

                int code = connection.getResponseCode();
                String response = read(connection, code);
                if (code >= 200 && code < 300) {
                    handler.handle(response);
                    return;
                }
                runOnUiThread(() -> toast("요청 실패: " + code));
            } catch (Exception e) {
                runOnUiThread(() -> toast("서버에 연결할 수 없습니다."));
            }
        });
    }

    private String read(HttpURLConnection connection, int code) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 400 ? connection.getErrorStream() : connection.getInputStream()
        ));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    private LinearLayout baseRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 44, 36, 44);
        layout.setBackgroundColor(Color.rgb(10, 13, 26));
        return layout;
    }

    private ScrollView wrap(View child) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(child);
        return scrollView;
    }

    private void horizontalHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 24);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = text(memberName, 20, true);
        header.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button logout = smallButton("로그아웃", this::logout);
        header.addView(logout);
        root.addView(header);
    }

    private void section(String title) {
        root.removeAllViews();
        horizontalHeader();
        title(title, 24);
    }

    private void title(String value, int size) {
        TextView view = text(value, size, true);
        view.setPadding(0, 0, 0, 18);
        root.addView(view);
    }

    private void label(String value) {
        TextView view = text(value, 13, false);
        view.setPadding(0, 10, 0, 8);
        root.addView(view);
    }

    private void note(String value) {
        TextView view = text(value, 13, false);
        view.setPadding(0, 18, 0, 0);
        root.addView(view);
    }

    private EditText input(String value) {
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setSingleLine(true);
        editText.setTextColor(Color.WHITE);
        editText.setHintTextColor(Color.GRAY);
        editText.setBackgroundColor(Color.rgb(26, 34, 54));
        editText.setPadding(18, 12, 18, 12);
        root.addView(editText, fullWidth());
        return editText;
    }

    private Button button(String text, Runnable action) {
        Button button = smallButton(text, action);
        root.addView(button, fullWidth());
        return button;
    }

    private Button smallButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void card(String title, String detail, Runnable action) {
        TextView view = text(title + "\n" + detail, 15, false);
        view.setPadding(24, 20, 24, 20);
        view.setBackgroundColor(Color.rgb(26, 34, 54));
        if (action != null) {
            view.setOnClickListener(v -> action.run());
        }
        root.addView(view, fullWidth());
    }

    private TextView text(String value, int size, boolean accent) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(accent ? Color.rgb(245, 158, 11) : Color.rgb(243, 244, 246));
        return view;
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        return params;
    }

    private String trimSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private String memberNameOf(String id) {
        switch (id) {
            case "1": return "김철수";
            case "2": return "이영희";
            case "3": return "박민준";
            case "4": return "최수진";
            case "5": return "정다은";
            case "6": return "강현수";
            case "7": return "윤지원";
            case "8": return "임서준";
            case "9": return "한지아";
            default: return "사용자 " + id;
        }
    }

    private String tomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        return String.format(
                "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private interface ResponseHandler {
        void handle(String response) throws Exception;
    }

    private static class ThemeItem {
        final long id;
        final String name;
        final String description;

        ThemeItem(long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
    }

    private static class TimeItem {
        final long id;
        final String startAt;
        final boolean available;

        TimeItem(long id, String startAt, boolean available) {
            this.id = id;
            this.startAt = startAt;
            this.available = available;
        }
    }
}
