# Airline-Booking-System
📌 Project Workflow (How the System Works)

The Airline Booking System follows a simple end-to-end workflow that connects the user interface, backend, and database to complete a flight booking. Below is the full lifecycle of how the system operates:

1️⃣ User Searches for Flights
The user opens the frontend webpage.
They enter origin, destination, and travel date.
When they click Search, the frontend sends a request to the backend API (e.g., /api/flights).
2️⃣ Backend Processes the Search Request
The backend receives the search parameters.
It queries the database to find flights matching:
Origin
Destination
Date
The backend returns a list of available flights to the frontend.
3️⃣ Flights Are Displayed to the User
The frontend receives the JSON response.
It displays flight options with:
Airline name
Flight number
Timing (departure/arrival)
Available seats
Ticket price
The user then selects the flight they want.
4️⃣ User Enters Passenger Details
After selecting a flight, the user enters:
Name
Email
No. of seats
The frontend validates the fields and sends the data to the backend via /api/bookings.
5️⃣ Booking Is Created in the Database
The backend verifies:
Seats available
Valid flight ID
If valid:
A new booking entry is inserted into the database.
Available seats for the flight are updated.
The backend returns:
Booking confirmation
Booking ID
Ticket details
6️⃣ Ticket Generation
Once booking is successful:
A PDF ticket is generated (or a static PDF is served, depending on your implementation).
User can click Download Ticket to save it.
The ticket includes:
Passenger details
Flight details
Travel date/time
Booking ID
You currently have a sample ticket (ticket_150.pdf) in the project that can be downloaded.
7️⃣ User Receives Confirmation
The user sees a final confirmation message.
They can:
Download/print ticket
Make another booking
View summary
<img width="870" height="489" alt="Picture1" src="https://github.com/user-attachments/assets/16c5e295-7e74-40df-bd0e-dd257b477dd8" />
<img width="1032" height="581" alt="Picture2" src="https://github.com/user-attachments/assets/876af421-73a8-469b-84b8-8c49b673ed9d" />
<img width="575" height="618" alt="Picture3" src="https://github.com/user-attachments/assets/6d78ea1d-d424-4655-b77b-11d6f0997b61" />
<img width="1162" height="508" alt="Picture4" src="https://github.com/user-attachments/assets/dc006b72-a3c0-46d5-8b0b-96e5579dcb3a" />
<img width="1061" height="554" alt="Picture5" src="https://github.com/user-attachments/assets/24dd5735-1fc8-4449-bc8e-4b23e6fba096" />
