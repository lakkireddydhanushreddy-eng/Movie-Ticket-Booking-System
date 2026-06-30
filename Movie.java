/**
 * Movie class representing a movie in the booking system.
 * Demonstrates encapsulation with private fields and getter/setter methods.
 */
public class Movie {
    private int id;
    private String title;
    private String genre;
    private double price;
    private int availableSeats;

    // Constructor with all fields
    public Movie(int id, String title, String genre, double price, int availableSeats) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.price = price;
        this.availableSeats = availableSeats;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    @Override
    public String toString() {
        return title + " (" + genre + ") - Rs. " + price + " [Seats: " + availableSeats + "]";
    }
}
