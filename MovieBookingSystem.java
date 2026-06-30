import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CineBook - Movie Ticket Booking System.
 * Main GUI class utilizing Swing and FlatLaf theme.
 * Expanded with Admin Movie Console, Catalog Search/Filter, and Receipt Exporting.
 */
public class MovieBookingSystem extends JFrame {

    private JTabbedPane tabbedPane;
    
    // Catalog Tab
    private JPanel moviesPanel;
    private JTextField searchField;
    private JComboBox<String> genreFilterCombo;
    private boolean isUpdatingGenres = false;

    // Bookings Tab
    private JTable bookingsTable;
    private DefaultTableModel bookingsTableModel;
    
    // Admin Tab
    private JTable adminMoviesTable;
    private DefaultTableModel adminMoviesTableModel;
    private JTextField adminIdField;
    private JTextField adminTitleField;
    private JTextField adminGenreField;
    private JSpinner adminPriceSpinner;
    private JSpinner adminSeatsSpinner;
    
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public MovieBookingSystem() {
        super("CineBook - Premium Movie Ticket Booking System");
        
        // Initialize database
        DatabaseHelper.initializeDatabase();

        // Setup Window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null); // Center window
        
        // Initialize Look & Feel (FlatLaf)
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Could not load FlatLaf: " + e.getMessage());
        }

        // Main Layout
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create Header Panel
        mainContainer.add(createHeaderPanel(), BorderLayout.NORTH);

        // Create Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Tab 1: Catalog
        tabbedPane.addTab("Browse Movies", createCatalogPanel());

        // Tab 2: Bookings
        tabbedPane.addTab("Manage Bookings", createBookingsPanel());

        // Tab 3: Admin Console
        tabbedPane.addTab("Admin Console", createAdminPanel());

        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        add(mainContainer);

        // Load initial data
        refreshGenreFilter();
        refreshMovies();
        refreshBookings();
    }

    /**
     * Creates a beautiful header bar.
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 30, 40));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("🎬 ");
        logoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        brandPanel.add(logoLabel);

        JPanel titleSubPanel = new JPanel(new GridLayout(2, 1));
        titleSubPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("CINEBOOK");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(255, 102, 102)); // Vibrant accent color
        
        JLabel subtitleLabel = new JLabel("Premium Movie Ticket Booking System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);

        titleSubPanel.add(titleLabel);
        titleSubPanel.add(subtitleLabel);
        brandPanel.add(titleSubPanel);

        headerPanel.add(brandPanel, BorderLayout.WEST);

        // Add a refresh button in the header
        JButton refreshBtn = new JButton("Refresh System Data");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBackground(new Color(50, 50, 70));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> {
            refreshGenreFilter();
            refreshMovies();
            refreshBookings();
            refreshAdminMoviesTable();
            JOptionPane.showMessageDialog(this, "System data refreshed from database.", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        actionPanel.setOpaque(false);
        actionPanel.add(refreshBtn);
        headerPanel.add(actionPanel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the Catalog Panel with search, genre filters, and movie cards.
     */
    private JPanel createCatalogPanel() {
        JPanel catalogPanel = new JPanel(new BorderLayout());

        // Search & Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        filterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 70)));

        filterPanel.add(new JLabel("Search Movie:"));
        searchField = new JTextField(15);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { performFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { performFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { performFilter(); }
            private void performFilter() {
                refreshMovies(searchField.getText(), (String) genreFilterCombo.getSelectedItem());
            }
        });
        filterPanel.add(searchField);

        filterPanel.add(new JLabel("Genre:"));
        genreFilterCombo = new JComboBox<>(new String[]{"All Genres"});
        genreFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        genreFilterCombo.addActionListener(e -> {
            if (!isUpdatingGenres) {
                refreshMovies(searchField.getText(), (String) genreFilterCombo.getSelectedItem());
            }
        });
        filterPanel.add(genreFilterCombo);

        catalogPanel.add(filterPanel, BorderLayout.NORTH);

        // Movies Scroll View
        moviesPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        moviesPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scrollPane = new JScrollPane(moviesPanel);
        scrollPane.setBorder(null);
        catalogPanel.add(scrollPane, BorderLayout.CENTER);

        return catalogPanel;
    }

    /**
     * Creates the managing panel for bookings.
     */
    private JPanel createBookingsPanel() {
        JPanel bookingsPanel = new JPanel(new BorderLayout(10, 10));
        bookingsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Create Bookings Table
        String[] columnNames = {"Booking ID", "Customer Name", "Movie Title", "Type", "Seats Booked", "Total Price", "Date"};
        bookingsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Immutable table cells
            }
        };

        bookingsTable = new JTable(bookingsTableModel);
        bookingsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bookingsTable.setRowHeight(25);
        bookingsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column aligning
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        bookingsTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        bookingsTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        bookingsTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        bookingsTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        bookingsTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(bookingsTable);
        bookingsPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton viewReceiptBtn = new JButton("View Booking Summary / Receipt");
        viewReceiptBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        viewReceiptBtn.setBackground(new Color(0, 153, 102));
        viewReceiptBtn.setForeground(Color.WHITE);
        viewReceiptBtn.addActionListener(e -> viewReceiptAction());
        buttonPanel.add(viewReceiptBtn);

        JButton cancelBookingBtn = new JButton("Cancel Booking");
        cancelBookingBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancelBookingBtn.setBackground(new Color(204, 51, 51));
        cancelBookingBtn.setForeground(Color.WHITE);
        cancelBookingBtn.addActionListener(e -> cancelBookingAction());
        buttonPanel.add(cancelBookingBtn);

        bookingsPanel.add(buttonPanel, BorderLayout.SOUTH);

        return bookingsPanel;
    }

    /**
     * Creates the Admin Console panel.
     */
    private JPanel createAdminPanel() {
        JPanel adminPanel = new JPanel(new BorderLayout(15, 15));
        adminPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Movie List Table (Left Side)
        String[] columns = {"ID", "Title", "Genre", "Price", "Available Seats"};
        adminMoviesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        adminMoviesTable = new JTable(adminMoviesTableModel);
        adminMoviesTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        adminMoviesTable.setRowHeight(25);
        adminMoviesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(adminMoviesTable);

        // Right Align price column
        DefaultTableCellRenderer priceRenderer = new DefaultTableCellRenderer();
        priceRenderer.setHorizontalAlignment(JLabel.RIGHT);
        adminMoviesTable.getColumnModel().getColumn(3).setCellRenderer(priceRenderer);

        // Center Align ID and Seats columns
        DefaultTableCellRenderer centerAlign = new DefaultTableCellRenderer();
        centerAlign.setHorizontalAlignment(JLabel.CENTER);
        adminMoviesTable.getColumnModel().getColumn(0).setCellRenderer(centerAlign);
        adminMoviesTable.getColumnModel().getColumn(4).setCellRenderer(centerAlign);

        adminPanel.add(tableScroll, BorderLayout.CENTER);

        // Details Form Panel (Right Side)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setPreferredSize(new Dimension(380, 0));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Movie Management Form"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0; gbc.gridy = 0;

        // Form Fields
        formPanel.add(new JLabel("Movie ID:"), gbc);
        gbc.gridx = 1;
        adminIdField = new JTextField(15);
        adminIdField.setEditable(false);
        formPanel.add(adminIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        adminTitleField = new JTextField(15);
        formPanel.add(adminTitleField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Genre:"), gbc);
        gbc.gridx = 1;
        adminGenreField = new JTextField(15);
        formPanel.add(adminGenreField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Base Price:"), gbc);
        gbc.gridx = 1;
        adminPriceSpinner = new JSpinner(new SpinnerNumberModel(150.0, 50.0, 1000.0, 10.0));
        formPanel.add(adminPriceSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Capacity (Seats):"), gbc);
        gbc.gridx = 1;
        adminSeatsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 500, 5));
        formPanel.add(adminSeatsSpinner, gbc);

        // Buttons Panels
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        JPanel actionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        
        JButton addBtn = new JButton("Add Movie");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setBackground(new Color(51, 153, 255));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> addMovieAction());
        actionsPanel.add(addBtn);

        JButton updateBtn = new JButton("Update Movie");
        updateBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        updateBtn.setBackground(new Color(255, 153, 51));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.addActionListener(e -> updateMovieAction());
        actionsPanel.add(updateBtn);

        JButton deleteBtn = new JButton("Delete Movie");
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteBtn.setBackground(new Color(204, 51, 51));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.addActionListener(e -> deleteMovieAction());
        actionsPanel.add(deleteBtn);

        JButton clearBtn = new JButton("Clear Form");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearBtn.addActionListener(e -> clearAdminForm());
        actionsPanel.add(clearBtn);

        formPanel.add(actionsPanel, gbc);
        adminPanel.add(formPanel, BorderLayout.EAST);

        // Table selection listener
        adminMoviesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = adminMoviesTable.getSelectedRow();
                if (row != -1) {
                    adminIdField.setText(adminMoviesTableModel.getValueAt(row, 0).toString());
                    adminTitleField.setText(adminMoviesTableModel.getValueAt(row, 1).toString());
                    adminGenreField.setText(adminMoviesTableModel.getValueAt(row, 2).toString());
                    
                    // Parse double
                    String priceStr = adminMoviesTableModel.getValueAt(row, 3).toString().replace("Rs. ", "").replace(",", "");
                    adminPriceSpinner.setValue(Double.parseDouble(priceStr));
                    
                    adminSeatsSpinner.setValue(Integer.parseInt(adminMoviesTableModel.getValueAt(row, 4).toString()));
                }
            }
        });

        // Initialize admin table
        refreshAdminMoviesTable();

        return adminPanel;
    }

    /**
     * Clear the fields in the admin console.
     */
    private void clearAdminForm() {
        adminIdField.setText("");
        adminTitleField.setText("");
        adminGenreField.setText("");
        adminPriceSpinner.setValue(150.0);
        adminSeatsSpinner.setValue(50);
        adminMoviesTable.clearSelection();
    }

    /**
     * Admin action to add a movie.
     */
    private void addMovieAction() {
        String title = adminTitleField.getText().trim();
        String genre = adminGenreField.getText().trim();
        double price = (double) adminPriceSpinner.getValue();
        int seats = (int) adminSeatsSpinner.getValue();

        if (title.isEmpty() || genre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all movie fields.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.addMovie(title, genre, price, seats);
            JOptionPane.showMessageDialog(this, "Movie successfully added!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearAdminForm();
            refreshGenreFilter();
            refreshMovies();
            refreshAdminMoviesTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to add movie:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Admin action to update a movie.
     */
    private void updateMovieAction() {
        String idStr = adminIdField.getText();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a movie from the table to update.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(idStr);
        String title = adminTitleField.getText().trim();
        String genre = adminGenreField.getText().trim();
        double price = (double) adminPriceSpinner.getValue();
        int seats = (int) adminSeatsSpinner.getValue();

        if (title.isEmpty() || genre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all movie fields.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseHelper.updateMovie(id, title, genre, price, seats);
            JOptionPane.showMessageDialog(this, "Movie successfully updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearAdminForm();
            refreshGenreFilter();
            refreshMovies();
            refreshBookings(); // Booking details might change
            refreshAdminMoviesTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to update movie:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Admin action to delete a movie.
     */
    private void deleteMovieAction() {
        String idStr = adminIdField.getText();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a movie from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = Integer.parseInt(idStr);
        String title = adminTitleField.getText();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete movie:\n" + title + "?\nThis will cascade delete all bookings associated with this movie!",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.deleteMovie(id);
                JOptionPane.showMessageDialog(this, "Movie and its bookings deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                clearAdminForm();
                refreshGenreFilter();
                refreshMovies();
                refreshBookings();
                refreshAdminMoviesTable();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to delete movie:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Refreshes the admin movies list table.
     */
    private void refreshAdminMoviesTable() {
        adminMoviesTableModel.setRowCount(0);
        try {
            List<Movie> movies = DatabaseHelper.getAllMovies();
            for (Movie movie : movies) {
                adminMoviesTableModel.addRow(new Object[]{
                        movie.getId(),
                        movie.getTitle(),
                        movie.getGenre(),
                        "Rs. " + currencyFormat.format(movie.getPrice()),
                        movie.getAvailableSeats()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading admin movies: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Dynamically collects unique genres from database and loads into combo box.
     */
    private void refreshGenreFilter() {
        isUpdatingGenres = true;
        Object selected = genreFilterCombo.getSelectedItem();
        
        genreFilterCombo.removeAllItems();
        genreFilterCombo.addItem("All Genres");

        try {
            List<Movie> movies = DatabaseHelper.getAllMovies();
            Set<String> genres = new HashSet<>();
            for (Movie movie : movies) {
                genres.add(movie.getGenre());
            }
            for (String genre : genres) {
                genreFilterCombo.addItem(genre);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load genres for filtering: " + e.getMessage());
        }

        // Restore selection if possible
        if (selected != null) {
            genreFilterCombo.setSelectedItem(selected);
        }
        isUpdatingGenres = false;
    }

    /**
     * Refreshes the movies grid view.
     */
    private void refreshMovies() {
        refreshMovies(null, "All Genres");
    }

    /**
     * Refreshes the movie cards grid dynamically with filtering.
     */
    private void refreshMovies(String query, String genreFilter) {
        moviesPanel.removeAll();
        try {
            List<Movie> movies = DatabaseHelper.getAllMovies();
            for (Movie movie : movies) {
                // Apply Text Filter
                if (query != null && !query.trim().isEmpty()) {
                    if (!movie.getTitle().toLowerCase().contains(query.toLowerCase().trim())) {
                        continue;
                    }
                }

                // Apply Genre Filter
                if (genreFilter != null && !genreFilter.equals("All Genres")) {
                    if (!movie.getGenre().equalsIgnoreCase(genreFilter)) {
                        continue;
                    }
                }

                moviesPanel.add(createMovieCard(movie));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading movies: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        moviesPanel.revalidate();
        moviesPanel.repaint();
    }

    /**
     * Refreshes the bookings table.
     */
    private void refreshBookings() {
        bookingsTableModel.setRowCount(0);
        try {
            List<Ticket> bookings = DatabaseHelper.getAllBookings();
            for (Ticket ticket : bookings) {
                bookingsTableModel.addRow(new Object[]{
                        ticket.getBookingId(),
                        ticket.getCustomerName(),
                        ticket.getMovie().getTitle(),
                        ticket.getTicketType(),
                        ticket.getTicketsCount(),
                        "Rs. " + currencyFormat.format(ticket.calculateTotal()),
                        "Success"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Helper to create a single styled movie card Panel.
     */
    private JPanel createMovieCard(Movie movie) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(45, 45, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 85), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        // Details Panel
        JPanel detailsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        detailsPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(movie.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel genreLabel = new JLabel("Genre: " + movie.getGenre());
        genreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        genreLabel.setForeground(Color.LIGHT_GRAY);

        JLabel priceLabel = new JLabel("Base Price: Rs. " + currencyFormat.format(movie.getPrice()));
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        priceLabel.setForeground(new Color(255, 204, 102));

        JLabel seatsLabel = new JLabel("Available Seats: " + movie.getAvailableSeats());
        seatsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (movie.getAvailableSeats() > 10) {
            seatsLabel.setForeground(new Color(153, 255, 153));
        } else if (movie.getAvailableSeats() > 0) {
            seatsLabel.setForeground(new Color(255, 204, 153));
        } else {
            seatsLabel.setForeground(new Color(255, 102, 102));
            seatsLabel.setText("SOLD OUT");
        }

        detailsPanel.add(titleLabel);
        detailsPanel.add(genreLabel);
        detailsPanel.add(priceLabel);
        detailsPanel.add(seatsLabel);

        card.add(detailsPanel, BorderLayout.CENTER);

        // Book Button
        JButton bookBtn = new JButton(movie.getAvailableSeats() > 0 ? "Book Tickets" : "Sold Out");
        bookBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bookBtn.setFocusPainted(false);
        if (movie.getAvailableSeats() > 0) {
            bookBtn.setBackground(new Color(255, 102, 102));
            bookBtn.setForeground(Color.WHITE);
            bookBtn.addActionListener(e -> showBookingDialog(movie));
        } else {
            bookBtn.setBackground(new Color(80, 80, 80));
            bookBtn.setForeground(Color.LIGHT_GRAY);
            bookBtn.setEnabled(false);
        }

        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setOpaque(false);
        btnWrapper.setBorder(new EmptyBorder(10, 0, 0, 0));
        btnWrapper.add(bookBtn, BorderLayout.CENTER);
        card.add(btnWrapper, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Action to cancel a booking.
     */
    private void cancelBookingAction() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table to cancel.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) bookingsTableModel.getValueAt(selectedRow, 0);
        String customerName = (String) bookingsTableModel.getValueAt(selectedRow, 1);
        String movieTitle = (String) bookingsTableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to cancel the booking for:\nCustomer: " + customerName + "\nMovie: " + movieTitle + "?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseHelper.cancelBooking(bookingId);
                JOptionPane.showMessageDialog(this, "Booking successfully cancelled. Seats refunded.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                refreshMovies();
                refreshBookings();
                refreshAdminMoviesTable(); // Seats capacity will update in Admin Panel
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error cancelling booking: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Action to display the visual ticket / receipt.
     */
    private void viewReceiptAction() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a booking from the table to view the receipt.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookingId = (int) bookingsTableModel.getValueAt(selectedRow, 0);
        try {
            List<Ticket> bookings = DatabaseHelper.getAllBookings();
            Ticket selectedTicket = null;
            for (Ticket ticket : bookings) {
                if (ticket.getBookingId() == bookingId) {
                    selectedTicket = ticket;
                    break;
                }
            }

            if (selectedTicket != null) {
                showReceiptDialog(selectedTicket);
            } else {
                JOptionPane.showMessageDialog(this, "Booking data not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching booking details: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Renders a highly styled dialog simulating a movie booking receipt.
     */
    private void showReceiptDialog(Ticket ticket) {
        JDialog receiptDialog = new JDialog(this, "CineBook - Booking Receipt", true);
        receiptDialog.setSize(420, 560);
        receiptDialog.setLocationRelativeTo(this);
        receiptDialog.setLayout(new BorderLayout());

        JPanel receiptPanel = new JPanel();
        receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
        receiptPanel.setBackground(new Color(250, 250, 250)); // Light background for paper ticket aesthetic
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // Styling helpers for black text on light background
        Font titleFont = new Font("Courier New", Font.BOLD, 22);
        Font sectionFont = new Font("Courier New", Font.BOLD, 14);
        Font bodyFont = new Font("Courier New", Font.PLAIN, 13);
        Color darkText = new Color(20, 20, 20);

        JLabel headerLabel = new JLabel("⭐ CINEBOOK TICKETING ⭐");
        headerLabel.setFont(titleFont);
        headerLabel.setForeground(darkText);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(headerLabel);

        receiptPanel.add(Box.createVerticalStrut(10));
        
        JLabel lineLabel1 = new JLabel("================================");
        lineLabel1.setFont(bodyFont);
        lineLabel1.setForeground(darkText);
        lineLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(lineLabel1);

        receiptPanel.add(Box.createVerticalStrut(15));

        // Details Panel
        JPanel details = new JPanel(new GridLayout(6, 2, 5, 10));
        details.setOpaque(false);

        String[][] receiptData = {
                {"Booking ID:", String.format("#%05d", ticket.getBookingId())},
                {"Customer Name:", ticket.getCustomerName()},
                {"Movie:", ticket.getMovie().getTitle()},
                {"Ticket Type:", ticket.getTicketType() + (ticket instanceof VIPTicket ? " (+Rs. 150 VIP Chg)" : " (Standard)")},
                {"Seats Booked:", String.valueOf(ticket.getTicketsCount())},
                {"Price/Ticket:", "Rs. " + currencyFormat.format(ticket.getPricePerTicket())}
        };

        for (String[] row : receiptData) {
            JLabel key = new JLabel(row[0]);
            key.setFont(sectionFont);
            key.setForeground(darkText);
            JLabel val = new JLabel(row[1]);
            val.setFont(bodyFont);
            val.setForeground(darkText);
            details.add(key);
            details.add(val);
        }

        receiptPanel.add(details);
        receiptPanel.add(Box.createVerticalStrut(15));

        JLabel lineLabel2 = new JLabel("================================");
        lineLabel2.setFont(bodyFont);
        lineLabel2.setForeground(darkText);
        lineLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(lineLabel2);

        receiptPanel.add(Box.createVerticalStrut(15));

        // Total Summary
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setOpaque(false);
        JLabel totalKey = new JLabel("TOTAL AMOUNT:");
        totalKey.setFont(new Font("Courier New", Font.BOLD, 16));
        totalKey.setForeground(darkText);
        JLabel totalVal = new JLabel("Rs. " + currencyFormat.format(ticket.calculateTotal()));
        totalVal.setFont(new Font("Courier New", Font.BOLD, 18));
        totalVal.setForeground(new Color(204, 51, 51)); // High contrast price

        totalPanel.add(totalKey, BorderLayout.WEST);
        totalPanel.add(totalVal, BorderLayout.EAST);
        receiptPanel.add(totalPanel);

        receiptPanel.add(Box.createVerticalStrut(20));

        JLabel thankYouLabel = new JLabel("Enjoy the movie!");
        thankYouLabel.setFont(new Font("Courier New", Font.ITALIC, 14));
        thankYouLabel.setForeground(darkText);
        thankYouLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(thankYouLabel);

        JLabel noteLabel = new JLabel("Please present this receipt at gate.");
        noteLabel.setFont(new Font("Courier New", Font.PLAIN, 10));
        noteLabel.setForeground(Color.GRAY);
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        receiptPanel.add(noteLabel);

        receiptDialog.add(receiptPanel, BorderLayout.CENTER);

        // Actions Panel (Save & Close)
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        actionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton saveBtn = new JButton("Save Receipt to File");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setBackground(new Color(0, 102, 204));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            // Build text receipt representation
            StringBuilder sb = new StringBuilder();
            sb.append("****************************************\n");
            sb.append("         CINEBOOK TICKET RECEIPT        \n");
            sb.append("****************************************\n");
            sb.append(String.format("Booking ID:     #%05d\n", ticket.getBookingId()));
            sb.append(String.format("Customer Name:  %s\n", ticket.getCustomerName()));
            sb.append(String.format("Movie Selected: %s\n", ticket.getMovie().getTitle()));
            sb.append(String.format("Ticket Type:    %s\n", ticket.getTicketType()));
            sb.append(String.format("Seats Booked:   %d\n", ticket.getTicketsCount()));
            sb.append(String.format("Price/Ticket:   Rs. %s\n", currencyFormat.format(ticket.getPricePerTicket())));
            sb.append("----------------------------------------\n");
            sb.append(String.format("TOTAL PAID:     Rs. %s\n", currencyFormat.format(ticket.calculateTotal())));
            sb.append("----------------------------------------\n");
            sb.append("Enjoy your movie! Thank you for booking.\n");
            sb.append("****************************************\n");

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Receipt");
            fileChooser.setSelectedFile(new File("Receipt_Booking_" + ticket.getBookingId() + ".txt"));
            int userSelection = fileChooser.showSaveDialog(receiptDialog);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try (FileWriter fw = new FileWriter(fileToSave)) {
                    fw.write(sb.toString());
                    JOptionPane.showMessageDialog(receiptDialog, "Receipt saved successfully to:\n" + fileToSave.getAbsolutePath(), "Receipt Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(receiptDialog, "Failed to save file:\n" + ex.getMessage(), "Write Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        actionPanel.add(saveBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.addActionListener(e -> receiptDialog.dispose());
        actionPanel.add(closeBtn);

        receiptDialog.add(actionPanel, BorderLayout.SOUTH);
        receiptDialog.setVisible(true);
    }

    /**
     * Prompts the modal dialog for booking details.
     */
    private void showBookingDialog(Movie movie) {
        JDialog bookingDialog = new JDialog(this, "Book Tickets for " + movie.getTitle(), true);
        bookingDialog.setSize(450, 420);
        bookingDialog.setLocationRelativeTo(this);
        bookingDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Header Label
        JLabel headerLabel = new JLabel("Ticket Booking Form");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerLabel.setForeground(new Color(255, 102, 102));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(headerLabel, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Movie Info
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Movie:"), gbc);
        gbc.gridx = 1;
        JLabel movieName = new JLabel(movie.getTitle() + " (" + movie.getGenre() + ")");
        movieName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(movieName, gbc);

        // Customer Name Input
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Customer Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        formPanel.add(nameField, gbc);

        // Seat Count Selection
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Number of Seats:"), gbc);
        gbc.gridx = 1;
        SpinnerModel spinnerModel = new SpinnerNumberModel(1, 1, movie.getAvailableSeats(), 1);
        JSpinner seatsSpinner = new JSpinner(spinnerModel);
        formPanel.add(seatsSpinner, gbc);

        // Ticket Type Selection
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Seat Class:"), gbc);
        gbc.gridx = 1;
        String[] classes = {"Standard (Regular)", "VIP Class (Rs. 150 surcharge)"};
        JComboBox<String> typeComboBox = new JComboBox<>(classes);
        formPanel.add(typeComboBox, gbc);

        // Live Total Amount Preview
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Estimated Cost:"), gbc);
        gbc.gridx = 1;
        JLabel liveTotalLabel = new JLabel("Rs. " + currencyFormat.format(movie.getPrice()));
        liveTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        liveTotalLabel.setForeground(new Color(255, 204, 102));
        formPanel.add(liveTotalLabel, gbc);

        // Setup real-time calculations
        Runnable updateCost = () -> {
            try {
                int count = (int) seatsSpinner.getValue();
                boolean isVip = typeComboBox.getSelectedIndex() == 1;
                double unitPrice = movie.getPrice();
                double total = isVip ? count * (unitPrice + 150.0) : count * unitPrice;
                liveTotalLabel.setText("Rs. " + currencyFormat.format(total));
            } catch (Exception ex) {
                // Ignore spinner parsing issues
            }
        };

        seatsSpinner.addChangeListener(e -> updateCost.run());
        typeComboBox.addActionListener(e -> updateCost.run());

        bookingDialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> bookingDialog.dispose());
        actionPanel.add(cancelBtn);

        JButton confirmBtn = new JButton("Confirm Booking");
        confirmBtn.setBackground(new Color(255, 102, 102));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        confirmBtn.addActionListener(e -> {
            // Form validation
            String customerName = nameField.getText().trim();
            if (customerName.isEmpty()) {
                JOptionPane.showMessageDialog(bookingDialog, "Please enter the customer's name.", "Input Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int seatsRequested;
            try {
                seatsRequested = (int) seatsSpinner.getValue();
                if (seatsRequested <= 0) {
                    throw new IllegalArgumentException("Tickets count must be greater than zero.");
                }
                if (seatsRequested > movie.getAvailableSeats()) {
                    throw new IllegalArgumentException("Requested seats exceed available capacity.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(bookingDialog, "Invalid number of seats: " + ex.getMessage(), "Input Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean isVip = typeComboBox.getSelectedIndex() == 1;

            Ticket ticket;
            if (isVip) {
                ticket = new VIPTicket(0, customerName, movie, seatsRequested, movie.getPrice());
            } else {
                ticket = new RegularTicket(0, customerName, movie, seatsRequested, movie.getPrice());
            }

            // Process via JDBC Helper
            try {
                DatabaseHelper.bookTicket(ticket);
                JOptionPane.showMessageDialog(
                        MovieBookingSystem.this,
                        "Tickets booked successfully!\nBooking ID: #" + String.format("%05d", ticket.getBookingId()) +
                                "\nTotal Cost: Rs. " + currencyFormat.format(ticket.calculateTotal()),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
                bookingDialog.dispose();
                refreshMovies();
                refreshBookings();
                refreshAdminMoviesTable(); // Seats capacity will update in Admin Panel
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(bookingDialog, "Database transaction failed:\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        actionPanel.add(confirmBtn);

        bookingDialog.add(actionPanel, BorderLayout.SOUTH);
        bookingDialog.setVisible(true);
    }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MovieBookingSystem app = new MovieBookingSystem();
            app.setVisible(true);
        });
    }
}