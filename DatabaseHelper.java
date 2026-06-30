import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that handles all database operations (JDBC + SQLite).
 */
public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:movie_booking.db";

    static {
        // Load the SQLite JDBC driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
        }
    }

    /**
     * Obtains a connection to the SQLite database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initializes the database, creating the movies and bookings tables if they don't exist.
     * Seeds initial movies if the movies table is empty.
     */
    public static void initializeDatabase() {
        String createMoviesTable = "CREATE TABLE IF NOT EXISTS movies (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT UNIQUE NOT NULL, " +
                "genre TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "available_seats INTEGER NOT NULL CHECK (available_seats >= 0)" +
                ");";

        String createBookingsTable = "CREATE TABLE IF NOT EXISTS bookings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_name TEXT NOT NULL, " +
                "movie_id INTEGER NOT NULL, " +
                "ticket_type TEXT NOT NULL CHECK (ticket_type IN ('REGULAR', 'VIP')), " +
                "tickets_count INTEGER NOT NULL CHECK (tickets_count > 0), " +
                "total_amount REAL NOT NULL, " +
                "booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create tables
            stmt.execute(createMoviesTable);
            stmt.execute(createBookingsTable);

            // Seed movies if table is empty
            seedInitialMovies(conn);

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seeds default movies into the database if the movies table is currently empty.
     */
    private static void seedInitialMovies(Connection conn) throws SQLException {
        String checkEmptyQuery = "SELECT COUNT(*) FROM movies";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkEmptyQuery)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertMovie = "INSERT INTO movies (title, genre, price, available_seats) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertMovie)) {
                    // Movie 1
                    pstmt.setString(1, "Leo");
                    pstmt.setString(2, "Action/Thriller");
                    pstmt.setDouble(3, 200.0);
                    pstmt.setInt(4, 50);
                    pstmt.executeUpdate();

                    // Movie 2
                    pstmt.setString(1, "Pushpa 2");
                    pstmt.setString(2, "Action/Drama");
                    pstmt.setDouble(3, 250.0);
                    pstmt.setInt(4, 60);
                    pstmt.executeUpdate();

                    // Movie 3
                    pstmt.setString(1, "Salaar");
                    pstmt.setString(2, "Action/Drama");
                    pstmt.setDouble(3, 220.0);
                    pstmt.setInt(4, 45);
                    pstmt.executeUpdate();

                    // Movie 4
                    pstmt.setString(1, "RRR");
                    pstmt.setString(2, "Epic Action/Drama");
                    pstmt.setDouble(3, 180.0);
                    pstmt.setInt(4, 80);
                    pstmt.executeUpdate();

                    // Movie 5
                    pstmt.setString(1, "Kalki 2898 AD");
                    pstmt.setString(2, "Sci-Fi/Mythology");
                    pstmt.setDouble(3, 300.0);
                    pstmt.setInt(4, 40);
                    pstmt.executeUpdate();
                }
                System.out.println("Database seeded with default movies.");
            }
        }
    }

    /**
     * Fetches all movies from the database.
     */
    public static List<Movie> getAllMovies() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String query = "SELECT * FROM movies ORDER BY title ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                movies.add(new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getDouble("price"),
                        rs.getInt("available_seats")
                ));
            }
        }
        return movies;
    }

    /**
     * Adds a new movie to the database.
     */
    public static void addMovie(String title, String genre, double price, int seats) throws SQLException {
        String query = "INSERT INTO movies (title, genre, price, available_seats) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, title);
            pstmt.setString(2, genre);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, seats);
            pstmt.executeUpdate();
        }
    }

    /**
     * Books a ticket. Performs dynamic update on available seats and inserts the booking.
     * Uses database transactions to ensure consistency.
     */
    public static void bookTicket(Ticket ticket) throws SQLException {
        Connection conn = null;
        PreparedStatement checkSeatsStmt = null;
        PreparedStatement updateSeatsStmt = null;
        PreparedStatement insertBookingStmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Check current seats available
            String checkSeatsQuery = "SELECT available_seats FROM movies WHERE id = ?";
            checkSeatsStmt = conn.prepareStatement(checkSeatsQuery);
            checkSeatsStmt.setInt(1, ticket.getMovie().getId());
            rs = checkSeatsStmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Movie with ID " + ticket.getMovie().getId() + " does not exist.");
            }

            int currentSeats = rs.getInt("available_seats");
            if (currentSeats < ticket.getTicketsCount()) {
                throw new SQLException("Insufficient seats! Available: " + currentSeats + ", Requested: " + ticket.getTicketsCount());
            }

            // 2. Update the movie's available seats
            String updateSeatsQuery = "UPDATE movies SET available_seats = available_seats - ? WHERE id = ?";
            updateSeatsStmt = conn.prepareStatement(updateSeatsQuery);
            updateSeatsStmt.setInt(1, ticket.getTicketsCount());
            updateSeatsStmt.setInt(2, ticket.getMovie().getId());
            int updatedRows = updateSeatsStmt.executeUpdate();

            if (updatedRows == 0) {
                throw new SQLException("Failed to update movie seats.");
            }

            // 3. Insert booking details
            String insertBookingQuery = "INSERT INTO bookings (customer_name, movie_id, ticket_type, tickets_count, total_amount) VALUES (?, ?, ?, ?, ?)";
            insertBookingStmt = conn.prepareStatement(insertBookingQuery, Statement.RETURN_GENERATED_KEYS);
            insertBookingStmt.setString(1, ticket.getCustomerName());
            insertBookingStmt.setInt(2, ticket.getMovie().getId());
            insertBookingStmt.setString(3, ticket.getTicketType());
            insertBookingStmt.setInt(4, ticket.getTicketsCount());
            insertBookingStmt.setDouble(5, ticket.calculateTotal());
            insertBookingStmt.executeUpdate();

            // Set generated ID back to the ticket object
            try (ResultSet generatedKeys = insertBookingStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    ticket.setBookingId(generatedKeys.getInt(1));
                }
            }

            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                    System.out.println("Transaction rolled back.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (checkSeatsStmt != null) checkSeatsStmt.close();
            if (updateSeatsStmt != null) updateSeatsStmt.close();
            if (insertBookingStmt != null) insertBookingStmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Cancels a booking, deletes it from the bookings table, and refunds the seats to the movies table.
     * Uses database transactions.
     */
    public static void cancelBooking(int bookingId) throws SQLException {
        Connection conn = null;
        PreparedStatement getBookingStmt = null;
        PreparedStatement deleteBookingStmt = null;
        PreparedStatement refundSeatsStmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Get booking details (movie_id and tickets_count)
            String getBookingQuery = "SELECT movie_id, tickets_count FROM bookings WHERE id = ?";
            getBookingStmt = conn.prepareStatement(getBookingQuery);
            getBookingStmt.setInt(1, bookingId);
            rs = getBookingStmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Booking ID " + bookingId + " not found.");
            }

            int movieId = rs.getInt("movie_id");
            int ticketsCount = rs.getInt("tickets_count");

            // 2. Delete the booking
            String deleteBookingQuery = "DELETE FROM bookings WHERE id = ?";
            deleteBookingStmt = conn.prepareStatement(deleteBookingQuery);
            deleteBookingStmt.setInt(1, bookingId);
            int deleted = deleteBookingStmt.executeUpdate();

            if (deleted == 0) {
                throw new SQLException("Failed to delete booking.");
            }

            // 3. Refund the seats back to the movie
            String refundSeatsQuery = "UPDATE movies SET available_seats = available_seats + ? WHERE id = ?";
            refundSeatsStmt = conn.prepareStatement(refundSeatsQuery);
            refundSeatsStmt.setInt(1, ticketsCount);
            refundSeatsStmt.setInt(2, movieId);
            refundSeatsStmt.executeUpdate();

            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (getBookingStmt != null) getBookingStmt.close();
            if (deleteBookingStmt != null) deleteBookingStmt.close();
            if (refundSeatsStmt != null) refundSeatsStmt.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * Retrieves all bookings from the database. Uses inheritance by instantiating either
     * RegularTicket or VIPTicket based on the stored ticket type.
     */
    public static List<Ticket> getAllBookings() throws SQLException {
        List<Ticket> bookings = new ArrayList<>();
        String query = "SELECT b.id AS booking_id, b.customer_name, b.ticket_type, b.tickets_count, " +
                "b.total_amount, m.id AS movie_id, m.title, m.genre, m.price AS movie_price, m.available_seats " +
                "FROM bookings b JOIN movies m ON b.movie_id = m.id ORDER BY b.id DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int bookingId = rs.getInt("booking_id");
                String customerName = rs.getString("customer_name");
                String ticketType = rs.getString("ticket_type");
                int ticketsCount = rs.getInt("tickets_count");
                
                Movie movie = new Movie(
                        rs.getInt("movie_id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getDouble("movie_price"),
                        rs.getInt("available_seats")
                );

                Ticket ticket;
                if ("VIP".equalsIgnoreCase(ticketType)) {
                    ticket = new VIPTicket(bookingId, customerName, movie, ticketsCount, movie.getPrice());
                } else {
                    ticket = new RegularTicket(bookingId, customerName, movie, ticketsCount, movie.getPrice());
                }
                
                bookings.add(ticket);
            }
        }
        return bookings;
    }

    /**
     * Updates an existing movie in the database.
     */
    public static void updateMovie(int id, String title, String genre, double price, int seats) throws SQLException {
        String query = "UPDATE movies SET title = ?, genre = ?, price = ?, available_seats = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, title);
            pstmt.setString(2, genre);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, seats);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes a movie from the database by ID.
     * Foreign Key constraint cascades to delete its bookings.
     */
    public static void deleteMovie(int id) throws SQLException {
        String query = "DELETE FROM movies WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
