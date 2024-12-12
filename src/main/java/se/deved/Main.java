package se.deved;

import javax.xml.transform.Result;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

public class Main {
    static Scanner scanner = new Scanner(System.in);
    static Connection connection;

    static void closeConnection() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    public static void main(String[] args) {
        try {
            connectToDatabase();
        } catch (SQLException ignored) {
            System.out.println("Failed to connect to database. Try again later.");
            return;
        }

        try {
            setupDatabaseTables();
        } catch (SQLException ignored) {
            System.out.println("Failed to create 'todo' table. Fix the SQL statement.");
            closeConnection();
            return;
        }

        try {
            runApp();
        } catch (Exception exception) {
            System.out.println("Something went wrong.");
            closeConnection();
            return;
        }

        closeConnection();
    }

    public static void runApp() {
        System.out.println("Welcome to the todo application!");
        System.out.println("create-todo    - Create and save a todo to database.");
        System.out.println("show-todos     - Fetch and show all saved todos.");
        System.out.println("view-todo      - Fetch and show a specific todo.");
        System.out.println("delete-todo    - Delete a previously saved todo.");
        System.out.println("update-todo    - Update a todo.");
        System.out.println("complete-todo  - Mark a todo as completed.");

        String commandName = scanner.nextLine();
        while (!commandName.equalsIgnoreCase("exit")) {
            if (commandName.equalsIgnoreCase("create-todo")) {
                createTodo();
            } else if (commandName.equalsIgnoreCase("show-todos")) {
                showTodos();
            } else if (commandName.equalsIgnoreCase("view-todo")) {
                showTodo();
            } else if (commandName.equalsIgnoreCase("delete-todo")) {
                deleteTodo();
            } else if (commandName.equalsIgnoreCase("update-todo")) {
                updateTodo();
            } else if (commandName.equalsIgnoreCase("complete-todo")) {
                completeTodo();
            } else {
                System.out.println("There is no such command, please try again.");
            }

            commandName = scanner.nextLine();
        }
    }

    public static void showTodos() {
        try (Statement selectStatement = connection.createStatement()) {
            try (ResultSet result = selectStatement.executeQuery("SELECT * FROM todos")) {
                while (result.next()) {
                    displayTodo(result);
                }
            }
        } catch (SQLException exception) {
            System.out.println("Failed to fetch todos from database.");
            return;
        }
    }

    public static void displayTodo(ResultSet result) throws SQLException {
        int id = result.getInt("id");
        String title = result.getString("title");
        Date deadlineDate = result.getDate("deadline_date");
        Date createdDate = result.getDate("created_date");
        boolean completed = result.getBoolean("completed");
        String username = result.getString("username");

        System.out.println("- (" + id + ") " + title);
        System.out.println("  Deadline: " + deadlineDate.toString());
        System.out.println("  Created: " + createdDate.toString());
        System.out.println("  Completed: " + (completed ? "Yes" : "No"));
        System.out.println("  User: " + username);
    }

    public static void showTodo() {
        System.out.print("Enter an id: ");
        int todoId = scanner.nextInt();
        scanner.nextLine();

        try (PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM todos WHERE id = ?")) {
            selectStatement.setInt(1, todoId);
            try (ResultSet result = selectStatement.executeQuery()) {
                if (result.next()) {
                    displayTodo(result);
                }
            }
        } catch (SQLException exception) {
            System.out.println("Failed to fetch todo from database.");
            return;
        }
    }

    public static void createTodo() {
        System.out.print("Enter a title: ");
        String title = scanner.nextLine();

        System.out.print("Enter a deadline date (YYYY-MM-DD): ");
        String deadlineString = scanner.nextLine();

        Date deadlineDate;
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            java.util.Date javaDate = dateFormat.parse(deadlineString);
            deadlineDate = new Date(javaDate.getTime());
        } catch (ParseException exception) {
            System.out.println("Could not parse date. Did you enter the correct format?");
            return;
        }

        System.out.print("Enter a username (or empty): ");
        String username = scanner.nextLine();

        try (PreparedStatement insertTodo = connection.prepareStatement("INSERT INTO todos (title, deadline_date, username) VALUES (?, ?, ?)")) {
            insertTodo.setString(1, title);
            insertTodo.setDate(2, deadlineDate);
            insertTodo.setString(3, username.isBlank() ? null : username);

            if (insertTodo.executeUpdate() == 0) {
                System.out.println("Nothing was inserted, perhaps there was a conflict?");
                return;
            }
        } catch (SQLException exception) {
            System.out.println("Failed to save to database.");
            return;
        }

        System.out.println("Saved todo to database.");

        // SQL INJECTIONS
        // String title = "SELECT password FROM users";
        // "INSERT INTO todos (title, deadline_date, username) VALUES (" + title + "," + deadline + "," + username + ")"
        // "INSERT INTO todos (title, deadline_date, username) VALUES ('STÄDA')"
        // "INSERT INTO todos (title, deadline_date, username) VALUES ('SELECT password FROM users')"
    }

    public static void deleteTodo() {
    }

    public static void completeTodo() {
    }

    public static void updateTodo() {
    }

    public static void setupDatabaseTables() throws SQLException {
        // Gammalt sätt att hantera resource-closing
        Statement createTablesStatement = null;
        try {
            createTablesStatement = connection.createStatement();

            createTablesStatement.execute("CREATE TABLE IF NOT EXISTS todos (" +
                    "id SERIAL PRIMARY KEY," +
                    "title TEXT NOT NULL," +
                    "deadline_date DATE," +
                    "created_date TIMESTAMP NOT NULL DEFAULT current_timestamp," +
                    "username TEXT," +
                    "completed BOOLEAN NOT NULL DEFAULT false)");
        } catch (SQLException exception) {
            throw exception;
        } finally {
            try {
                if (createTablesStatement != null) {
                    createTablesStatement.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public static void connectToDatabase() throws SQLException {
        String connectionString = "jdbc:postgresql://localhost/todo?user=postgres&password=password";
        connection = DriverManager.getConnection(connectionString);
    }
}