package sample.readinglog;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/*")
public class LibraryServlet extends HttpServlet {
    private static final String SCHEMA = "book_test_user";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        try {
            if ("/books".equals(path)) {
                sendJson(response, books());
            } else if ("/records".equals(path)) {
                sendJson(response, records());
            } else {
                sendError(response, 404, "Not found");
            }
        } catch (SQLException e) {
            sendError(response, 500, "Database error");
            getServletContext().log("Database read failed", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        try {
            if ("/books".equals(path)) {
                createBook(request);
            } else if ("/records".equals(path)) {
                createRecord(request);
            } else {
                sendError(response, 404, "Not found");
                return;
            }
            sendJson(response, "{\"ok\":true}");
        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());
        } catch (SQLException e) {
            sendError(response, 500, "Database error");
            getServletContext().log("Database write failed", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/records/[0-9]+")) {
            sendError(response, 400, "Invalid record id");
            return;
        }
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM " + SCHEMA + ".reading_records WHERE id = ?")) {
            statement.setLong(1, Long.parseLong(path.substring("/records/".length())));
            if (statement.executeUpdate() == 0) {
                sendError(response, 404, "Record not found");
                return;
            }
            sendJson(response, "{\"ok\":true}");
        } catch (SQLException e) {
            sendError(response, 500, "Database error");
            getServletContext().log("Database delete failed", e);
        }
    }

    private List<Book> books() throws SQLException {
        List<Book> result = new ArrayList<>();
        String sql = "SELECT isbn, title, volume, author, translator, page_count, reading_status "
                + "FROM " + SCHEMA + ".books ORDER BY title, volume NULLS FIRST";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Book(rs.getString("isbn"), rs.getString("title"),
                        (Integer) rs.getObject("volume"), rs.getString("author"),
                        rs.getString("translator"), rs.getInt("page_count"),
                        rs.getString("reading_status")));
            }
        }
        return result;
    }

    private List<ReadingRecord> records() throws SQLException {
        List<ReadingRecord> result = new ArrayList<>();
        String sql = "SELECT r.id, r.reading_date, r.isbn, b.title, r.pages_read, r.comment, r.completed "
                + "FROM " + SCHEMA + ".reading_records r JOIN " + SCHEMA + ".books b ON b.isbn = r.isbn "
                + "ORDER BY r.reading_date DESC, r.id DESC";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new ReadingRecord(rs.getLong("id"), rs.getDate("reading_date").toString(),
                        rs.getString("isbn"), rs.getString("title"), rs.getInt("pages_read"),
                        rs.getString("comment"), rs.getBoolean("completed")));
            }
        }
        return result;
    }

    private void createBook(HttpServletRequest request) throws SQLException {
        String isbn = required(request, "isbn");
        String title = required(request, "title");
        String author = required(request, "author");
        int pages = positiveInt(request, "pageCount");
        String volume = optional(request, "volume");
        String translator = optional(request, "translator");
        String status = optional(request, "readingStatus");
        if (status == null) status = "00";
        if (!List.of("00", "01", "03", "09").contains(status)) throw new IllegalArgumentException("Invalid reading status");
        String sql = "INSERT INTO " + SCHEMA + ".books (isbn, title, volume, author, translator, page_count, reading_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn); statement.setString(2, title);
            if (volume == null) statement.setNull(3, java.sql.Types.INTEGER); else statement.setInt(3, Integer.parseInt(volume));
            statement.setString(4, author); if (translator == null) statement.setNull(5, java.sql.Types.VARCHAR); else statement.setString(5, translator);
            statement.setInt(6, pages); statement.setString(7, status); statement.executeUpdate();
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Volume must be a number"); }
    }

    private void createRecord(HttpServletRequest request) throws SQLException {
        String isbn = required(request, "isbn");
        String date = required(request, "readingDate");
        int pages = nonNegativeInt(request, "pagesRead");
        String comment = optional(request, "comment");
        boolean completed = "true".equals(request.getParameter("completed"));
        String sql = "INSERT INTO " + SCHEMA + ".reading_records (reading_date, isbn, pages_read, comment, completed) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date)); statement.setString(2, isbn); statement.setInt(3, pages);
            if (comment == null) statement.setNull(4, java.sql.Types.VARCHAR); else statement.setString(4, comment);
            statement.setBoolean(5, completed); statement.executeUpdate();
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Reading date must be YYYY-MM-DD"); }
    }

    private static String required(HttpServletRequest request, String name) {
        String value = optional(request, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String optional(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int positiveInt(HttpServletRequest request, String name) {
        int value = nonNegativeInt(request, name);
        if (value == 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }

    private static int nonNegativeInt(HttpServletRequest request, String name) {
        try { int value = Integer.parseInt(required(request, name)); if (value < 0) throw new NumberFormatException(); return value; }
        catch (NumberFormatException e) { throw new IllegalArgumentException(name + " must be a non-negative number"); }
    }

    private static void sendJson(HttpServletResponse response, Object value) throws IOException {
        response.setCharacterEncoding("UTF-8"); response.setContentType("application/json");
        response.getWriter().write(Json.encode(value));
    }

    private static void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); sendJson(response, "{\"error\":\"" + Json.escape(message) + "\"}");
    }

    private record Book(String isbn, String title, Integer volume, String author, String translator, int pageCount, String readingStatus) {}
    private record ReadingRecord(long id, String readingDate, String isbn, String title, int pagesRead, String comment, boolean completed) {}

    private static final class Json {
        static String encode(Object value) {
            if (value instanceof String s) return s.startsWith("{") || s.startsWith("[") ? s : "\"" + escape(s) + "\"";
            if (value instanceof List<?> list) return list.stream().map(Json::encode).reduce("[", (a, b) -> a.equals("[") ? a + b : a + "," + b) + "]";
            if (value instanceof Book b) return "{\"isbn\":\"" + escape(b.isbn) + "\",\"title\":\"" + escape(b.title) + "\",\"volume\":" + nullable(b.volume) + ",\"author\":\"" + escape(b.author) + "\",\"translator\":" + nullable(b.translator) + ",\"pageCount\":" + b.pageCount + ",\"readingStatus\":\"" + escape(b.readingStatus) + "\"}";
            ReadingRecord r = (ReadingRecord) value;
            return "{\"id\":" + r.id + ",\"readingDate\":\"" + escape(r.readingDate) + "\",\"isbn\":\"" + escape(r.isbn) + "\",\"title\":\"" + escape(r.title) + "\",\"pagesRead\":" + r.pagesRead + ",\"comment\":" + nullable(r.comment) + ",\"completed\":" + r.completed + "}";
        }
        static String nullable(Object value) { return value == null ? "null" : "\"" + escape(String.valueOf(value)) + "\""; }
        static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
    }
}
