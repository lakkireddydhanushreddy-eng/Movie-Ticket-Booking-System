# CineBook - Feature Expansion Plan

We will add advanced premium features to the Movie Ticket Booking System to make it a complete, production-ready desktop application.

## Proposed Features

### 1. Admin Panel Tab
We will add a new tab to the GUI restricted to administrative controls:
- **Manage Movies Table**: View all movies, their genres, prices, and total/available seats.
- **Add Movie**: Form to input Title, Genre, Base Price, and Initial Seats capacity.
- **Update Movie**: Edit price or seats of an existing movie.
- **Delete Movie**: Remove a movie from the catalog.

### 2. Search & Filter Bar
At the top of the **Browse Movies** tab, we will add a search control:
- **Search Field**: Dynamic filtering by title in real-time as the user types.
- **Genre Filter**: Combo box showing available genres (e.g., Action, Sci-Fi, Drama) to filter the catalog.

### 3. Receipt Exporting
In the visual **Booking Summary / Receipt** dialog, we will add:
- **Save Receipt Button**: Opens a `JFileChooser` dialog to let the user save the ticket receipt as a `.txt` file on their system.

---

## File Modifications

### 1. [MODIFY] [DatabaseHelper.java](file:///c:/Users/Sruth/OneDrive/Desktop/Movie%20ticket%20booking/DatabaseHelper.java)
- Add method `updateMovie(Movie movie)` to modify price and capacity in SQL.
- Add method `deleteMovie(int movieId)` to delete a movie.
- Ensure foreign key constraints cascades deletion of related bookings.

### 2. [MODIFY] [MovieBookingSystem.java](file:///c:/Users/Sruth/OneDrive/Desktop/Movie%20ticket%20booking/MovieBookingSystem.java)
- Add **Search and Genre filter panel** to "Browse Movies" tab.
- Add a new tab **"Admin Console"** with movie management forms and table.
- Add "Save Receipt" functionality with file output operations in the receipt dialog.

---

## Verification Plan

### Automated Verification
We will run `javac` to verify compile success, then launch the program.

### Manual Verification
1. **Search & Filter**: Type "Leo" in the search box; only "Leo" should show up. Select "Sci-Fi" in genre; only Sci-Fi movies should appear.
2. **Admin Panel**:
   - Add a new movie "Avatar 3" with 100 seats, base price Rs. 350. Check that it populates the main catalog.
   - Delete a movie and ensure it disappears from both the admin table and catalog.
3. **Save Receipt**: Click "Save Receipt", choose a file path, save it, and verify that the file contains the text ticket receipt.
