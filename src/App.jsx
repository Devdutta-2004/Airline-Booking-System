import React, { useEffect, useState } from 'react';
import { LuPlane, LuPlaneTakeoff } from 'react-icons/lu';
import SearchBar from './components/SearchBar';
import BookingSuccessPopup from './components/BookingSuccessPopup';
import './styles.css';

export default function App(){
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [selectedFlight, setSelectedFlight] = useState(null);
  const [seatsToBook, setSeatsToBook] = useState(1);
  const [seatMap, setSeatMap] = useState([]); // current flight seat map
  const [currentBooking, setCurrentBooking] = useState(null); // { bookingId, pnr, amount, expiresAt }

  // Popup state
  const [showSuccess, setShowSuccess] = useState(false);
  const [successData, setSuccessData] = useState(null);

  useEffect(()=>{ fetchAll(); },[]);

  async function fetchAll(){
    setLoading(true);
    setMessage('');
    try{
      const res = await fetch('http://localhost:8080/api/flights');
      const data = await res.json();
      setFlights(data);
    }catch(err){
      setMessage('Could not load flights. Is backend running?');
    }
    setLoading(false);
  }

  async function search(origin, destination, date){
    setLoading(true);
    setMessage('');
    try{
      const url = `http://localhost:8080/api/flights/search?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&date=${encodeURIComponent(date)}`;
      const res = await fetch(url);
      if(!res.ok) throw new Error(await res.text());
      const data = await res.json();
      setFlights(data);
    }catch(err){
      setMessage('Search failed');
    }
    setLoading(false);
  }

  // fetch seat map for selected flight
  async function loadSeatMap(flight) {
    setSeatMap([]);
    if (!flight) return;
    try {
      const res = await fetch(`http://localhost:8080/api/flights/${flight.id}/seats`);
      if (!res.ok) throw new Error('Seat map fetch failed');
      const map = await res.json();
      setSeatMap(map);
    } catch (err) {
      setMessage('Could not load seat map: ' + (err.message || err));
    }
  }

  // New book() — uses /api/book/hold and seat labels
  async function book(){
    if(!selectedFlight) return setMessage('Please select a flight first');
    setMessage('');
    setLoading(true);

    try{
      // ensure seat map loaded
      if (!seatMap || seatMap.length === 0) {
        await loadSeatMap(selectedFlight);
      }

      // pick available seats
      const available = (seatMap || []).filter(s => (s.status === 'AVAILABLE' || s.status === 'available'));
      const need = Number(seatsToBook) || 1;
      if(available.length < need){
        throw new Error(`Not enough available seats (requested ${need}, available ${available.length})`);
      }
      const chosenLabels = available.slice(0, need).map(s => s.seatLabel);

      // build body (amount = price * seats)
      const totalAmount = (selectedFlight.price || 0) * need;
      const body = {
        userId: 1,
        flightId: selectedFlight.id,
        seats: chosenLabels,
        amount: totalAmount
      };

      const res = await fetch('http://localhost:8080/api/book/hold', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify(body)
      });

      const data = await res.json();
      if(!res.ok) throw new Error(typeof data === 'string' ? data : JSON.stringify(data));

      // success: show PNR and expiry in sidebar
      setCurrentBooking({
        bookingId: data.bookingId,
        pnr: data.pnr,
        amount: data.amount,
        expiresAt: data.expiresAt
      });
      setMessage(`Hold created — PNR: ${data.pnr}. Pay to confirm before ${new Date(data.expiresAt).toLocaleString()}`);

      // refresh flights & seat map
      await fetchAll();
      await loadSeatMap(selectedFlight);
    }catch(err){
      setMessage('Booking failed: ' + (err.message || err));
    }finally{
      setLoading(false);
    }
  }

  // Mock confirm payment (calls backend)
  async function confirmPayment(success = true) {
    if (!currentBooking) return setMessage('No booking to confirm');
    setLoading(true);
    setMessage('');
    try {
      const res = await fetch('http://localhost:8080/api/payment/confirm', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ bookingId: currentBooking.bookingId, success })
      });
      const data = await res.json();
      if(!res.ok) throw new Error(typeof data === 'string' ? data : JSON.stringify(data));
      setMessage(success ? 'Payment successful — booking confirmed.' : 'Payment failed — booking cancelled.');
      // refresh seats & flights
      await fetchAll();
      await loadSeatMap(selectedFlight);

      // If payment succeeded, show success popup with booking info
      if (success) {
        // use a snapshot of the booking to show in popup (PNR & id)
        setSuccessData({
          bookingId: currentBooking.bookingId,
          pnr: currentBooking.pnr
        });
        setShowSuccess(true);
        // clear currentBooking in the sidebar (booking is now confirmed)
        setCurrentBooking(null);
      }
    } catch (err) {
      setMessage('Payment confirm failed: ' + (err.message || err));
    } finally {
      setLoading(false);
    }
  }

  // Download ticket (opens new tab)
 function downloadTicket() {
  window.open(`${window.location.origin}/ticket_bg.pdf`, '_blank');
}


  // runtime URL to public/airplane.png (public folder)
  const heroBgUrl = `${process.env.PUBLIC_URL}/airplane.png`;

  return (
    <div className="app-root light-with-pattern">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand">
            <img src="/logo.png" alt="logo" className="logo" />
            <div className="brand-text">
              <h1>SkyPass Ticket Booking</h1>
            </div>
          </div>

          <div className="actions">
            <button className="btn ghost" onClick={fetchAll}>Show all</button>

            <button className="btn">
              <LuPlane style={{ marginRight: 8 }} />
              Flights
            </button>
          </div>
        </div>
      </header>

      <div className="container">
        <main>
          <div
            className="hero-banner"
            style={{ backgroundImage: `url("${heroBgUrl}")` }}
            role="img"
            aria-label="airplane banner"
          >
            <div className="hero-content">
              <h2>Find your next flight</h2>
              <p>Book safely and quickly — student discounts available.</p>
            </div>

            <div className="hero-plane" aria-hidden>
              <LuPlane style={{ color: 'var(--accent)', width: 20, height: 20 }} />
            </div>
          </div>

          <SearchBar onSearch={search} />

          {message && <div className="alert">{message}</div>}

          {loading ? (
            <div className="center">Loading…</div>
          ) : (
            <div className="grid">
              <aside className="sidebar">
                <h3>Selected</h3>
                {selectedFlight ? (
                  <div className="selected">
                    <div style={{display:'flex',alignItems:'center',gap:10}}>
                      <LuPlaneTakeoff style={{color:'var(--accent)'}} />
                      <div style={{fontWeight:700}}>{selectedFlight.airline} — {selectedFlight.flightNo}</div>
                    </div>
                    <div style={{marginTop:8}}>{selectedFlight.origin} → {selectedFlight.destination}</div>
                    <div className="muted" style={{marginTop:6}}>Seats left: <strong>{selectedFlight.seatsAvailable}</strong></div>
                    <div style={{marginTop:12}}>
                      <label className="muted">Seats to book</label>
                      <input type="number" min="1" max={selectedFlight.seatsAvailable} value={seatsToBook}
                        onChange={e => setSeatsToBook(e.target.value)} />
                    </div>

                    <div style={{display:'flex', gap:8, marginTop:12}}>
                      <button className="btn primary" onClick={book}>Book now</button>
                      <button className="btn" onClick={() => loadSeatMap(selectedFlight)}>Refresh seats</button>
                    </div>

                    {currentBooking && (
                      <div style={{marginTop:12, padding:8, border:'1px solid #eee', borderRadius:6}}>
                        <div><strong>PNR:</strong> {currentBooking.pnr}</div>
                        <div className="muted">Expires: {new Date(currentBooking.expiresAt).toLocaleString()}</div>
                        <div style={{marginTop:8, display:'flex', gap:8}}>
                          <button className="btn" onClick={() => confirmPayment(true)}>Mock Pay (success)</button>
                          <button className="btn ghost" onClick={() => confirmPayment(false)}>Mock Pay (fail)</button>
                          <button className="btn" onClick={downloadTicket}>Download Ticket</button>
                        </div>
                      </div>
                    )}
                  </div>
                ) : <div className="muted">No flight selected</div>}
              </aside>

              <section className="list">
                {flights.length === 0 ? <div className="card">No flights found.</div> : flights.map(f => (
                  <div key={f.id} className="flight-card">
                    <div className="flight-left">
                      <div style={{display:'flex',alignItems:'center',gap:8}}>
                        <LuPlane style={{color:'var(--accent)'}} />
                        <div style={{fontWeight:700}}>{f.airline}</div>
                      </div>
                      <div className="flightno">{f.flightNo}</div>
                    </div>

                    <div className="flight-main">
                      <div className="route">{f.origin} → {f.destination}</div>
                      <div className="meta">{f.depart} — {f.arrive}</div>
                      <div className="meta">{f.seatsAvailable} seats available</div>
                    </div>

                    <div className="flight-price">
                      <div className="amount">₹{f.price}</div>
                      <div className="sub">per person</div>
                      <button className="btn" style={{marginTop:8}} onClick={() => { setSelectedFlight(f); setSeatsToBook(1); loadSeatMap(f); }}>
                        <LuPlaneTakeoff style={{marginRight:8}} /> Select
                      </button>
                    </div>
                  </div>
                ))}
              </section>
            </div>
          )}
        </main>

        <footer className="footer">
          <code>http://localhost:8080</code>
        </footer>
      </div>

      {/* Booking success popup */}
      {showSuccess && successData && (
        <BookingSuccessPopup
          open={showSuccess}
          pnr={successData.pnr}
          bookingId={successData.bookingId}
          onClose={() => setShowSuccess(false)}
          onDownload={() => window.open(`http://localhost:8080/api/booking/${successData.bookingId}/ticket`)}
        />
      )}
    </div>
  );
}
