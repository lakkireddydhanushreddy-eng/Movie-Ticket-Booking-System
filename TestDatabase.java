import java.util.List;

/**
 * Automated test script to verify database operations programmatically.
 */
public class TestDatabase {
    public static void main(String[] args) {
        System.out.println("Running automated JDBC & SQL tests...");
        try {
            // 1. Initialize DB
            DatabaseHelper.initializeDatabase();
            System.out.println("[PASS] Database initialized successfully.");

            // 2. Fetch movies
            List<Movie> movies = DatabaseHelper.getAllMovies();
            System.out.println("[INFO] Available Movies in DB:");
            for (Movie m : movies) {
                System.out.println("  - ID: " + m.getId() + " | " + m.getTitle() + " | Price: Rs." + m.getPrice() + " | Seats: " + m.getAvailableSeats());
            }
            if (movies.isEmpty()) {
                throw new Exception("No movies found in database!");
            }
            System.out.println("[PASS] Fetched " + movies.size() + " movies.");

            // 3. Perform a regular booking
            Movie targetMovie = movies.get(0);
            int initialSeats = targetMovie.getAvailableSeats();
            System.out.println("[INFO] Testing booking for movie: " + targetMovie.getTitle() + " (Seats available: " + initialSeats + ")");

            Ticket regTicket = new RegularTicket(0, "Test Customer", targetMovie, 3, targetMovie.getPrice());
            DatabaseHelper.bookTicket(regTicket);
            System.out.println("[PASS] Regular booking successful! Generated Booking ID: " + regTicket.getBookingId());

            // 4. Verify seats decreased
            List<Movie> updatedMovies = DatabaseHelper.getAllMovies();
            Movie updatedMovie = null;
            for (Movie m : updatedMovies) {
                if (m.getId() == targetMovie.getId()) {
                    updatedMovie = m;
                    break;
                }
            }
            System.out.println("[INFO] Seats after booking: " + updatedMovie.getAvailableSeats());
            if (updatedMovie.getAvailableSeats() != initialSeats - 3) {
                throw new Exception("Seats count did not update correctly in DB! Expected: " + (initialSeats - 3) + ", Found: " + updatedMovie.getAvailableSeats());
            }
            System.out.println("[PASS] Movie seats successfully decremented.");

            // 5. Test VIP Booking with Surcharge
            Ticket vipTicket = new VIPTicket(0, "Test VIP Customer", targetMovie, 2, targetMovie.getPrice());
            DatabaseHelper.bookTicket(vipTicket);
            System.out.println("[PASS] VIP booking successful! Generated Booking ID: " + vipTicket.getBookingId());
            System.out.println("[INFO] VIP Total calculated: Rs. " + vipTicket.calculateTotal() + " (Expected: 2 * ( " + targetMovie.getPrice() + " + 150 ) = Rs. " + (2 * (targetMovie.getPrice() + 150)) + ")");
            if (vipTicket.calculateTotal() != 2 * (targetMovie.getPrice() + 150)) {
                throw new Exception("VIP Ticket price calculation error!");
            }
            System.out.println("[PASS] VIP Ticket polymorphism and calculations correct.");

            // 6. Retrieve bookings and check list size
            List<Ticket> bookings = DatabaseHelper.getAllBookings();
            System.out.println("[INFO] All Bookings in DB:");
            for (Ticket t : bookings) {
                System.out.println("  - Booking ID: " + t.getBookingId() + " | Customer: " + t.getCustomerName() + " | Movie: " + t.getMovie().getTitle() + " | Type: " + t.getTicketType() + " | Total: Rs." + t.calculateTotal());
            }
            if (bookings.isEmpty()) {
                throw new Exception("No bookings retrieved!");
            }
            System.out.println("[PASS] Retrieved " + bookings.size() + " bookings successfully from DB.");

            // 7. Cancel regular booking
            System.out.println("[INFO] Cancelling regular booking ID: " + regTicket.getBookingId());
            DatabaseHelper.cancelBooking(regTicket.getBookingId());
            System.out.println("[PASS] Booking cancelled successfully.");

            // 8. Verify seats refunded
            updatedMovies = DatabaseHelper.getAllMovies();
            for (Movie m : updatedMovies) {
                if (m.getId() == targetMovie.getId()) {
                    updatedMovie = m;
                    break;
                }
            }
            System.out.println("[INFO] Seats after cancellation: " + updatedMovie.getAvailableSeats());
            // It should be: initialSeats - 3 (regular booked) - 2 (vip booked) + 3 (regular cancelled) = initialSeats - 2
            if (updatedMovie.getAvailableSeats() != initialSeats - 2) {
                throw new Exception("Seats not refunded correctly! Expected: " + (initialSeats - 2) + ", Found: " + updatedMovie.getAvailableSeats());
            }
            System.out.println("[PASS] Seats refunded correctly.");

            // Clean up VIP booking
            DatabaseHelper.cancelBooking(vipTicket.getBookingId());
            System.out.println("[PASS] Cleaned up VIP booking.");

            System.out.println("\n>>> ALL JDBC DATABASE TESTS PASSED SUCCESSFULLY! <<<");

        } catch (Exception e) {
            System.err.println("\n[FAIL] Test encountered an exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
