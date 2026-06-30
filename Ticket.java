/**
 * Abstract class representing a generic ticket booking.
 * Demonstrates inheritance and encapsulation.
 */
public abstract class Ticket {
    private int bookingId;
    private String customerName;
    private Movie movie;
    private int ticketsCount;
    private double pricePerTicket;

    // Constructor
    public Ticket(int bookingId, String customerName, Movie movie, int ticketsCount, double pricePerTicket) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.movie = movie;
        this.ticketsCount = ticketsCount;
        this.pricePerTicket = pricePerTicket;
    }

    // Getters and Setters
    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public int getTicketsCount() {
        return ticketsCount;
    }

    public void setTicketsCount(int ticketsCount) {
        this.ticketsCount = ticketsCount;
    }

    public double getPricePerTicket() {
        return pricePerTicket;
    }

    public void setPricePerTicket(double pricePerTicket) {
        this.pricePerTicket = pricePerTicket;
    }

    /**
     * Abstract method to calculate the total booking cost.
     * Must be implemented by subclasses.
     */
    public abstract double calculateTotal();

    /**
     * Returns the type of the ticket.
     */
    public abstract String getTicketType();
}
