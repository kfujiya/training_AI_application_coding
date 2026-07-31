<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="com.example.readinglog.Html" %>
<%@ page import="com.example.readinglog.ReadingLogRepository" %>
<%
    List<ReadingLogRepository.Book> books = (List<ReadingLogRepository.Book>) request.getAttribute("books");
    List<ReadingLogRepository.ReadingRecord> records = (List<ReadingLogRepository.ReadingRecord>) request.getAttribute("readingRecords");
    String activeTab = "books".equals(request.getParameter("tab")) ? "books" : "records";
%>
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>読書記録</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body data-active-tab="<%= activeTab %>">
<main class="container">
    <header class="page-header">
        <div><p class="eyebrow">READING JOURNAL</p><h1>読書記録</h1></div>
        <button class="primary-button" id="open-create" type="button">＋ 登録</button>
    </header>

    <% if (request.getParameter("message") != null) { %>
        <div class="notice success"><%= Html.escape(request.getParameter("message")) %></div>
    <% } %>
    <% if (request.getParameter("error") != null) { %>
        <div class="notice error"><%= Html.escape(request.getParameter("error")) %></div>
    <% } %>

    <nav class="tabs" aria-label="表示切替">
        <button type="button" class="tab-button" data-tab="books">書籍一覧 <span><%= books.size() %></span></button>
        <button type="button" class="tab-button" data-tab="records">読書記録 <span><%= records.size() %></span></button>
    </nav>

    <section class="tab-panel" id="books-panel">
        <div class="table-wrap"><table>
            <thead><tr><th>ISBN</th><th>タイトル</th><th>巻</th><th>著者</th><th>翻訳</th><th>ページ数</th><th>読破状況</th><th></th></tr></thead>
            <tbody>
            <% for (ReadingLogRepository.Book book : books) { %>
                <tr>
                    <td class="mono"><%= Html.escape(book.isbn()) %></td>
                    <td class="title-cell"><%= Html.escape(book.title()) %></td>
                    <td><%= book.volume() == null ? "—" : book.volume() %></td>
                    <td><%= Html.escape(book.author()) %></td>
                    <td><%= book.translator() == null ? "—" : Html.escape(book.translator()) %></td>
                    <td><%= book.pageCount() %></td>
                    <td><span class="status <%= book.readingStatus() %>"><%= "completed".equals(book.readingStatus()) ? "読破済み" : "reading".equals(book.readingStatus()) ? "読書中" : "未読" %></span></td>
                    <td><form method="post" action="<%= request.getContextPath() %>/app" class="delete-form" data-label="<%= Html.escape(book.title()) %>">
                        <input type="hidden" name="action" value="deleteBook"><input type="hidden" name="isbn" value="<%= Html.escape(book.isbn()) %>">
                        <button class="delete-button" type="submit">削除</button>
                    </form></td>
                </tr>
            <% } %>
            </tbody>
        </table></div>
    </section>

    <section class="tab-panel" id="records-panel">
        <div class="table-wrap"><table>
            <thead><tr><th>日付</th><th>書籍</th><th>ISBN</th><th>読破ページ数</th><th>コメント</th><th>完読</th><th></th></tr></thead>
            <tbody>
            <% for (ReadingLogRepository.ReadingRecord record : records) { %>
                <tr>
                    <td><%= record.readingDate() %></td><td class="title-cell"><%= Html.escape(record.title()) %></td>
                    <td class="mono"><%= Html.escape(record.isbn()) %></td><td><%= record.pagesRead() %> ページ</td>
                    <td class="comment-cell"><%= Html.escape(record.comment()) %></td>
                    <td><span class="complete-mark <%= record.completed() ? "yes" : "" %>"><%= record.completed() ? "✓" : "—" %></span></td>
                    <td><form method="post" action="<%= request.getContextPath() %>/app" class="delete-form" data-label="<%= record.readingDate() %> の読書記録">
                        <input type="hidden" name="action" value="deleteRecord"><input type="hidden" name="recordId" value="<%= record.id() %>">
                        <button class="delete-button" type="submit">削除</button>
                    </form></td>
                </tr>
            <% } %>
            </tbody>
        </table></div>
    </section>
</main>

<dialog id="book-dialog">
    <form method="post" action="<%= request.getContextPath() %>/app" class="modal-form"><input type="hidden" name="action" value="createBook">
        <div class="modal-header"><div><p class="eyebrow">NEW BOOK</p><h2>書籍を登録</h2></div><button type="button" class="close-button" aria-label="閉じる">×</button></div>
        <div class="form-grid">
            <label class="wide">ISBN<input name="isbn" required pattern="[0-9]{10}([0-9]{3})?" maxlength="13" placeholder="9784101010014"></label>
            <label class="wide">タイトル<input name="title" required maxlength="255"></label>
            <label>巻<input name="volume" type="number" min="1" placeholder="任意"></label>
            <label>ページ数<input name="pageCount" type="number" required min="1"></label>
            <label class="wide">著者<input name="author" required maxlength="255"></label>
            <label class="wide">翻訳<input name="translator" maxlength="255" placeholder="任意"></label>
            <label class="wide">読破状況<select name="readingStatus"><option value="unread">未読</option><option value="reading">読書中</option><option value="completed">読破済み</option></select></label>
        </div>
        <div class="modal-actions"><button type="button" class="secondary-button close-button">キャンセル</button><button type="submit" class="primary-button">決定</button></div>
    </form>
</dialog>

<dialog id="record-dialog">
    <form method="post" action="<%= request.getContextPath() %>/app" class="modal-form"><input type="hidden" name="action" value="createRecord">
        <div class="modal-header"><div><p class="eyebrow">NEW ENTRY</p><h2>読書記録を登録</h2></div><button type="button" class="close-button" aria-label="閉じる">×</button></div>
        <div class="form-grid">
            <label class="wide">日付<input name="readingDate" type="date" required value="<%= java.time.LocalDate.now() %>"></label>
            <label class="wide">書籍<select name="isbn" required><option value="">選択してください</option>
                <% for (ReadingLogRepository.Book book : books) { %><option value="<%= Html.escape(book.isbn()) %>"><%= Html.escape(book.title()) %><%= book.volume() == null ? "" : " " + book.volume() + "巻" %>（<%= Html.escape(book.isbn()) %>）</option><% } %>
            </select></label>
            <label class="wide">読破ページ数<input name="pagesRead" type="number" min="0" required></label>
            <label class="wide">コメント<textarea name="comment" rows="4" placeholder="読んだ感想やメモ（任意）"></textarea></label>
            <label class="checkbox wide"><input name="completed" type="checkbox" value="true"> この読書で完読した</label>
        </div>
        <div class="modal-actions"><button type="button" class="secondary-button close-button">キャンセル</button><button type="submit" class="primary-button">決定</button></div>
    </form>
</dialog>
<script src="<%= request.getContextPath() %>/assets/app.js"></script>
</body>
</html>
