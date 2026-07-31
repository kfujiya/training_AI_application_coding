package com.example.readinglog;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/app")
public class ReadingLogServlet extends HttpServlet {
    private final ReadingLogRepository repository = new ReadingLogRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            request.setAttribute("books", repository.findBooks());
            request.setAttribute("readingRecords", repository.findReadingRecords());
            request.getRequestDispatcher("/WEB-INF/views/index.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("データの取得に失敗しました。", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String action = request.getParameter("action");
        String tab = "records";
        try {
            if ("createBook".equals(action)) {
                tab = "books";
                Integer volume = optionalInteger(request.getParameter("volume"));
                repository.createBook(new ReadingLogRepository.Book(
                        required(request, "isbn"), required(request, "title"), volume,
                        required(request, "author"), request.getParameter("translator"),
                        positiveInteger(request, "pageCount"), required(request, "readingStatus")));
            } else if ("createRecord".equals(action)) {
                repository.createReadingRecord(
                        LocalDate.parse(required(request, "readingDate")), required(request, "isbn"),
                        nonNegativeInteger(request, "pagesRead"), request.getParameter("comment"),
                        "true".equals(request.getParameter("completed")));
            } else if ("deleteBook".equals(action)) {
                tab = "books";
                repository.deleteBook(required(request, "isbn"));
            } else if ("deleteRecord".equals(action)) {
                repository.deleteReadingRecord(Long.parseLong(required(request, "recordId")));
            } else {
                throw new IllegalArgumentException("不明な操作です。");
            }
            redirect(response, request.getContextPath(), tab, "保存しました。", false);
        } catch (IllegalArgumentException e) {
            redirect(response, request.getContextPath(), tab, e.getMessage(), true);
        } catch (SQLException e) {
            String message = "23505".equals(e.getSQLState())
                    ? "同じキーのデータが既に登録されています。"
                    : "データベース処理に失敗しました。";
            redirect(response, request.getContextPath(), tab, message, true);
        }
    }

    private static String required(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("必須項目を入力してください。");
        return value.trim();
    }

    private static Integer optionalInteger(String value) {
        if (value == null || value.isBlank()) return null;
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) throw new IllegalArgumentException("巻は1以上で入力してください。");
        return parsed;
    }

    private static int positiveInteger(HttpServletRequest request, String name) {
        int value = Integer.parseInt(required(request, name));
        if (value <= 0) throw new IllegalArgumentException("ページ数は1以上で入力してください。");
        return value;
    }

    private static int nonNegativeInteger(HttpServletRequest request, String name) {
        int value = Integer.parseInt(required(request, name));
        if (value < 0) throw new IllegalArgumentException("読破ページ数は0以上で入力してください。");
        return value;
    }

    private static void redirect(HttpServletResponse response, String contextPath, String tab,
                                 String message, boolean error) throws IOException {
        response.sendRedirect(contextPath + "/app?tab=" + tab + "&" + (error ? "error" : "message") + "=" +
                URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
