CREATE TABLE Fan (
	email VARCHAR(50) NOT NULL,
	name VARCHAR(50),
	phone VARCHAR(20),
	PRIMARY KEY (email)
);

CREATE TABLE Venue (
	address VARCHAR(125) NOT NULL,
	name VARCHAR(75),
	capacity INTEGER,
	contact_info VARCHAR(50),
	PRIMARY KEY (address)
);

CREATE TABLE Management (
	license_no VARCHAR(15) NOT NULL,
	name VARCHAR(50),
	PRIMARY KEY (license_no)
);

CREATE TABLE Artist (
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	industry VARCHAR(50),
	PRIMARY KEY (license_no, artist_name),
	FOREIGN KEY (license_no) REFERENCES Management(license_no)
);

CREATE TABLE Tour_Event (
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	PRIMARY KEY (tour_name, event_date, license_no, artist_name),
	FOREIGN KEY (license_no, artist_name) REFERENCES Artist(license_no,artist_name)
);

CREATE TABLE Concert (
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	setlist VARCHAR(500),
	PRIMARY KEY (tour_name, event_date, license_no, artist_name), 
	FOREIGN KEY (tour_name, event_date, license_no, artist_name) REFERENCES Tour_Event(tour_name, event_date, license_no, artist_name)
);

CREATE TABLE Fansign (
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	interaction_time INTEGER,
	PRIMARY KEY (tour_name, event_date, license_no, artist_name), 
	FOREIGN KEY (tour_name, event_date, license_no, artist_name) REFERENCES Tour_Event(tour_name, event_date, license_no, artist_name)
);

CREATE TABLE Listing (
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	venue_address VARCHAR(125) NOT NULL,
	PRIMARY KEY (license_no, artist_name, tour_name, event_date, venue_address),
	FOREIGN KEY (tour_name, event_date, license_no, artist_name) REFERENCES Tour_Event(tour_name, event_date, license_no, artist_name),
	FOREIGN KEY (venue_address) REFERENCES Venue(address)
);

CREATE TABLE Likes (
	fan_email VARCHAR(50) NOT NULL,
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	PRIMARY KEY (fan_email, license_no, artist_name),
	FOREIGN KEY (fan_email) REFERENCES Fan(email),
	FOREIGN KEY (license_no, artist_name) REFERENCES Artist(license_no, artist_name)
);

CREATE TABLE Employee (
	employee_id INTEGER NOT NULL,
	venue_address VARCHAR(125) NOT NULL,
	name VARCHAR(50),
	PRIMARY KEY (employee_id),
	FOREIGN KEY (venue_address) REFERENCES Venue(address)
);

CREATE TABLE Ticket(
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	venue_address VARCHAR(125) NOT NULL,
	section VARCHAR(10) NOT NULL,
	seat_no INTEGER NOT NULL,
	price DECIMAL(10, 2),
	tier VARCHAR(10),
	PRIMARY KEY (license_no, artist_name, tour_name, event_date, venue_address, section, seat_no),
	FOREIGN KEY (license_no, artist_name, tour_name, event_date, venue_address)
	REFERENCES Listing(license_no, artist_name, tour_name, event_date, venue_address)
	ON DELETE CASCADE
);

CREATE TABLE Buys_Ticket (
	fan_email VARCHAR(50) NOT NULL,
	employee_id INTEGER NOT NULL,
	license_no VARCHAR(15) NOT NULL,
	artist_name VARCHAR(50) NOT NULL,
	tour_name VARCHAR(50) NOT NULL,
	event_date DATE NOT NULL,
	venue_address VARCHAR(125) NOT NULL,
	section VARCHAR(10) NOT NULL,
	seat_no INTEGER NOT NULL,
	purchase_time TIMESTAMP,
  	purchase_method VARCHAR(10),
	UNIQUE (license_no, artist_name, tour_name, event_date, venue_address, section, seat_no),
  	PRIMARY KEY (fan_email, employee_id, license_no, artist_name, tour_name, event_date, venue_address, section, seat_no),
  	FOREIGN KEY (fan_email) REFERENCES Fan(email),
  	FOREIGN KEY (employee_id) REFERENCES Employee(employee_id),
  	FOREIGN KEY (license_no, artist_name, tour_name, event_date, venue_address, section, seat_no)
  	REFERENCES Ticket(license_no, artist_name, tour_name, event_date, venue_address, section, seat_no)
);
