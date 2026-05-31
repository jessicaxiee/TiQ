SELECT DISTINCT T.artist_name, T.tour_name, T.event_date, V.name AS venue_name, T.venue_address, T.section, T.seat_no, T.price, T.tier
FROM Ticket T JOIN Venue V ON T.venue_address = V.address
WHERE NOT EXISTS (
    SELECT 1 
    FROM Buys_Ticket B
    WHERE T.license_no = B.license_no
      AND T.artist_name = B.artist_name
      AND T.tour_name = B.tour_name
      AND T.event_date = B.event_date
      AND T.venue_address = B.venue_address
      AND T.section = B.section
      AND T.seat_no = B.seat_no
);
