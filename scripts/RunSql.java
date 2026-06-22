import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RunSql {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException("Usage: RunSql <jdbcUrl> <username> <password> <sqlFile> <driverClass>");
        }
        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        String sqlFile = args[3];
        String driverClass = args[4];

        Class.forName(driverClass);
        String sql = Files.readString(Path.of(sqlFile), StandardCharsets.UTF_8);
        List<String> statements = splitStatements(sql);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             Statement statement = connection.createStatement()) {
            for (String item : statements) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                boolean hasResultSet = statement.execute(trimmed);
                if (hasResultSet) {
                    printResult(statement.getResultSet());
                }
            }
        }
    }

    private static List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char prev = i > 0 ? sql.charAt(i - 1) : '\0';
            if (ch == '\'' && !inDoubleQuote && prev != '\\') {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote && prev != '\\') {
                inDoubleQuote = !inDoubleQuote;
            }
            if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private static void printResult(ResultSet resultSet) throws Exception {
        ResultSetMetaData meta = resultSet.getMetaData();
        int columns = meta.getColumnCount();
        while (resultSet.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columns; i++) {
                if (i > 1) {
                    row.append(" | ");
                }
                row.append(meta.getColumnLabel(i)).append('=').append(resultSet.getString(i));
            }
            System.out.println(row);
        }
    }
}
