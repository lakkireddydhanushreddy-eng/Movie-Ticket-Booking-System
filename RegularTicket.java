/**
 * RegularTicket class representing a standard ticket booking.
 * Extends the abstract Ticket class, showing inheritance.
 */
public class RegularTicket extends Ticket {

    public RegularTicket(int bookingId, String customerName, Movie movie, int ticketsCount, double pricePerTicket) {
        super(bookingId, customerName, movie, ticketsCount, pricePerTicket);
    }

    @Override
    public double calculateTotal() {
        // Standard calculation: count * ticket price
        return getTicketsCount() * getPricePerTicket();
    }

    @Override
    public String getTicketType() {
        return "REGULAR";
    }
}
