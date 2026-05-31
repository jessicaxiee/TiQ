import java.sql.*;
import java.util.Scanner;
import java.time.*;
import java.time.format.*;
import java.sql.Timestamp;

public class FanMenu {

    static Connection con = null;
    static Scanner scanner;

    // Inject shared connection + scanner
    public static void init(Connection connection, Scanner sharedScanner) {
        con = connection;
        scanner = sharedScanner;
    }

    public static void addArtistToLikes(String fanEmail)
    {
        System.out.println("\n--- Add Artist to Likes ---");

        try {

            System.out.print("Artist name: ");
            String artistName = scanner.nextLine().trim();

            // Show all matching artists by name
            String findArtistSQL =
                "SELECT license_no, artist_name " +
                "FROM Artist " +
                "WHERE artist_name = ?";
            PreparedStatement psFindArtist = con.prepareStatement(findArtistSQL);
            psFindArtist.setString(1, artistName);
            ResultSet rsArtist = psFindArtist.executeQuery();

            boolean foundArtist = false;
            System.out.println("\nMatching artists:");
            while (rsArtist.next()) {
                foundArtist = true;
                System.out.println(
                    "License: " + rsArtist.getString("license_no") +
                    " | Artist: " + rsArtist.getString("artist_name")
                );
            }
            rsArtist.close();
            psFindArtist.close();

            if (!foundArtist) {
                System.out.println("Error: No artist found with that name.\n");
                return;
            }

            System.out.print("Enter management license number: ");
            String licenseNo = scanner.nextLine().trim();

            // Verify exact artist exists
            String checkArtistSQL =
                "SELECT 1 FROM Artist WHERE license_no = ? AND artist_name = ?";
            PreparedStatement psCheckArtist = con.prepareStatement(checkArtistSQL);
            psCheckArtist.setString(1, licenseNo);
            psCheckArtist.setString(2, artistName);
            ResultSet rsCheckArtist = psCheckArtist.executeQuery();

            if (!rsCheckArtist.next()) {
                System.out.println("Error: No artist found with that license number and name.\n");
                rsCheckArtist.close();
                psCheckArtist.close();
                return;
            }
            rsCheckArtist.close();
            psCheckArtist.close();

            // Check if fan already likes this artist
            String checkLikeSQL =
                "SELECT 1 FROM Likes WHERE fan_email = ? AND license_no = ? AND artist_name = ?";
            PreparedStatement psCheckLike = con.prepareStatement(checkLikeSQL);
            psCheckLike.setString(1, fanEmail);
            psCheckLike.setString(2, licenseNo);
            psCheckLike.setString(3, artistName);
            ResultSet rsLike = psCheckLike.executeQuery();

            if (rsLike.next()) {
                System.out.println("Fan already likes this artist.\n");
                rsLike.close();
                psCheckLike.close();
                return;
            }
            rsLike.close();
            psCheckLike.close();

            // Insert into Likes
            String insertLikeSQL =
                "INSERT INTO Likes (fan_email, license_no, artist_name) VALUES (?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(insertLikeSQL);
            psInsert.setString(1, fanEmail);
            psInsert.setString(2, licenseNo);
            psInsert.setString(3, artistName);
            psInsert.executeUpdate();
            psInsert.close();

            System.out.println("Artist added to Likes successfully.\n");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            System.out.println();
        }
    }

    public static void viewLikedArtists(String fanEmail)
    {
        System.out.println("\n--- View Liked Artists ---");

        try {

            String querySQL =
                "SELECT L.license_no, L.artist_name " +
                "FROM Likes L " +
                "WHERE L.fan_email = ? " +
                "ORDER BY L.artist_name, L.license_no";

            PreparedStatement ps = con.prepareStatement(querySQL);
            ps.setString(1, fanEmail);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.println("\nLiked artists:");
            while (rs.next()) {
                found = true;
                System.out.println(
                    "License: " + rs.getString("license_no") +
                    " | Artist: " + rs.getString("artist_name")
                );
            }

            if (!found) {
                System.out.println("This fan has no liked artists.");
            }

            System.out.println();
            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            System.out.println();
        }
    }

    public static void viewEvents(String fanEmail)
    {
        System.out.println("\n--- View Events ---");

        try {

            System.out.print("Artist name (leave blank for all): ");
            String artistName = scanner.nextLine().trim();

            System.out.print("Minimum ticket price (leave blank for no minimum): ");
            String minPriceInput = scanner.nextLine().trim();

            System.out.print("Maximum ticket price (leave blank for no maximum): ");
            String maxPriceInput = scanner.nextLine().trim();

            Double minPrice = null;
            Double maxPrice = null;

            if (!minPriceInput.isEmpty()) {
                try {
                    minPrice = Double.parseDouble(minPriceInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid minimum price.\n");
                    return;
                }
            }

            if (!maxPriceInput.isEmpty()) {
                try {
                    maxPrice = Double.parseDouble(maxPriceInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid maximum price.\n");
                    return;
                }
            }

            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                System.out.println("Error: minimum price cannot be greater than maximum price.\n");
                return;
            }

            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT DISTINCT " +
                "    T.license_no, " +
                "    T.artist_name, " +
                "    T.tour_name, " +
                "    T.event_date, " +
                "    V.name AS venue_name, " +
                "    T.venue_address, " +
                "    MIN(T.price) OVER (PARTITION BY T.license_no, T.artist_name, T.tour_name, T.event_date, T.venue_address) AS min_event_price " +
                "FROM Ticket T " +
                "JOIN Venue V ON T.venue_address = V.address " +
                "WHERE 1=1 "
            );

            if (!artistName.isEmpty()) {
                sql.append("AND T.artist_name = ? ");
            }
            if (minPrice != null) {
                sql.append("AND T.price >= ? ");
            }
            if (maxPrice != null) {
                sql.append("AND T.price <= ? ");
            }

            sql.append("ORDER BY T.event_date, T.artist_name, T.tour_name");

            PreparedStatement ps = con.prepareStatement(sql.toString());

            int paramIndex = 1;

            if (!artistName.isEmpty()) {
                ps.setString(paramIndex++, artistName);
            }
            if (minPrice != null) {
                ps.setDouble(paramIndex++, minPrice);
            }
            if (maxPrice != null) {
                ps.setDouble(paramIndex++, maxPrice);
            }

            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.println("\nAvailable Tour Events:");
            while (rs.next()) {
                found = true;
                System.out.println(
                    "License: " + rs.getString("license_no") +
                    " | Artist: " + rs.getString("artist_name") +
                    " | Tour: " + rs.getString("tour_name") +
                    " | Date: " + rs.getDate("event_date") +
                    " | Venue: " + rs.getString("venue_name") +
                    " | Address: " + rs.getString("venue_address") +
                    " | Starting Price: $" + rs.getDouble("min_event_price")
                );
            }

            if (!found) {
                System.out.println("No matching tour events found.");
            }

            System.out.println();
            rs.close();
            ps.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            System.out.println();
        }
    }

    public static void buyTicket(String fanEmail){
        System.out.println("\n--- Buy Ticket ---");

        try {
            // filter by Artist + Tour
            Long validLicenseNum = null;
            String licenseNum = null;
            while(validLicenseNum == null){
                System.out.print("Artist Management license number: ");
                licenseNum = scanner.nextLine().trim();

                if(licenseNum.isEmpty()){
                    System.out.println("Management license number cannot be left empty. Please enter a Management license number.");
                    continue;
                }
                try {
                    validLicenseNum = Long.parseLong(licenseNum);
                    
                    if(validLicenseNum < 0){
                        validLicenseNum = null;
                        System.out.println("too small");
                        throw new NumberFormatException("Negative integer.");
                        
                    }
                } catch (NumberFormatException e){
                    System.out.println("Management license number must be a valid positive integer. Please enter a valid Management license number.");
                    continue;
                }
                
            }

            // input validation for artist/tour
            System.out.print("Artist name: ");
            String artistName = scanner.nextLine().trim();
            while(artistName.isEmpty()){
                System.out.println("Artist name cannot be left empty. Please enter an artist name.");
                System.out.print("Artist name: ");
                artistName = scanner.nextLine().trim();
            }

            System.out.print("Tour name: ");
            String tourName = scanner.nextLine().trim();
            while(tourName.isEmpty()){
                System.out.println("Tour name cannot be left empty. Please enter a tour name.");
                System.out.print("Tour name: ");
                tourName = scanner.nextLine().trim();
            }
            
            // removes tickets that have been purchased from selection
            StringBuilder getTourSQL = new StringBuilder();
            getTourSQL.append(
                "SELECT DISTINCT T.license_no, T.artist_name, T.tour_name, T.event_date, V.name AS venue_name, T.venue_address, T.section, T.seat_no, T.price, T.tier " +
                "FROM Ticket T JOIN Venue V ON T.venue_address = V.address " +
                "WHERE NOT EXISTS (" +
                    "SELECT 1 " +
                    "FROM Buys_Ticket B " +
                    "WHERE T.license_no = B.license_no "+
                    "AND T.artist_name = B.artist_name "+
                    "AND T.tour_name = B.tour_name "+
                    "AND T.event_date = B.event_date "+
                    "AND T.venue_address = B.venue_address "+
                    "AND T.section = B.section "+
                    "AND T.seat_no = B.seat_no "+
                ") " +
                "AND T.artist_name = ? AND T.tour_name = ? AND T.license_no = ?" 
                
            );

            // avoids SQL injections
            PreparedStatement getTourPS = con.prepareStatement(getTourSQL.toString());
            getTourPS.setString(1, artistName);
            getTourPS.setString(2, tourName);
            getTourPS.setLong(3, validLicenseNum);

            ResultSet getTourRS = getTourPS.executeQuery();

            // print SQL output
            if (getTourRS.next() == false){
                System.out.println("No events found for " + artistName + ". Try again?");
                return;
            } else {
                System.out.println("Tickets for upcoming events for "+ artistName + ": ");
                do {
                    System.out.println(
                        " | Artist: " + getTourRS.getString("artist_name") +
                        " | Tour: " + getTourRS.getString("tour_name") +
                        " | Date: " + getTourRS.getDate("event_date") +
                        " | Venue: " + getTourRS.getString("venue_name") +
                        " | Address: " + getTourRS.getString("venue_address")  +
                        " | Section: " + getTourRS.getString("section") + 
                        " | Seat: " + getTourRS.getString("seat_no") + 
                        " | Price: " + getTourRS.getString("price") + 
                        " | Tier: " + getTourRS.getString("tier")
                    );
                }
                while (getTourRS.next());
            }
            getTourPS.close();
            getTourRS.close();

            // filter over Artist/Tour/Venue/Date 
            System.out.print("Venue name: ");
            String venueName = scanner.nextLine().trim();
            while(venueName.isEmpty()){
                System.out.println("Venue cannot be left empty. Please enter a venue name.");
                System.out.print("Venue name: ");
                venueName = scanner.nextLine().trim();
            }


            // date filtering
            DateTimeFormatter dformat = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
            LocalDate validDate = null;
            String eventDate = null;
            while (validDate == null) {
                System.out.print("Date (YYYY-MM-DD): ");
                eventDate = scanner.nextLine().trim();

                if (eventDate.isEmpty()) {
                    System.out.println("Date cannot be left empty.");
                    continue; // Restart the loop to ask again
                }

                try {
                    validDate = LocalDate.parse(eventDate, dformat);
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date or format. Please use YYYY-MM-DD.");
                }
            }

            // filter further over venue + date
            // full statement included for easy readability
            StringBuilder getVenueSQL = new StringBuilder();
            getVenueSQL.append(
                "SELECT DISTINCT T.artist_name, T.tour_name, T.event_date, V.name AS venue_name, T.venue_address, T.section, T.seat_no, T.price, T.tier " +
                "FROM Ticket T JOIN Venue V ON T.venue_address = V.address " +
                "WHERE NOT EXISTS (" +
                    "SELECT 1 " +
                    "FROM Buys_Ticket B " +
                    "WHERE T.license_no = B.license_no "+
                    "AND T.artist_name = B.artist_name "+
                    "AND T.tour_name = B.tour_name "+
                    "AND T.event_date = B.event_date "+
                    "AND T.venue_address = B.venue_address "+
                    "AND T.section = B.section "+
                    "AND T.seat_no = B.seat_no "+
                ") " +
                "AND T.artist_name = ? AND T.tour_name = ? AND V.name = ? AND T.event_date = ? AND T.license_no = ?"
            );
            
            // avoids SQL injections
            PreparedStatement getVenuePS = con.prepareStatement(getVenueSQL.toString());
            getVenuePS.setString(1, artistName);
            getVenuePS.setString(2, tourName);
            getVenuePS.setString(3, venueName);
            getVenuePS.setDate(4, java.sql.Date.valueOf(eventDate));
            getVenuePS.setLong(5, validLicenseNum);

            ResultSet getVenueRS = getVenuePS.executeQuery();

            if (getVenueRS.next() == false){
                System.out.println("No tickets found for " + artistName + " at "+ venueName + " on " + eventDate +". Try again?");
                return;
            } else {
                System.out.println("Tickets for upcoming events for "+ artistName + " at "+ venueName + " on " + eventDate +": ");
                do {
                    System.out.println(
                        " | Artist: " + getVenueRS.getString("artist_name") +
                        " | Tour: " + getVenueRS.getString("tour_name") +
                        " | Date: " + getVenueRS.getDate("event_date") +
                        " | Venue: " + getVenueRS.getString("venue_name") +
                        " | Address: " + getVenueRS.getString("venue_address")  +
                        " | Section: " + getVenueRS.getString("section") + 
                        " | Seat: " + getVenueRS.getString("seat_no") + 
                        " | Price: " + getVenueRS.getString("price") + 
                        " | Tier: " + getVenueRS.getString("tier")
                    );
                }
                while (getVenueRS.next());
            }
            getVenuePS.close();
            getVenueRS.close();
            
            // Select section + seat
            String sectionNum = null;

            Integer validSectionNum = null;
            while(validSectionNum == null){
                System.out.print("Section number: ");
                sectionNum = scanner.nextLine().trim();

                if(sectionNum.isEmpty()){
                    System.out.println("Section number cannot be left empty. Please enter a section number.");
                    continue;
                }
                // input validation
                try {
                    validSectionNum = Integer.parseInt(sectionNum);
                    if(validSectionNum < 0){
                        validSectionNum = null;
                        throw new NumberFormatException("Negative integer.");
                    }
                } catch (NumberFormatException e){
                    System.out.println("Section number must be a valid positive integer. Please enter a valid section number.");
                    continue;
                }
                
            }

            String seatNum = null;
            Integer validSeatNum = null;
            while(validSeatNum == null){
                System.out.print("Seat number: ");
                seatNum = scanner.nextLine().trim();
                if(seatNum.isEmpty()){
                    System.out.println("Seat number cannot be left empty. Please enter a seat number.");
                    continue;
                }
                try {
                    validSeatNum = Integer.parseInt(seatNum);
                    if(validSeatNum < 0){
                        validSeatNum = null;
                        throw new NumberFormatException("Negative integer.");
                    }
                } catch (NumberFormatException e){
                    System.out.println("Seat number must be a valid positive integer. Please enter a valid seat number or leave it blank.");
                    continue;
                }
            }

            StringBuilder getSectionSQL = new StringBuilder();
            getSectionSQL.append(
                "SELECT DISTINCT T.artist_name, T.tour_name, T.event_date, V.name AS venue_name, T.venue_address, T.section, T.seat_no, T.price, T.tier " +
                "FROM Ticket T JOIN Venue V ON T.venue_address = V.address " +
                "WHERE NOT EXISTS (" +
                    "SELECT 1 " +
                    "FROM Buys_Ticket B " +
                    "WHERE T.license_no = B.license_no "+
                    "AND T.artist_name = B.artist_name "+
                    "AND T.tour_name = B.tour_name "+
                    "AND T.event_date = B.event_date "+
                    "AND T.venue_address = B.venue_address "+
                    "AND T.section = B.section "+
                    "AND T.seat_no = B.seat_no "+
                ") " +
                "AND T.artist_name = ? AND T.tour_name = ? AND V.name = ? AND T.event_date = ? AND T.section = ? AND T.license_no = ? AND T.seat_no = ?"
            );

            // avoids SQL injections
            PreparedStatement getSectionPS = con.prepareStatement(getSectionSQL.toString());
            getSectionPS.setString(1, artistName);
            getSectionPS.setString(2, tourName);
            getSectionPS.setString(3, venueName);
            getSectionPS.setDate(4, java.sql.Date.valueOf(eventDate));
            getSectionPS.setInt(5, validSectionNum);
            getSectionPS.setLong(6, validLicenseNum);
            getSectionPS.setInt(7, validSeatNum);

            ResultSet getSectionRS = getSectionPS.executeQuery();

            String venueAddress = null;
            if (getSectionRS.next() == false){
                System.out.println("No tickets found for section "+ sectionNum + " and seat "+ seatNum + " for " + artistName + " at "+ venueName + " on " + eventDate +". Try again?");
                return;
            } else {
                System.out.println("Ticket for section "+ sectionNum + ", seat "+ seatNum +" for "+ artistName + " at "+ venueName + " on " + eventDate +": ");
                venueAddress = getSectionRS.getString("venue_address");
                do {
                    System.out.println(
                        " | Artist: " + getSectionRS.getString("artist_name") +
                        " | Tour: " + getSectionRS.getString("tour_name") +
                        " | Date: " + getSectionRS.getDate("event_date") +
                        " | Venue: " + getSectionRS.getString("venue_name") +
                        " | Address: " + getSectionRS.getString("venue_address")  +
                        " | Section: " + getSectionRS.getString("section") + 
                        " | Seat: " + getSectionRS.getString("seat_no") + 
                        " | Price: " + getSectionRS.getString("price") + 
                        " | Tier: " + getSectionRS.getString("tier")
                    );
                }
                while (getSectionRS.next());
            }

            getSectionRS.close();
            getSectionPS.close();

            String confirm = null;
            String validConfirm = null;
            String payMethod = null;
            while(validConfirm == null){
                System.out.print("Would you like to purchase this ticket? (y/n): ");
                confirm = scanner.nextLine().trim();
                if(confirm.toLowerCase().equals("y") || confirm.toLowerCase().equals("yes")){
                    validConfirm = "true";
                    continue;
                } else if(confirm.toLowerCase().equals("n") || confirm.toLowerCase().equals("no")){
                    System.out.println("Ticket purchase cancelled. Returning to main menu.");
                    return;
                } else {
                    System.out.println("Invalid answer. Please answer with 'y' or 'n'.");
                    continue;
                }
                
            }
            System.out.print("How would you like to pay? (Credit/Debit): ");
            payMethod = scanner.nextLine().trim();
            if(!payMethod.toLowerCase().equals("credit") && !payMethod.toLowerCase().equals("debit")){
                System.out.println("Ticket purchase cancelled. Returning to main menu.");
                return;
            }
            if(payMethod.toLowerCase().equals("credit")){
                payMethod = "Credit";
            } else {
                payMethod = "Debit";
            }

            StringBuilder buyTicketSQL = new StringBuilder();
            buyTicketSQL.append(
                "INSERT INTO Buys_Ticket " +
                "(fan_email, employee_id, license_no, artist_name, tour_name, event_date, venue_address, section, seat_no, purchase_time, purchase_method)" +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)"
            );

            // avoids SQL injections
            PreparedStatement buyTicketPS = con.prepareStatement(buyTicketSQL.toString());
            buyTicketPS.setString(1, fanEmail);
            buyTicketPS.setInt(2, 0);
            buyTicketPS.setLong(3, validLicenseNum);
            buyTicketPS.setString(4, artistName);
            buyTicketPS.setString(5, tourName);
            buyTicketPS.setDate(6, java.sql.Date.valueOf(eventDate));
            buyTicketPS.setString(7, venueAddress);
            buyTicketPS.setInt(8, validSectionNum);
            buyTicketPS.setInt(9, validSeatNum);

            Timestamp currentSqlTimestamp = new Timestamp(System.currentTimeMillis());
            buyTicketPS.setTimestamp(10,currentSqlTimestamp);
            buyTicketPS.setString(11, payMethod);
            buyTicketPS.executeUpdate();
            buyTicketPS.close();

            System.out.println("Purchase successful of " + artistName + ": " + tourName + " on " + eventDate + " at " + venueName + ", section " + sectionNum + " seat " + seatNum);
                
            buyTicketPS.close();
            

        } catch (SQLException e) {
            // catches any DB2 errors
            System.out.println("Database error: " + e.getMessage());
            System.out.println("SQLSTATE: " + e.getSQLState() + "  Code: " + e.getErrorCode());
            System.out.println();
        }
    }

    public static void viewTickets(String fanEmail) throws SQLException {
        StringBuilder getTicketSQL = new StringBuilder();
        getTicketSQL.append(
            "SELECT BT.license_no, BT.artist_name, BT.tour_name, BT.event_date, V.name AS venue_name, BT.venue_address, BT.section, BT.seat_no, T.price, T.tier, BT.purchase_time, BT.purchase_method " +
            "FROM Buys_Ticket BT JOIN Ticket T ON BT.license_no = T.license_no AND BT.artist_name = T.artist_name AND BT.tour_name = T.tour_name AND BT.event_date = T.event_date AND BT.venue_address = T.venue_address AND BT.section = T.section AND BT.seat_no = T.seat_no " +
            "JOIN Venue V ON BT.venue_address = V.address " +
            "WHERE BT.fan_email = ?"
        );
        PreparedStatement getTicketPS = con.prepareStatement(getTicketSQL.toString());

        getTicketPS.setString(1, fanEmail);
        ResultSet getTicketRS = getTicketPS.executeQuery();

        
        if (getTicketRS.next() == false){
            System.out.println("No purchased tickets found. Returning to main menu.");
            return;
        } else {
            System.out.println("Your purchased tickets:");
            do {
                System.out.println(
                    " | License number: " + getTicketRS.getString("license_no") +
                    " | Artist: " + getTicketRS.getString("artist_name") +
                    " | Tour: " + getTicketRS.getString("tour_name") +
                    " | Date: " + getTicketRS.getDate("event_date") +
                    " | Venue: " + getTicketRS.getString("venue_name") +
                    " | Address: " + getTicketRS.getString("venue_address")  +
                    " | Section: " + getTicketRS.getString("section") + 
                    " | Seat: " + getTicketRS.getString("seat_no") + 
                    " | Price: " + getTicketRS.getString("price") + 
                    " | Tier: " + getTicketRS.getString("tier") +
                    " | Purchase Time: " + getTicketRS.getString("purchase_time") +
                    " | Purchase Method: " + getTicketRS.getString("purchase_method")
                );
            }
            while (getTicketRS.next());

            getTicketRS.close();
            getTicketPS.close();

        }
    
    }
}
