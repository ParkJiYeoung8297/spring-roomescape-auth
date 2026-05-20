package com.roomescape.mobile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int BG = Color.rgb(10, 13, 26);
    private static final int SURFACE = Color.rgb(17, 24, 39);
    private static final int CARD = Color.rgb(26, 34, 54);
    private static final int CARD_HOVER = Color.rgb(31, 42, 64);
    private static final int ACCENT = Color.rgb(245, 158, 11);
    private static final int TEXT = Color.rgb(243, 244, 246);
    private static final int MUTED = Color.rgb(156, 163, 175);
    private static final int BORDER = Color.rgb(31, 41, 55);
    private static final int DANGER = Color.rgb(239, 68, 68);

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
    private Long editingReservationId;
    private boolean adminMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CookieHandler.setDefault(new CookieManager());
        showLogin();
    }

    private void showLogin() {
        root = baseRoot();
        root.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("MYSTERY EXPERIENCE", 11, true);
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(0.18f);
        root.addView(logo, fullWidth());
        title("RoomEscape", 34);
        note("사용자 ID를 입력하고 방탈출을 시작하세요.");
        label("백엔드 주소");
        serverInput = input(baseUrl);
        label("사용자 ID");
        memberInput = input("1");
        primaryButton("입장하기", this::login);
        secondaryButton("관리자 모드", this::showAdminLogin);
        note("에뮬레이터는 10.0.2.2:8080, 실제 휴대폰은 노트북 IP:8080을 입력하세요.");
        setContentView(wrap(root));
    }

    private void showAdminLogin() {
        root = baseRoot();
        root.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("ADMIN CONSOLE", 11, true);
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(0.18f);
        root.addView(logo, fullWidth());
        title("관리자 로그인", 32);
        note("관리자 화면은 사용자 화면과 분리해서 진입합니다.");
        label("백엔드 주소");
        serverInput = input(baseUrl);
        label("관리자 ID");
        memberInput = input("1");
        primaryButton("관리자 입장", this::adminLogin);
        secondaryButton("사용자 로그인으로", this::showLogin);
        setContentView(wrap(root));
    }

    private void showHome() {
        root = baseRoot();
        setContentView(wrap(root));
        loadPopularThemes();
    }

    private void login() {
        adminMode = false;
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

    private void adminLogin() {
        adminMode = true;
        baseUrl = trimSlash(serverInput.getText().toString());
        String memberId = memberInput.getText().toString().trim();
        if (memberId.isEmpty()) {
            toast("관리자 ID를 입력하세요.");
            return;
        }
        memberName = "관리자";

        request("POST", "/login", "{\"memberId\":" + memberId + "}", response -> {
            toast("관리자 모드로 입장했습니다.");
            showAdminHome();
        });
    }

    private void logout() {
        request("POST", "/logout", null, response -> {
            adminMode = false;
            memberName = "";
            selectedTheme = null;
            selectedTime = null;
            showLogin();
        });
    }

    private void loadPopularThemes() {
        section("인기 테마");
        note("최근 7일간 가장 많이 예약된 테마를 확인하세요.");
        request("GET", "/themes/popular", null, response -> {
            renderThemes(response, false);
        });
    }

    private void loadThemesForBooking() {
        editingReservationId = null;
        section("테마 선택");
        note("테마를 선택하고 원하는 날짜와 시간을 예약하세요.");
        request("GET", "/themes", null, response -> {
            renderThemes(response, true);
        });
    }

    private void renderThemes(String response, boolean selectable) throws Exception {
        themes.clear();
        JSONArray array = new JSONArray(response);
        runOnUiThread(() -> {
            section(selectable ? "테마 선택" : "인기 테마");
            note(selectable ? "예약할 테마를 선택하세요." : "최근 7일 기준 인기 테마입니다.");
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    ThemeItem theme = new ThemeItem(
                            object.getLong("id"),
                            object.getString("name"),
                            object.optString("description")
                    );
                    themes.add(theme);
                    card((selectable ? "" : "#" + (i + 1) + " ") + theme.name, theme.description, selectable ? () -> selectTheme(theme) : null);
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
        stepBadge("1 테마 선택  >  2 날짜 선택");
        card("선택한 테마", theme.name, null);
        secondaryButton("날짜: " + selectedDate, this::pickDate);
        primaryButton("시간 조회", this::loadTimes);
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
                stepBadge("2 날짜 선택  >  3 시간 선택");
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
                        Button button = secondaryButton(time.startAt + (time.available ? "" : " 마감"), () -> selectTime(time));
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
        section(editingReservationId == null ? "예약 확인" : "예약 수정");
        stepBadge("3 시간 선택  >  4 확인");
        card("테마", selectedTheme.name, null);
        card("날짜", selectedDate, null);
        card("시간", selectedTime.startAt, null);
        card("예약자", memberName, null);
        primaryButton(editingReservationId == null ? "예약 확정" : "수정 완료", this::submitReservation);
    }

    private void submitReservation() {
        if (selectedTheme == null || selectedTime == null) {
            toast("테마와 시간을 선택하세요.");
            return;
        }
        if (editingReservationId != null) {
            updateReservation();
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

    private void updateReservation() {
        String body = "{"
                + "\"targetDate\":\"" + selectedDate + "\","
                + "\"timeId\":" + selectedTime.id
                + "}";
        request("PATCH", "/reservations/" + editingReservationId, body, response -> {
            toast("예약이 수정되었습니다.");
            editingReservationId = null;
            loadMyReservations();
        });
    }

    private void loadMyReservations() {
        editingReservationId = null;
        section("내 예약");
        request("GET", "/reservations/mine", null, response -> {
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("내 예약");
                if (array.length() == 0) {
                    empty("예약 내역이 없습니다.");
                    return;
                }
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        ReservationItem reservation = new ReservationItem(
                                object.getLong("id"),
                                object.getString("date"),
                                object.getString("themeName"),
                                object.optString("themeDescription"),
                                object.getString("time")
                        );
                        reservationCard(reservation, false);
                    } catch (Exception e) {
                        toast("예약을 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void reservationCard(ReservationItem reservation, boolean admin) {
        LinearLayout box = cardBox();
        TextView main = text(reservation.themeName, 17, true);
        main.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(main, fullWidth());
        String detail = reservation.date + " " + reservation.time;
        if (admin && !reservation.name.isEmpty()) {
            detail = reservation.name + "\n" + detail;
        }
        TextView sub = text(detail, 14, false);
        sub.setTextColor(MUTED);
        box.addView(sub, fullWidth());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (!admin) {
            Button edit = smallButton("수정", () -> startReservationEdit(reservation));
            styleButton(edit, false);
            actions.addView(edit, weighted());
        }
        Button delete = smallButton("삭제", () -> deleteReservation(reservation.id, admin));
        styleDangerButton(delete);
        actions.addView(delete, weighted());
        box.addView(actions, fullWidth());
        root.addView(box, fullWidth());
    }

    private void deleteReservation(long reservationId, boolean admin) {
        request("DELETE", "/reservations/" + reservationId, null, response -> {
            toast("예약이 삭제되었습니다.");
            if (admin) {
                loadAdminReservations();
                return;
            }
            loadMyReservations();
        });
    }

    private void startReservationEdit(ReservationItem reservation) {
        editingReservationId = reservation.id;
        selectedDate = reservation.date;
        selectedTime = null;
        request("GET", "/themes", null, response -> {
            JSONArray array = new JSONArray(response);
            ThemeItem matched = null;
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                ThemeItem theme = new ThemeItem(
                        object.getLong("id"),
                        object.getString("name"),
                        object.optString("description")
                );
                if (theme.name.equals(reservation.themeName)) {
                    matched = theme;
                }
            }
            if (matched == null) {
                toast("수정할 테마를 찾을 수 없습니다.");
                return;
            }
            selectedTheme = matched;
            section("예약 수정");
            stepBadge("1 날짜 선택  >  2 시간 선택");
            card("예약", selectedTheme.name + "\n현재 " + reservation.date + " " + reservation.time, null);
            secondaryButton("날짜: " + selectedDate, this::pickDate);
            primaryButton("시간 조회", this::loadTimes);
        });
    }

    private void showAdminHome() {
        section("관리자");
        note("예약, 테마, 시간을 모바일에서 관리합니다.");
        primaryButton("예약 관리", this::loadAdminReservations);
        secondaryButton("테마 관리", this::loadAdminThemes);
        secondaryButton("시간 관리", this::loadAdminTimes);
    }

    private void loadAdminReservations() {
        section("관리자 예약");
        request("GET", "/admin/reservations", null, response -> {
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("관리자 예약");
                note("전체 예약을 확인하고 삭제할 수 있습니다.");
                if (array.length() == 0) {
                    empty("예약 내역이 없습니다.");
                    return;
                }
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        ReservationItem reservation = new ReservationItem(
                                object.getLong("id"),
                                object.getString("date"),
                                object.getString("themeName"),
                                "",
                                object.getString("time")
                        );
                        reservation.name = object.optString("name");
                        reservationCard(reservation, true);
                    } catch (Exception e) {
                        toast("예약을 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void loadAdminThemes() {
        section("관리자 테마");
        request("GET", "/themes", null, response -> {
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("관리자 테마");
                note("테마를 추가하거나 삭제합니다.");
                label("테마 이름");
                EditText name = input("");
                label("설명");
                EditText description = input("");
                label("썸네일 URL");
                EditText thumbnail = input("");
                primaryButton("테마 추가", () -> createAdminTheme(name, description, thumbnail));
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        ThemeItem theme = new ThemeItem(
                                object.getLong("id"),
                                object.getString("name"),
                                object.optString("description")
                        );
                        adminThemeCard(theme);
                    } catch (Exception e) {
                        toast("테마를 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void createAdminTheme(EditText name, EditText description, EditText thumbnail) {
        String themeName = name.getText().toString().trim();
        if (themeName.isEmpty()) {
            toast("테마 이름을 입력하세요.");
            return;
        }
        String body = "{"
                + "\"name\":\"" + escape(themeName) + "\","
                + "\"description\":\"" + escape(description.getText().toString().trim()) + "\","
                + "\"thumbnailUrl\":\"" + escape(thumbnail.getText().toString().trim()) + "\""
                + "}";
        request("POST", "/admin/themes", body, response -> {
            toast("테마가 추가되었습니다.");
            loadAdminThemes();
        });
    }

    private void adminThemeCard(ThemeItem theme) {
        LinearLayout box = cardBox();
        TextView main = text(theme.name, 17, true);
        main.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(main, fullWidth());
        TextView sub = text(theme.description, 14, false);
        sub.setTextColor(MUTED);
        box.addView(sub, fullWidth());
        Button delete = smallButton("삭제", () -> deleteAdminTheme(theme.id));
        styleDangerButton(delete);
        box.addView(delete, fullWidth());
        root.addView(box, fullWidth());
    }

    private void deleteAdminTheme(long themeId) {
        request("DELETE", "/admin/themes/" + themeId, null, response -> {
            toast("테마가 삭제되었습니다.");
            loadAdminThemes();
        });
    }

    private void loadAdminTimes() {
        section("관리자 시간");
        request("GET", "/admin/times", null, response -> {
            JSONArray array = new JSONArray(response);
            runOnUiThread(() -> {
                section("관리자 시간");
                note("예약 가능한 시간을 추가하거나 삭제합니다.");
                label("시간");
                EditText startAt = input("10:00");
                primaryButton("시간 추가", () -> createAdminTime(startAt));
                if (array.length() == 0) {
                    empty("등록된 시간이 없습니다.");
                    return;
                }
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject object = array.getJSONObject(i);
                        TimeItem time = new TimeItem(
                                object.getLong("id"),
                                object.getString("startAt"),
                                true
                        );
                        adminTimeCard(time);
                    } catch (Exception e) {
                        toast("시간을 표시할 수 없습니다.");
                    }
                }
            });
        });
    }

    private void createAdminTime(EditText startAt) {
        String value = startAt.getText().toString().trim();
        if (value.isEmpty()) {
            toast("시간을 입력하세요.");
            return;
        }
        String body = "{\"startAt\":\"" + escape(value) + "\"}";
        request("POST", "/admin/times", body, response -> {
            toast("시간이 추가되었습니다.");
            loadAdminTimes();
        });
    }

    private void adminTimeCard(TimeItem time) {
        LinearLayout box = cardBox();
        TextView main = text(time.startAt, 18, true);
        main.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(main, fullWidth());
        Button delete = smallButton("삭제", () -> deleteAdminTime(time.id));
        styleDangerButton(delete);
        box.addView(delete, fullWidth());
        root.addView(box, fullWidth());
    }

    private void deleteAdminTime(long timeId) {
        request("DELETE", "/admin/times/" + timeId, null, response -> {
            toast("시간이 삭제되었습니다.");
            loadAdminTimes();
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
                    runOnUiThread(() -> {
                        try {
                            handler.handle(response);
                        } catch (Exception e) {
                            toast("응답을 처리할 수 없습니다.");
                        }
                    });
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
        layout.setPadding(dp(18), dp(28), dp(18), dp(32));
        layout.setBackgroundColor(BG);
        return layout;
    }

    private ScrollView wrap(View child) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        scrollView.addView(child);
        return scrollView;
    }

    private void horizontalHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(16));
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = text((adminMode ? "Admin Console" : "RoomEscape") + "\n" + memberName + " 님", 18, true);
        header.addView(name, weighted());
        Button logout = smallButton("나가기", this::logout);
        styleButton(logout, false);
        header.addView(logout);
        root.addView(header);
    }

    private void section(String title) {
        root.removeAllViews();
        horizontalHeader();
        if (adminMode) {
            adminNavigation();
        } else {
            navigation();
        }
        title(title, 24);
    }

    private void navigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, 0, 0, dp(16));
        nav.addView(navButton("인기", this::loadPopularThemes), weighted());
        nav.addView(navButton("예약", this::loadThemesForBooking), weighted());
        nav.addView(navButton("내 예약", this::loadMyReservations), weighted());
        root.addView(nav, fullWidth());
    }

    private void adminNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, 0, 0, dp(16));
        nav.addView(navButton("예약", this::loadAdminReservations), weighted());
        nav.addView(navButton("테마", this::loadAdminThemes), weighted());
        nav.addView(navButton("시간", this::loadAdminTimes), weighted());
        root.addView(nav, fullWidth());
    }

    private void title(String value, int size) {
        TextView view = text(value, size, true);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, 0, 0, dp(10));
        root.addView(view);
    }

    private void label(String value) {
        TextView view = text(value, 13, false);
        view.setTextColor(MUTED);
        view.setPadding(0, dp(14), 0, dp(7));
        root.addView(view);
    }

    private void note(String value) {
        TextView view = text(value, 13, false);
        view.setTextColor(MUTED);
        view.setPadding(0, dp(4), 0, dp(14));
        root.addView(view);
    }

    private void stepBadge(String value) {
        TextView view = text(value, 12, false);
        view.setTextColor(ACCENT);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackground(rounded(Color.rgb(32, 26, 14), ACCENT, 999));
        root.addView(view, fullWidth());
    }

    private void empty(String value) {
        TextView view = text(value, 15, false);
        view.setGravity(Gravity.CENTER);
        view.setTextColor(MUTED);
        view.setPadding(dp(24), dp(42), dp(24), dp(42));
        view.setBackground(rounded(CARD, BORDER, 12));
        root.addView(view, fullWidth());
    }

    private EditText input(String value) {
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setSingleLine(true);
        editText.setTextColor(TEXT);
        editText.setTextSize(16);
        editText.setHintTextColor(MUTED);
        editText.setBackground(rounded(Color.rgb(18, 24, 40), BORDER, 9));
        editText.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(editText, fullWidth());
        return editText;
    }

    private Button primaryButton(String text, Runnable action) {
        Button button = smallButton(text, action);
        styleButton(button, true);
        root.addView(button, fullWidth());
        return button;
    }

    private Button secondaryButton(String text, Runnable action) {
        Button button = smallButton(text, action);
        styleButton(button, false);
        root.addView(button, fullWidth());
        return button;
    }

    private Button navButton(String text, Runnable action) {
        Button button = smallButton(text, action);
        button.setTextSize(13);
        button.setTextColor(TEXT);
        button.setBackground(rounded(CARD, BORDER, 10));
        return button;
    }

    private Button smallButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void styleButton(Button button, boolean primary) {
        button.setTextSize(15);
        button.setTextColor(primary ? BG : TEXT);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setMinHeight(dp(48));
        button.setBackground(rounded(primary ? ACCENT : CARD, primary ? ACCENT : BORDER, 9));
    }

    private void styleDangerButton(Button button) {
        button.setTextSize(15);
        button.setTextColor(TEXT);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setMinHeight(dp(48));
        button.setBackground(rounded(Color.rgb(45, 18, 24), DANGER, 9));
    }

    private LinearLayout cardBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(rounded(CARD, BORDER, 10));
        return box;
    }

    private void card(String title, String detail, Runnable action) {
        TextView view = text(title + "\n" + detail, 15, false);
        view.setLineSpacing(dp(3), 1.0f);
        view.setPadding(dp(18), dp(16), dp(18), dp(16));
        view.setBackground(rounded(action == null ? CARD : CARD_HOVER, action == null ? BORDER : ACCENT, 10));
        if (action != null) {
            view.setOnClickListener(v -> action.run());
        }
        root.addView(view, fullWidth());
    }

    private TextView text(String value, int size, boolean accent) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(accent ? ACCENT : TEXT);
        return view;
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(7), 0, dp(7));
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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

    private static class ReservationItem {
        final long id;
        final String date;
        final String themeName;
        final String themeDescription;
        final String time;
        String name = "";

        ReservationItem(long id, String date, String themeName, String themeDescription, String time) {
            this.id = id;
            this.date = date;
            this.themeName = themeName;
            this.themeDescription = themeDescription;
            this.time = time;
        }
    }
}
