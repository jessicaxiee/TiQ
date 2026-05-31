import java.sql.*;
import java.util.Scanner;

public class EmployeeMenu {

    static Connection con = null;
    static Scanner scanner;

    // Called by main script to inject the shared connection and scanner
    public static void init(Connection connection, Scanner sharedScanner) {
        con = connection;
        scanner = sharedScanner;
    }

    // Add a new event
    // Inserts a row into Tour_Event, then into Concert or Fansign.
    static void addNewEvent(int employeeId) {
        System.out.println("--- Add a New Event ---");

        try {
            System.out.print("Tour name: ");
            String tourName = scanner.nextLine().trim();

            System.out.print("Event date (YYYY-MM-DD): ");
            Date eventDate;
            try {
                eventDate = Date.valueOf(scanner.nextLine().trim());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.\n");
                return;
            }

            System.out.print("Management license number: ");
            String licenseNo = scanner.nextLine().trim();

            System.out.print("Artist name: ");
            String artistName = scanner.nextLine().trim();

            // Verify the artist exists
            String checkArtist = "SELECT 1 FROM Artist WHERE license_no = ? AND artist_name = ?";
            PreparedStatement psCheck = con.prepareStatement(checkArtist);
            psCheck.setString(1, licenseNo);
            psCheck.setString(2, artistName);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                System.out.println("Error: No artist found with that license number and name. Event not added.\n");
                rsCheck.close();
                psCheck.close();
                con.rollback();
                return;
            }
            rsCheck.close();
            psCheck.close();

            // Insert into Tour_Event
            String insertTour = "INSERT INTO Tour_Event (tour_name, event_date, license_no, artist_name) "
                              + "VALUES (?, ?, ?, ?)";
            PreparedStatement psTour = con.prepareStatement(insertTour);
            psTour.setString(1, tourName);
            psTour.setDate(2, eventDate);
            psTour.setString(3, licenseNo);
            psTour.setString(4, artistName);
            psTour.executeUpdate();
            psTour.close();

            // Determine event subtype
            System.out.print("Event type — enter C for Concert, F for Fansign: ");
            String typeChoice = scanner.nextLine().trim().toUpperCase();

            if (typeChoice.equals("C")) {
                System.out.print("Setlist (comma-separated songs): ");
                String setlist = scanner.nextLine().trim();

                PreparedStatement psConcert = con.prepareStatement(
                    "INSERT INTO Concert (tour_name, event_date, license_no, artist_name, setlist) "
                  + "VALUES (?, ?, ?, ?, ?)");
                psConcert.setString(1, tourName);
                psConcert.setDate(2, eventDate);
                psConcert.setString(3, licenseNo);
                psConcert.setString(4, artistName);
                psConcert.setString(5, setlist);
                psConcert.executeUpdate();
                psConcert.close();

            } else if (typeChoice.equals("F")) {
                System.out.print("Interaction time (minutes): ");
                int interactionTime;
                try {
                    interactionTime = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid interaction time. Rolling back.\n");
                    con.rollback();
                    return;
                }

                PreparedStatement psFansign = con.prepareStatement(
                    "INSERT INTO Fansign (tour_name, event_date, license_no, artist_name, interaction_time) "
                  + "VALUES (?, ?, ?, ?, ?)");
                psFansign.setString(1, tourName);
                psFansign.setDate(2, eventDate);
                psFansign.setString(3, licenseNo);
                psFansign.setString(4, artistName);
                psFansign.setInt(5, interactionTime);
                psFansign.executeUpdate();
                psFansign.close();

            } else {
                System.out.println("Unknown event type. Rolling back.\n");
                con.rollback();
                return;
            }

            con.commit();
            System.out.println("Event added successfully!\n");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            try { con.rollback(); } catch (SQLException ex) { System.out.println("Rollback failed: " + ex.getMessage()); }
            System.out.println();
        }
    }

    // Cancel an event
    // Deletes all dependent rows (Buys_Ticket, Ticket, Listing, Concert/Fansign) before removing the Tour_Event row.
    static void cancelEvent(int employeeId) {
        System.out.println("--- Cancel an Event ---");

        try {
            System.out.print("Tour name: ");
            String tourName = scanner.nextLine().trim();

            System.out.print("Event date (YYYY-MM-DD): ");
            Date eventDate;
            try {
                eventDate = Date.valueOf(scanner.nextLine().trim());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.\n");
                return;
            }

            System.out.print("Management license number: ");
            String licenseNo = scanner.nextLine().trim();

            System.out.print("Artist name: ");
            String artistName = scanner.nextLine().trim();

            // Verify the event exists
            PreparedStatement psCheck = con.prepareStatement(
                "SELECT 1 FROM Tour_Event "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psCheck.setString(1, tourName);
            psCheck.setDate(2, eventDate);
            psCheck.setString(3, licenseNo);
            psCheck.setString(4, artistName);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                System.out.println("Error: No matching event found. Nothing was cancelled.\n");
                rsCheck.close();
                psCheck.close();
                con.rollback();
                return;
            }
            rsCheck.close();
            psCheck.close();

            // Delete in dependency order

            // 1. Buys_Ticket
            PreparedStatement psBuys = con.prepareStatement(
                "DELETE FROM Buys_Ticket "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psBuys.setString(1, tourName);
            psBuys.setDate(2, eventDate);
            psBuys.setString(3, licenseNo);
            psBuys.setString(4, artistName);
            int buysDeleted = psBuys.executeUpdate();
            psBuys.close();

            // 2. Ticket
            PreparedStatement psTicket = con.prepareStatement(
                "DELETE FROM Ticket "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psTicket.setString(1, tourName);
            psTicket.setDate(2, eventDate);
            psTicket.setString(3, licenseNo);
            psTicket.setString(4, artistName);
            int ticketsDeleted = psTicket.executeUpdate();
            psTicket.close();

            // 3. Listing
            PreparedStatement psListing = con.prepareStatement(
                "DELETE FROM Listing "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psListing.setString(1, tourName);
            psListing.setDate(2, eventDate);
            psListing.setString(3, licenseNo);
            psListing.setString(4, artistName);
            psListing.executeUpdate();
            psListing.close();

            // 4a. Concert
            PreparedStatement psConcert = con.prepareStatement(
                "DELETE FROM Concert "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psConcert.setString(1, tourName);
            psConcert.setDate(2, eventDate);
            psConcert.setString(3, licenseNo);
            psConcert.setString(4, artistName);
            psConcert.executeUpdate();
            psConcert.close();

            // 4b. Fansign
            PreparedStatement psFansign = con.prepareStatement(
                "DELETE FROM Fansign "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psFansign.setString(1, tourName);
            psFansign.setDate(2, eventDate);
            psFansign.setString(3, licenseNo);
            psFansign.setString(4, artistName);
            psFansign.executeUpdate();
            psFansign.close();

            // 5. Tour_Event
            PreparedStatement psTour = con.prepareStatement(
                "DELETE FROM Tour_Event "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
            psTour.setString(1, tourName);
            psTour.setDate(2, eventDate);
            psTour.setString(3, licenseNo);
            psTour.setString(4, artistName);
            psTour.executeUpdate();
            psTour.close();

            con.commit();
            System.out.println("Event cancelled successfully.");
            System.out.println("  Ticket purchases removed : " + buysDeleted);
            System.out.println("  Tickets removed          : " + ticketsDeleted + "\n");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            try { con.rollback(); } catch (SQLException ex) { System.out.println("Rollback failed: " + ex.getMessage()); }
            System.out.println();
        }
    }

    // Modify ticket prices
    // Shows a summary of current tickets for the event, then lets the employee update prices for all tickets, a specific tier, or a specific section.
    static void modifyPrices(int employeeId) {
        System.out.println("--- Modify Ticket Prices ---");

        Statement psSetContext = null;
        boolean contextSet = false;

        try {
            System.out.print("Tour name: ");
            String tourName = scanner.nextLine().trim();

            System.out.print("Event date (YYYY-MM-DD): ");
            Date eventDate;
            try {
                eventDate = Date.valueOf(scanner.nextLine().trim());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.\n");
                return;
            }

            System.out.print("Management license number: ");
            String licenseNo = scanner.nextLine().trim();

            System.out.print("Artist name: ");
            String artistName = scanner.nextLine().trim();

            // Show existing ticket breakdown
            PreparedStatement psShow = con.prepareStatement(
                "SELECT section, tier, price, COUNT(*) AS num_seats "
              + "FROM Ticket "
              + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ? "
              + "GROUP BY section, tier, price "
              + "ORDER BY section, tier");
            psShow.setString(1, tourName);
            psShow.setDate(2, eventDate);
            psShow.setString(3, licenseNo);
            psShow.setString(4, artistName);
            ResultSet rsShow = psShow.executeQuery();

            System.out.println("\nCurrent tickets for this event:");
            System.out.printf("  %-15s %-15s %-10s %-10s%n", "Section", "Tier", "Price", "Seats");
            System.out.println("  ----------------------------------------------------");
            boolean found = false;
            while (rsShow.next()) {
                found = true;
                System.out.printf("  %-15s %-15s %-10.2f %-10d%n",
                    rsShow.getString("section"),
                    rsShow.getString("tier"),
                    rsShow.getDouble("price"),
                    rsShow.getInt("num_seats"));
            }
            rsShow.close();
            psShow.close();

            if (!found) {
                System.out.println("  No tickets found for this event.\n");
                con.rollback();
                return;
            }

            // System.out.print("Employee ID making this change: ");
            // int employeeId;
            // try {
            //     employeeId = Integer.parseInt(scanner.nextLine().trim());
            // } catch (NumberFormatException e) {
            //     System.out.println("Invalid employee ID. No changes made.\n");
            //     con.rollback();
            //     return;
            // }

            // PreparedStatement psEmployee = con.prepareStatement(
            //     "SELECT 1 FROM Employee WHERE employee_id = ?");
            // psEmployee.setInt(1, employeeId);
            // ResultSet rsEmployee = psEmployee.executeQuery();
            // if (!rsEmployee.next()) {
            //     System.out.println("Employee ID not found. No changes made.\n");
            //     rsEmployee.close();
            //     psEmployee.close();
            //     con.rollback();
            //     return;
            // }
            // rsEmployee.close();
            // psEmployee.close();

            psSetContext = con.createStatement();
            psSetContext.executeUpdate("SET CURRENT_EMPLOYEE_ID = " + employeeId);
            contextSet = true;

            // Choose update scope
            System.out.println("\nUpdate prices for:");
            System.out.println("  T - A specific tier");
            System.out.println("  S - A specific section");
            System.out.println("  A - All tickets for this event");
            System.out.print("Choice: ");
            String filterChoice = scanner.nextLine().trim().toUpperCase();

            System.out.print("New price: ");
            double newPrice;
            try {
                newPrice = Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid price. No changes made.\n");
                con.rollback();
                return;
            }
            if (newPrice < 0) {
                System.out.println("Price cannot be negative. No changes made.\n");
                con.rollback();
                return;
            }

            PreparedStatement psUpdate;
            int rowsUpdated;

            if (filterChoice.equals("T")) {
                System.out.print("Tier name: ");
                String tier = scanner.nextLine().trim();
                psUpdate = con.prepareStatement(
                    "UPDATE Ticket SET price = ? "
                  + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ? AND tier = ?");
                psUpdate.setDouble(1, newPrice);
                psUpdate.setString(2, tourName);
                psUpdate.setDate(3, eventDate);
                psUpdate.setString(4, licenseNo);
                psUpdate.setString(5, artistName);
                psUpdate.setString(6, tier);
                rowsUpdated = psUpdate.executeUpdate();
                psUpdate.close();

            } else if (filterChoice.equals("S")) {
                System.out.print("Section name: ");
                String section = scanner.nextLine().trim();
                psUpdate = con.prepareStatement(
                    "UPDATE Ticket SET price = ? "
                  + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ? AND section = ?");
                psUpdate.setDouble(1, newPrice);
                psUpdate.setString(2, tourName);
                psUpdate.setDate(3, eventDate);
                psUpdate.setString(4, licenseNo);
                psUpdate.setString(5, artistName);
                psUpdate.setString(6, section);
                rowsUpdated = psUpdate.executeUpdate();
                psUpdate.close();

            } else if (filterChoice.equals("A")) {
                psUpdate = con.prepareStatement(
                    "UPDATE Ticket SET price = ? "
                  + "WHERE tour_name = ? AND event_date = ? AND license_no = ? AND artist_name = ?");
                psUpdate.setDouble(1, newPrice);
                psUpdate.setString(2, tourName);
                psUpdate.setDate(3, eventDate);
                psUpdate.setString(4, licenseNo);
                psUpdate.setString(5, artistName);
                rowsUpdated = psUpdate.executeUpdate();
                psUpdate.close();

            } else {
                System.out.println("Invalid filter choice. No changes made.\n");
                con.rollback();
                return;
            }

            con.commit();
            System.out.println("Price updated to " + String.format("%.2f", newPrice)
                             + ". " + rowsUpdated + " ticket(s) affected.\n");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            try { con.rollback(); } catch (SQLException ex) { System.out.println("Rollback failed: " + ex.getMessage()); }
            System.out.println();
        } finally {
            if (psSetContext != null) {
                if (contextSet) {
                    try {
                        psSetContext.executeUpdate("SET CURRENT_EMPLOYEE_ID = NULL");
                    } catch (SQLException ignored) {
                        // Ignore cleanup errors so they do not hide the root cause.
                    }
                }
                try {
                    psSetContext.close();
                } catch (SQLException ignored) {
                    // Ignore cleanup errors.
                }
            }
        }
    }
}
