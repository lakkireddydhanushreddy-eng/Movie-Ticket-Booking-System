/**
 * VIPTicket class representing a VIP ticket booking.
 * Extends the abstract Ticket class, showing inheritance.
 */
public class VIPTicket extends Ticket {
    // Surcharge for VIP tickets (e.g. recliner seats, lounge access)
    private static final double VIP_SURCHARGE = 150.0;

    public VIPTicket(int bookingId, String customerName, Movie movie, int ticketsCount, double pricePerTicket) {
        super(bookingId, customerName, movie, ticketsCount, pricePerTicket);
    }

    @Override
    public double calculateTotal() {
        // VIP price calculation: (ticket price + VIP surcharge) * count
        return getTicketsCount() * (getPricePerTicket() + VIP_SURCHARGE);
    }

    @Override
    public String getTicketType() {
        return "VIP";
    }

    public double getSurcharge() {
        return VIP_SURCHARGE;
    }
}
