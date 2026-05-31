--#SET TERMINATOR @

CREATE VARIABLE CURRENT_EMPLOYEE_ID INTEGER DEFAULT NULL
@

CREATE TABLE Ticket_Price_Audit (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    license_no VARCHAR(15) NOT NULL,
    artist_name VARCHAR(50) NOT NULL,
    tour_name VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    venue_address VARCHAR(125) NOT NULL,
    section VARCHAR(10) NOT NULL,
    seat_no INTEGER NOT NULL,
    old_price DECIMAL(10,2),
    new_price DECIMAL(10,2),
    changed_by_employee_id INTEGER,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP
)
@

CREATE TRIGGER TRG_TICKET_PRICE_AUDIT
AFTER UPDATE OF price ON Ticket
REFERENCING OLD AS O NEW AS N
FOR EACH ROW
WHEN (O.price IS DISTINCT FROM N.price)
BEGIN ATOMIC
    INSERT INTO Ticket_Price_Audit (
        license_no,
        artist_name,
        tour_name,
        event_date,
        venue_address,
        section,
        seat_no,
        old_price,
        new_price,
        changed_by_employee_id,
        changed_at
    )
    VALUES (
        O.license_no,
        O.artist_name,
        O.tour_name,
        O.event_date,
        O.venue_address,
        O.section,
        O.seat_no,
        O.price,
        N.price,
        CURRENT_EMPLOYEE_ID,
        CURRENT TIMESTAMP
    );
END
@
