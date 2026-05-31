--#SET TERMINATOR @

CREATE PROCEDURE Adjust_Ticket_Prices
(
    IN p_event_date DATE,
    IN p_percent_increase DECIMAL(5,2),
    IN p_price_cap DECIMAL(10,2)
)
LANGUAGE SQL
BEGIN
    DECLARE v_license_no VARCHAR(15);
    DECLARE v_artist_name VARCHAR(50);
    DECLARE v_tour_name VARCHAR(50);
    DECLARE v_event_date DATE;
    DECLARE v_venue_address VARCHAR(125);
    DECLARE v_section VARCHAR(10);
    DECLARE v_seat_no INTEGER;
    DECLARE v_old_price DECIMAL(10,2);
    DECLARE v_new_price DECIMAL(10,2);
    DECLARE at_end INT DEFAULT 0;

    DECLARE not_found CONDITION FOR SQLSTATE '02000';

    DECLARE ticket_cursor CURSOR FOR
        SELECT T.license_no,
               T.artist_name,
               T.tour_name,
               T.event_date,
               T.venue_address,
               T.section,
               T.seat_no,
               T.price
        FROM Ticket T
        LEFT JOIN Buys_Ticket B
          ON T.license_no = B.license_no
         AND T.artist_name = B.artist_name
         AND T.tour_name = B.tour_name
         AND T.event_date = B.event_date
         AND T.venue_address = B.venue_address
         AND T.section = B.section
         AND T.seat_no = B.seat_no
        WHERE T.event_date = p_event_date
          AND B.fan_email IS NULL
          AND T.price IS NOT NULL;

    DECLARE CONTINUE HANDLER FOR not_found
        SET at_end = 1;

    OPEN ticket_cursor;

    FETCH ticket_cursor INTO
        v_license_no,
        v_artist_name,
        v_tour_name,
        v_event_date,
        v_venue_address,
        v_section,
        v_seat_no,
        v_old_price;

    WHILE at_end = 0 DO
        SET v_new_price = DECIMAL(DOUBLE(v_old_price) * (1 + DOUBLE(p_percent_increase) / 100), 10, 2);

        IF v_new_price > p_price_cap THEN
            SET v_new_price = p_price_cap;
        END IF;

        UPDATE Ticket
        SET price = v_new_price
        WHERE license_no = v_license_no
          AND artist_name = v_artist_name
          AND tour_name = v_tour_name
          AND event_date = v_event_date
          AND venue_address = v_venue_address
          AND section = v_section
          AND seat_no = v_seat_no;

        FETCH ticket_cursor INTO
            v_license_no,
            v_artist_name,
            v_tour_name,
            v_event_date,
            v_venue_address,
            v_section,
            v_seat_no,
            v_old_price;
    END WHILE;

    CLOSE ticket_cursor;
END
@
