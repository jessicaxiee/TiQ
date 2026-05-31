import java.sql.*;
import java.util.Scanner;

class simpleJDBC
{
    static Connection con;
    static Scanner in = new Scanner(System.in);

    public static void main(String[] args) throws SQLException
    {
        int sqlCode = 0;
        String sqlState = "00000";

        try { DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver()); }
        catch (Exception e) { System.out.println("Class not found"); }

        String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/COMP421";

        String user = System.getenv("SOCSUSER");
        String password = System.getenv("SOCSPASSWD");

        if (user == null || password == null)
        {
            System.out.println("Missing credentials");
            System.exit(1);
        }

        con = DriverManager.getConnection(url, user, password);
        System.out.println("Connected to DB2.");

	EmployeeMenu.init(con, in);
	FanMenu.init(con, in);
        mainMenu();

        con.close();
        in.close();
    }

	// Note: currently  have not implemented password and checking, that can be added later ;)
    public static void mainMenu() throws SQLException
    {
        while (true)
        {
            System.out.println("\nTiQ Main Menu");
            System.out.println("\t1. Employees");
            System.out.println("\t2. Fans");
            System.out.println("\t0. Exit");
            System.out.print("Please Enter Your Option: ");

            int choice = Integer.parseInt(in.nextLine());

            if (choice == 1) employeeLogin();
            else if (choice == 2) fanLogin();
            else if (choice == 0) return;
            else System.out.println("Invalid choice.");
        }
    }

    public static void employeeMenu(int employeeId)
    {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("     TiQ Employee Menu   ");
            System.out.println("========================================");
            System.out.println("  1. Add a new event");
            System.out.println("  2. Cancel an event");
            System.out.println("  3. Modify ticket prices");
            System.out.println("  0. Back");
            System.out.println("========================================");
            System.out.print("Enter your choice: ");

            String choice = in.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1": EmployeeMenu.addNewEvent(employeeId);  break;
                case "2": EmployeeMenu.cancelEvent(employeeId);  break;
                case "3": EmployeeMenu.modifyPrices(employeeId); break;
                case "0": return;
                default:
                    System.out.println("Invalid choice.\n");
            }
        }
    }

    public static void fanMenu(String fanEmail) throws SQLException
    {
        while (true){
            System.out.println("\n========================================");
            System.out.println("     TiQ Fan Menu   ");
            System.out.println("========================================");
            System.out.println("1. Add artist to Likes");
            System.out.println("2. View liked artists");
            System.out.println("3. View events (filter)");
            System.out.println("4. Buy tickets");
            System.out.println("5. View purchased tickets");
            System.out.println("0. Back");
            System.out.println("========================================");
            System.out.print("Please Enter Your Option: ");

            String choice = in.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1": FanMenu.addArtistToLikes(fanEmail);	break;
                case "2": FanMenu.viewLikedArtists(fanEmail);	break;
                case "3": FanMenu.viewEvents(fanEmail);		break;
                case "4": FanMenu.buyTicket(fanEmail); break;
                case "5": FanMenu.viewTickets(fanEmail); break;
                case "0": return;
                default:
                    System.out.println("Invalid choice.\n");
	     }
        }
    }

    // not formal login, but a placeholder for real username/password login
    // just have to enter their email (and it has to exist in the database already)
    public static void fanLogin() throws SQLException {
        System.out.print("Fan email: ");
        String fanEmail = in.nextLine().trim();

        // Verify fan exists
        String checkFanSQL = "SELECT 1 FROM Fan WHERE email = ?";
        PreparedStatement psCheckFan = con.prepareStatement(checkFanSQL);
        psCheckFan.setString(1, fanEmail);
        ResultSet rsFan = psCheckFan.executeQuery();

        if (!rsFan.next()) {
            System.out.println("Error: No fan found with that email.\n");
            rsFan.close();
            psCheckFan.close();
            return;
        }
        rsFan.close();
        psCheckFan.close();

        fanMenu(fanEmail);
    }

    // not formal login, but a placeholder for real username/password login
    // employees just have to enter their ID (must already exist in database)
    public static void employeeLogin() throws SQLException {
        System.out.print("Employee ID: ");
        int employeeId;
        try {
            employeeId = Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid employee ID.");
            return;
        }


        // validate employee id exists
        PreparedStatement psEmployee = con.prepareStatement(
            "SELECT 1 FROM Employee WHERE employee_id = ?");
        psEmployee.setInt(1, employeeId);
        ResultSet rsEmployee = psEmployee.executeQuery();
        if (!rsEmployee.next()) {
            System.out.println("Employee ID not found");
            rsEmployee.close();
            psEmployee.close();
            return;
        }
        rsEmployee.close();
        psEmployee.close();

        employeeMenu(employeeId);
    }
}
