package com.example.readinglog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReadingLogRepository {
    public record Book(String isbn, String title, Integer volume, String author,
                       String translator, int pageCount, String readingStatus) {}

    public record ReadingRecord(long id, LocalDate readingDate, String isbn, String title,
                                int pagesRead, String comment, boolean completed) {}

    public List<Book> findBooks() throws SQLException {
        String sql = "SELECT isbn, title, volume, author, translator, page_count, reading_status " +
                "FROM public.books ORDER BY title, volume NULLS FIRST";
        List<Book> books = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(rs.getString("isbn"), rs.getString("title"),
                        (Integer) rs.getObject("volume"), rs.getString("author"),
                        rs.getString("translator"), rs.getInt("page_count"),
                        rs.getString("reading_status")));
            }
        }
        return books;
    }

    public List<ReadingRecord> findReadingRecords() throws SQLException {
        String sql = "SELECT r.reading_record_id, r.reading_date, r.isbn, b.title, " +
                "r.pages_read, r.comment, r.is_completed FROM public.reading_records r " +
                "JOIN public.books b ON b.isbn = r.isbn " +
                "ORDER BY r.reading_date DESC, r.reading_record_id DESC";
        List<ReadingRecord> records = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                records.add(new ReadingRecord(rs.getLong("reading_record_id"),
                        rs.getDate("reading_date").toLocalDate(), rs.getString("isbn"),
                        rs.getString("title"), rs.getInt("pages_read"),
                        rs.getString("comment"), rs.getBoolean("is_completed")));
            }
        }
        return records;
    }

    public void createBook(Book book) throws SQLException {
        String sql = "INSERT INTO public.books " +
                "(isbn, title, volume, author, translator, page_count, reading_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.isbn());
            statement.setString(2, book.title());
            if (book.volume() == null) statement.setNull(3, java.sql.Types.INTEGER);
            else statement.setInt(3, book.volume());
            statement.setString(4, book.author());
            if (book.translator() == null || book.translator().isBlank()) statement.setNull(5, java.sql.Types.VARCHAR);
            else statement.setString(5, book.translator());
            statement.setInt(6, book.pageCount());
            statement.setString(7, book.readingStatus());
            statement.executeUpdate();
        }
    }

    public void createReadingRecord(LocalDate date, String isbn, int pagesRead,
                                    String comment, boolean completed) throws SQLException {
        String sql = "INSERT INTO public.reading_records " +
                "(reading_date, isbn, pages_read, comment, is_completed) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setString(2, isbn);
            statement.setInt(3, pagesRead);
            if (comment == null || comment.isBlank()) statement.setNull(4, java.sql.Types.VARCHAR);
            else statement.setString(4, comment);
            statement.setBoolean(5, completed);
            statement.executeUpdate();
        }
    }

    public void deleteBook(String isbn) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement records = connection.prepareStatement(
                    "DELETE FROM public.reading_records WHERE isbn = ?");
                 PreparedStatement book = connection.prepareStatement(
                    "DELETE FROM public.books WHERE isbn = ?")) {
                records.setString(1, isbn);
                records.executeUpdate();
                book.setString(1, isbn);
                book.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void deleteReadingRecord(long id) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM public.reading_records WHERE reading_record_id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
