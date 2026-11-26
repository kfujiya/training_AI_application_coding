package jp.library.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/books")
public class LibraryStatusServlet extends HttpServlet {
    private static final String STATUS_SQL =
            "SELECT b.isbn, b.title, b.volume, b.author, "
            + "COALESCE((SELECT string_agg(DISTINCT u.full_name, '、' ORDER BY u.full_name) "
            + "FROM loan_records lr JOIN users u ON u.user_id = lr.user_id "
            + "WHERE lr.isbn = b.isbn AND lr.returned_at IS NULL), '—') AS borrowers, "
            + "(SELECT count(*) FROM reservations r "
            + "WHERE r.isbn = b.isbn AND r.loaned_at IS NULL) AS reservation_count "
            + "FROM books b ORDER BY b.title, b.isbn";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try {
            writePage(response, loadBookStatuses(), request.getParameter("message"));
        } catch (SQLException | IllegalStateException e) {
            getServletContext().log("Failed to load library book status", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeErrorPage(response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if ("loan".equals(request.getParameter("action"))) {
            handleLoanOrReservation(request, response);
            return;
        }
        if ("return".equals(request.getParameter("action"))) {
            handleReturn(request, response);
            return;
        }
        String isbn = trim(request.getParameter("isbn"));
        String title = trim(request.getParameter("title"));
        String volumeText = trim(request.getParameter("volume"));
        String author = trim(request.getParameter("author"));

        String normalizedIsbn = isbn == null ? null : isbn.replace("-", "");
        if (normalizedIsbn == null || !normalizedIsbn.matches("(?:[0-9]{9}[0-9Xx]|[0-9]{13})")
                || title == null || title.isEmpty()
                || author == null || author.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/books?message=invalid");
            return;
        }
        Integer volume = null;
        if (volumeText != null && !volumeText.isEmpty()) {
            try {
                volume = Integer.valueOf(volumeText);
                if (volume < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/books?message=invalid");
                return;
            }
        }

        String sql = "INSERT INTO books (isbn, title, volume, author) VALUES (?, ?, ?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedIsbn);
            statement.setString(2, title);
            if (volume == null) statement.setNull(3, java.sql.Types.INTEGER);
            else statement.setInt(3, volume);
            statement.setString(4, author);
            statement.executeUpdate();
            response.sendRedirect(request.getContextPath() + "/books?message=added");
        } catch (SQLException e) {
            getServletContext().log("Failed to register book", e);
            response.sendRedirect(request.getContextPath()
                    + ("23505".equals(e.getSQLState()) ? "/books?message=duplicate" : "/books?message=error"));
        }
    }

    private void handleLoanOrReservation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String isbn = trim(request.getParameter("isbn"));
        String fullName = trim(request.getParameter("fullName"));
        String normalizedIsbn = isbn == null ? null : isbn.replace("-", "");
        if (normalizedIsbn == null || !normalizedIsbn.matches("(?:[0-9]{9}[0-9Xx]|[0-9]{13})")
                || fullName == null || fullName.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/books?message=loanInvalid");
            return;
        }

        String result;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                // Serialize requests for the same book so two users cannot both borrow it.
                try (PreparedStatement book = connection.prepareStatement(
                        "SELECT isbn FROM books WHERE isbn = ? FOR UPDATE")) {
                    book.setString(1, normalizedIsbn);
                    try (ResultSet rows = book.executeQuery()) {
                        if (!rows.next()) {
                            result = "notFound";
                            connection.rollback();
                            response.sendRedirect(request.getContextPath() + "/books?message=" + result);
                            return;
                        }
                    }
                }
                Long userId = null;
                try (PreparedStatement user = connection.prepareStatement(
                        "SELECT user_id FROM users WHERE full_name = ? ORDER BY user_id LIMIT 1")) {
                    user.setString(1, fullName);
                    try (ResultSet rows = user.executeQuery()) {
                        if (rows.next()) userId = rows.getLong(1);
                    }
                }
                if (userId == null) {
                    result = "userNotFound";
                } else if (hasActiveLoan(connection, normalizedIsbn)) {
                    try (PreparedStatement reservation = connection.prepareStatement(
                            "INSERT INTO reservations (user_id, isbn, reserved_at) "
                            + "SELECT ?, ?, CURRENT_TIMESTAMP WHERE NOT EXISTS ("
                            + "SELECT 1 FROM reservations WHERE user_id = ? AND isbn = ? AND loaned_at IS NULL)")) {
                        reservation.setLong(1, userId);
                        reservation.setString(2, normalizedIsbn);
                        reservation.setLong(3, userId);
                        reservation.setString(4, normalizedIsbn);
                        result = reservation.executeUpdate() == 1 ? "reserved" : "alreadyReserved";
                    }
                } else {
                    Long latestReservationUser = null;
                    java.sql.Timestamp latestReservedAt = null;
                    try (PreparedStatement reservation = connection.prepareStatement(
                            "SELECT user_id, reserved_at FROM reservations WHERE isbn = ? AND loaned_at IS NULL "
                            + "ORDER BY reserved_at DESC LIMIT 1 FOR UPDATE")) {
                        reservation.setString(1, normalizedIsbn);
                        try (ResultSet rows = reservation.executeQuery()) {
                            if (rows.next()) {
                                latestReservationUser = rows.getLong(1);
                                latestReservedAt = rows.getTimestamp(2);
                            }
                        }
                    }
                    if (latestReservationUser != null && latestReservationUser.longValue() != userId.longValue()) {
                        result = "reservationOrder";
                    } else {
                        try (PreparedStatement loan = connection.prepareStatement(
                                "INSERT INTO loan_records (user_id, isbn, loaned_at) VALUES (?, ?, CURRENT_TIMESTAMP)")) {
                            loan.setLong(1, userId);
                            loan.setString(2, normalizedIsbn);
                            loan.executeUpdate();
                        }
                        if (latestReservationUser != null) {
                            try (PreparedStatement update = connection.prepareStatement(
                                    "UPDATE reservations SET loaned_at = CURRENT_TIMESTAMP "
                                    + "WHERE user_id = ? AND isbn = ? AND reserved_at = ? AND loaned_at IS NULL")) {
                                update.setLong(1, userId);
                                update.setString(2, normalizedIsbn);
                                update.setTimestamp(3, latestReservedAt);
                                update.executeUpdate();
                            }
                        }
                        result = "loaned";
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | IllegalStateException e) {
            getServletContext().log("Failed to process loan or reservation", e);
            result = "error";
        }
        response.sendRedirect(request.getContextPath() + "/books?message=" + result);
    }

    private boolean hasActiveLoan(Connection connection, String isbn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM loan_records WHERE isbn = ? AND returned_at IS NULL LIMIT 1")) {
            statement.setString(1, isbn);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String isbn = trim(request.getParameter("isbn"));
        String normalizedIsbn = isbn == null ? null : isbn.replace("-", "");
        if (normalizedIsbn == null || !normalizedIsbn.matches("(?:[0-9]{9}[0-9Xx]|[0-9]{13})")) {
            response.sendRedirect(request.getContextPath() + "/books?message=returnInvalid");
            return;
        }

        String result;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement book = connection.prepareStatement(
                        "SELECT isbn FROM books WHERE isbn = ? FOR UPDATE")) {
                    book.setString(1, normalizedIsbn);
                    try (ResultSet rows = book.executeQuery()) {
                        if (!rows.next()) {
                            connection.rollback();
                            response.sendRedirect(request.getContextPath() + "/books?message=notFound");
                            return;
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE loan_records SET returned_at = CURRENT_TIMESTAMP "
                        + "WHERE isbn = ? AND returned_at IS NULL")) {
                    update.setString(1, normalizedIsbn);
                    result = update.executeUpdate() > 0 ? "returned" : "notOnLoan";
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException | IllegalStateException e) {
            getServletContext().log("Failed to return book", e);
            result = "returnError";
        }
        response.sendRedirect(request.getContextPath() + "/books?message=" + result);
    }

    private List<BookStatus> loadBookStatuses() throws SQLException {
        List<BookStatus> books = new ArrayList<BookStatus>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(STATUS_SQL);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                books.add(new BookStatus(result.getString("isbn"), result.getString("title"),
                        result.getObject("volume") == null ? null : result.getInt("volume"),
                        result.getString("author"), result.getString("borrowers"),
                        result.getLong("reservation_count")));
            }
        }
        return books;
    }

    private Connection openConnection() throws SQLException {
        String url = requiredSetting("LIBRARY_DB_URL");
        String user = requiredSetting("LIBRARY_DB_USER");
        String password = requiredSetting("LIBRARY_DB_PASSWORD");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver is not available", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private String requiredSetting(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void writePage(HttpServletResponse response, List<BookStatus> books, String message) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>書籍・貸出状況 | 図書館</title>");
        out.println("<style>:root{font-family:system-ui,-apple-system,'Segoe UI',sans-serif;color:#20322e;background:#f3f6f2}*{box-sizing:border-box}body{margin:0}.top{background:#173f37;color:#fff;padding:34px max(24px,calc((100% - 1060px)/2)) 38px}.eyebrow{font-size:.78rem;letter-spacing:.14em;color:#b8d5c7;margin:0 0 8px}h1{font-size:clamp(1.6rem,4vw,2.25rem);margin:0;font-weight:700}main{max-width:1060px;margin:30px auto;padding:0 24px 48px}.summary{color:#65736e;margin:0 0 14px}.toolbar{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}.toolbar .summary{margin:0}.button{border:0;border-radius:8px;background:#276344;color:white;padding:11px 18px;font:inherit;font-weight:650;cursor:pointer}.button:hover{background:#1c5037}.table-wrap{overflow-x:auto;background:#fff;border:1px solid #dce5df;border-radius:14px;box-shadow:0 12px 30px #173f370d}table{width:100%;border-collapse:collapse;min-width:700px}th,td{text-align:left;padding:15px 18px;border-bottom:1px solid #e8eeea}th{background:#f8faf8;color:#53645c;font-size:.88rem;font-weight:650}tbody tr:last-child td{border-bottom:0}td:first-child{font-weight:650;color:#203c34}.count{display:inline-grid;place-items:center;min-width:2rem;height:2rem;padding:0 .55rem;border-radius:999px;background:#e5f1e9;color:#276344;font-weight:700}.empty{color:#76827d}.no-books{text-align:center;padding:36px;color:#76827d}.notice{padding:12px 16px;margin:0 0 16px;border-radius:8px;background:#e5f1e9;color:#276344}.notice.error{background:#fff0ed;color:#963e2e}.modal{position:fixed;inset:0;z-index:5;display:none;place-items:center;padding:20px;background:#10241fb0}.modal.open{display:grid}.dialog{width:min(100%,480px);max-height:90vh;overflow:auto;background:#fff;border-radius:16px;padding:26px;box-shadow:0 22px 70px #10241f55}.dialog h2{margin:0 0 20px;font-size:1.35rem}.field{display:grid;gap:6px;margin:0 0 15px}.field label{font-weight:600;color:#344940}.field input{width:100%;padding:11px 12px;border:1px solid #cbd8d0;border-radius:8px;font:inherit}.field input:focus{outline:2px solid #85b99a;border-color:#276344}.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}.cancel{background:#eef2ef;color:#344940}body.modal-open{overflow:hidden}footer{max-width:1060px;margin:-30px auto 30px;padding:0 24px;color:#87918c;font-size:.8rem}@media(max-width:640px){.top{padding-top:26px;padding-bottom:30px}main{margin-top:20px;padding-right:14px;padding-left:14px}.toolbar{align-items:flex-start;gap:12px}.dialog{padding:22px}}</style></head><body>");
        out.println("<header class=\"top\"><p class=\"eyebrow\">LIBRARY CATALOG</p><h1>書籍・貸出状況</h1></header><main>");
        if (message != null) {
            String notice = "added".equals(message) ? "書籍を登録しました。"
                    : "duplicate".equals(message) ? "この ISBN はすでに登録されています。"
                    : "invalid".equals(message) ? "入力内容を確認してください。ISBN・タイトル・著者は必須です。"
                    : "書籍を登録できませんでした。入力内容とデータベースを確認してください。";
            if ("loaned".equals(message)) notice = "貸出しました。";
            else if ("reserved".equals(message)) notice = "予約しました。";
            else if ("alreadyReserved".equals(message)) notice = "この利用者はすでに予約しています。";
            else if ("reservationOrder".equals(message)) notice = "予約日時が最も新しい利用者のみ貸出できます。";
            else if ("loanInvalid".equals(message)) notice = "ISBNと氏名を確認してください。";
            else if ("notFound".equals(message)) notice = "該当するISBNの書籍がありません。";
            else if ("userNotFound".equals(message)) notice = "該当する氏名の利用者がありません。";
            else if ("error".equals(message)) notice = "処理に失敗しました。入力内容とデータベースを確認してください。";
            else if ("returned".equals(message)) notice = "返却しました。";
            else if ("notOnLoan".equals(message)) notice = "この書籍は貸出中ではありません。";
            else if ("returnInvalid".equals(message)) notice = "ISBNを確認してください。";
            else if ("returnError".equals(message)) notice = "返却処理に失敗しました。データベースを確認してください。";
            boolean success = "added".equals(message) || "loaned".equals(message)
                    || "reserved".equals(message) || "returned".equals(message);
            out.println("<p class=\"notice " + (success ? "" : "error") + "\" role=\"status\">" + notice + "</p>");
        }
        out.println("<div class=\"toolbar\"><p class=\"summary\">登録書籍 " + books.size() + " 冊</p><div><button class=\"button\" type=\"button\" id=\"open-loan\">貸出/予約</button> <button class=\"button\" type=\"button\" id=\"open-return\">返却</button> <button class=\"button\" type=\"button\" id=\"open-registration\">登録</button></div></div>");
        if (books.isEmpty()) out.println("<div class=\"table-wrap no-books\">表示できる書籍がありません。</div>");
        else {
            out.println("<div class=\"table-wrap\"><table><thead><tr><th>タイトル</th><th>巻</th><th>著者</th><th>貸出中のユーザ</th><th>予約人数</th></tr></thead><tbody>");
            for (BookStatus book : books) {
                out.println("<tr><td>" + escapeHtml(book.title) + "</td><td>" + (book.volume == null ? "—" : book.volume)
                        + "</td><td>" + escapeHtml(book.author) + "</td><td>");
                if ("—".equals(book.borrowers)) out.print("<span class=\"empty\">貸出なし</span>");
                else out.print(escapeHtml(book.borrowers));
                out.println("</td><td><span class=\"count\">" + book.reservationCount + "</span></td></tr>");
            }
            out.println("</tbody></table></div>");
        }
        out.println("</main><footer>貸出中は返却日時が未登録の貸出記録、予約人数は未貸出の予約件数を表示しています。</footer>");
        out.println("<div class=\"modal\" id=\"loan-modal\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"loan-title\"><section class=\"dialog\"><h2 id=\"loan-title\">貸出/予約</h2>");
        out.println("<form method=\"post\" action=\"" + escapeHtml(getServletContext().getContextPath()) + "/books\"><input type=\"hidden\" name=\"action\" value=\"loan\"><div class=\"field\"><label for=\"loan-isbn\">ISBN</label><input id=\"loan-isbn\" name=\"isbn\" type=\"text\" required maxlength=\"17\" pattern=\"[0-9Xx-]{10,17}\" placeholder=\"9784000000000\"></div>");
        out.println("<div class=\"field\"><label for=\"full-name\">氏名</label><input id=\"full-name\" name=\"fullName\" type=\"text\" required maxlength=\"200\"></div><div class=\"actions\"><button class=\"button cancel\" type=\"button\" id=\"close-loan\">キャンセル</button><button class=\"button\" type=\"submit\">決定</button></div></form></section></div>");
        out.println("<div class=\"modal\" id=\"return-modal\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"return-title\"><section class=\"dialog\"><h2 id=\"return-title\">返却</h2>");
        out.println("<form method=\"post\" action=\"" + escapeHtml(getServletContext().getContextPath()) + "/books\"><input type=\"hidden\" name=\"action\" value=\"return\"><div class=\"field\"><label for=\"return-isbn\">ISBN</label><input id=\"return-isbn\" name=\"isbn\" type=\"text\" required maxlength=\"17\" pattern=\"[0-9Xx-]{10,17}\" placeholder=\"9784000000000\"></div><div class=\"actions\"><button class=\"button cancel\" type=\"button\" id=\"close-return\">キャンセル</button><button class=\"button\" type=\"submit\">決定</button></div></form></section></div>");
        out.println("<div class=\"modal\" id=\"registration-modal\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"registration-title\"><section class=\"dialog\"><h2 id=\"registration-title\">書籍を登録</h2>");
        out.println("<form method=\"post\" action=\"" + escapeHtml(getServletContext().getContextPath()) + "/books\"><div class=\"field\"><label for=\"isbn\">ISBN</label><input id=\"isbn\" name=\"isbn\" type=\"text\" required maxlength=\"17\" pattern=\"[0-9Xx-]{10,17}\" placeholder=\"9784000000000\"></div>");
        out.println("<div class=\"field\"><label for=\"title\">タイトル</label><input id=\"title\" name=\"title\" type=\"text\" required maxlength=\"500\"></div><div class=\"field\"><label for=\"volume\">巻</label><input id=\"volume\" name=\"volume\" type=\"number\" min=\"0\"></div>");
        out.println("<div class=\"field\"><label for=\"author\">著者</label><input id=\"author\" name=\"author\" type=\"text\" required maxlength=\"500\"></div><div class=\"actions\"><button class=\"button cancel\" type=\"button\" id=\"close-registration\">キャンセル</button><button class=\"button\" type=\"submit\">決定</button></div></form></section></div>");
        out.println("<script>(function(){function setup(id,openId,closeId,inputId){var modal=document.getElementById(id);function open(){modal.classList.add('open');document.body.classList.add('modal-open');document.getElementById(inputId).focus()}function close(){modal.classList.remove('open');document.body.classList.remove('modal-open')}document.getElementById(openId).addEventListener('click',open);document.getElementById(closeId).addEventListener('click',close);modal.addEventListener('click',function(e){if(e.target===modal)close()});document.addEventListener('keydown',function(e){if(e.key==='Escape')close()});return open}setup('loan-modal','open-loan','close-loan','loan-isbn');setup('return-modal','open-return','close-return','return-isbn');setup('registration-modal','open-registration','close-registration','isbn');var message=new URLSearchParams(location.search).get('message');if(message&&message!=='added'&&['duplicate','invalid'].indexOf(message)>=0)document.getElementById('registration-modal').classList.add('open')})();</script></body></html>");
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void writeErrorPage(HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\"><title>接続エラー</title></head><body>");
        out.println("<h1>書籍情報を読み込めませんでした</h1><p>データベースの接続設定と PostgreSQL の稼働状況を確認してください。</p></body></html>");
    }

    private static final class BookStatus {
        private final String isbn;
        private final String title;
        private final Integer volume;
        private final String author;
        private final String borrowers;
        private final long reservationCount;
        private BookStatus(String isbn, String title, Integer volume, String author, String borrowers, long reservationCount) {
            this.isbn = isbn; this.title = title; this.volume = volume; this.author = author;
            this.borrowers = borrowers; this.reservationCount = reservationCount;
        }
    }
}
