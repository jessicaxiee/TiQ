**TiQ Project**

**Purpose**
- **Summary**: 
This program is an SQL-based ticketing platform with TUI for event management, validated on datasets of over 1000 records. Through this project, we designed and implemented complex relational structures to reflect real-world constraints. We also learned to apply indexing and advanced SQL queries to optimize performance and support business logic. Lastly, it incorporates role-based functionality for employees and customers, including ticket purchasing, event search and deletion, and triggers for ticket price modification auditing.

**What it evaluates**
- **Database setup**: SQL scripts in the repo should create the schema and sample data without errors.
- **JDBC connectivity**: Java programs connect to the database and run queries.
- **Menu functionality**: The menu programs allow interactive queries and basic operations.
- **SQL features**: Stored procedure and trigger examples are included (`q3_procedure.sql`, `q7_trigger_audit.sql`).

**Requirements**
- **Java**: JDK 8 or newer.
- **Database**: MySQL or compatible RDBMS (adjust commands/connection URL as needed).
- **JDBC driver**: MySQL Connector/J (add to classpath when compiling/running).
- **Shell**: The repository includes `.sh` scripts; on Windows use WSL, Git Bash, or convert commands for PowerShell.

**Setup**
1. Create a database (example name: `tiqdb`).

   - MySQL example:

     ```bash
     mysql -u root -p -e "CREATE DATABASE tiqdb;"
     ```

2. Create the tables:

   - Using the provided script (in Bash/WSL/Git Bash):

     ```bash
     sh createtbl.sh
     ```

   - Or directly with MySQL client:

     ```bash
     mysql -u USER -p tiqdb < createtbl.sql
     ```

3. Load sample data:

   ```bash
   sh loaddata.sh
   # or
   mysql -u USER -p tiqdb < loaddata.sql
   ```

4. (Optional) Install procedure/trigger examples:

   ```bash
   mysql -u USER -p tiqdb < q3_procedure.sql
   mysql -u USER -p tiqdb < q7_trigger_audit.sql
   ```

**Compile & Run (examples)**
- Add the MySQL Connector/J to your classpath. Replace `path/to/mysql-connector-java.jar` with the actual path.

- Compile (Windows PowerShell example):

```powershell
javac -cp .;path\to\mysql-connector-java.jar *.java
```

- Compile (Bash/WSL example):

```bash
javac -cp .:path/to/mysql-connector-java.jar *.java
```

- Run (adjust classpath and main class as needed):

```powershell
# Windows
java -cp .;path\to\mysql-connector-java.jar simpleJDBC
```

```bash
# Bash/WSL
java -cp .:path/to/mysql-connector-java.jar simpleJDBC
```

**Files of interest**
- **Main Java programs**: [EmployeeMenu.java](EmployeeMenu.java), [FanMenu.java](FanMenu.java), [simpleJDBC.java](simpleJDBC.java)
- **Alternate/test Java**: [testJava/simpleJDBC.java](testJava/simpleJDBC.java)
- **SQL & scripts**: [createtbl.sql](createtbl.sql), [loaddata.sql](loaddata.sql), [createtbl.sh](createtbl.sh), [loaddata.sh](loaddata.sh), [droptbl.sh](droptbl.sh), [droptbl.sql](droptbl.sql)
- **Examples**: [q3_procedure.sql](q3_procedure.sql), [q7_trigger_audit.sql](q7_trigger_audit.sql)

**Notes & next steps**
- On Windows prefer WSL or Git Bash to run the `.sh` scripts, or run the `.sql` files directly with the DB client.
- If you want, I can compile and run the Java programs here (requires a running DB and the JDBC driver). Would you like me to do that or create a commit with this README?

**Ticket Price Audit (DB2)**
- **Overview**: Modifying ticket price records an audit entry in the `Ticket_Price_Audit` table so price changes can be reviewed.
- **DB2 query**: Run the following on DB2 to view the most recent 20 ticket-price audit records:

```sql
SELECT changed_by_employee_id, old_price, new_price, changed_at,
        tour_name, event_date, section, seat_no
   FROM Ticket_Price_Audit
   ORDER BY changed_at DESC
   FETCH FIRST 20 ROWS ONLY;
```

**Running the audit SQL on DB2**
- **Using the provided script**: Save the query to `db2_audit.sql` (already included in the repo) and run it from the DB2 command line processor:

```bash
# connect (replace placeholders)
db2 connect to <DB_NAME> user <DB_USER> using <DB_PASSWORD>

# run the saved SQL file
db2 -tvf db2_audit.sql

# disconnect when done
db2 connect reset
```

- **One-line query** (no script file):

```bash
db2 "SELECT changed_by_employee_id, old_price, new_price, changed_at, tour_name, event_date, section, seat_no FROM Ticket_Price_Audit ORDER BY changed_at DESC FETCH FIRST 20 ROWS ONLY"
```

- **Notes**: Replace `<DB_NAME>`, `<DB_USER>`, and `<DB_PASSWORD>` with your DB2 credentials. The `-t` flag tells DB2 to use semicolon-terminated statements and `-v` prints the statements; `-f` runs the file.
