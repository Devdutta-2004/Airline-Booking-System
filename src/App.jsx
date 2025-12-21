import React, { useEffect, useState } from 'react';
import { LuPlane, LuPlaneTakeoff, LuCalendarClock, LuTicket, LuCheckCircle, LuXCircle } from 'react-icons/lu';
import SearchBar from './components/SearchBar';
import BookingSuccessPopup from './components/BookingSuccessPopup';
import './styles.css';

export default function App() {
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [selectedFlight, setSelectedFlight] = useState(null);
  const [seatsToBook, setSeatsToBook] = useState(1);
  const [seatMap, setSeatMap] = useState([]);
  const [currentBooking, setCurrentBooking] = useState(null);

  // Popup state
  const [showSuccess, setShowSuccess] = useState(false);
  const [successData, setSuccessData] = useState(null);

  useEffect(() => { fetchAll(); }, []);

  async function fetchAll() {
    setLoading(true);
    setMessage('');
    try {
      const res = await fetch('http://localhost:8080/api/flights');
      const data = await res.json();
      setFlights(data);
    } catch (err) {
      setMessage('Could not connect to server.');
    }
    setLoading(false);
  }

  async function search(origin, destination, date) {
    setLoading(true);
    setMessage('');
    try {
      const url = `http://localhost:8080/api/flights/search?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&date=${encodeURIComponent(date)}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      setFlights(data);
    } catch (err) {
      setMessage('No flights found for this route.');
    }
    setLoading(false);
  }

  async function loadSeatMap(flight) {
    setSeatMap([]);
    if (!flight) return;
    try {
      const res = await fetch(`http://localhost:8080/api/flights/${flight.id}/seats`);
      if (!res.ok) throw new Error('Seat map fetch failed');
      const map = await res.json();
      setSeatMap(map);
    } catch (err) {
      console.error(err);
    }
  }

  // Handle Flight Selection
  const handleSelectFlight = (flight) => {
    setSelectedFlight(flight);
    setSeatsToBook(1);
    setCurrentBooking(null); // Clear previous booking if switching
    loadSeatMap(flight);
    // Smooth scroll to top on mobile
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  async function book() {
    if (!selectedFlight) return setMessage('Please select a flight first');
    setMessage('');
    setLoading(true);

    try {
      if (!seatMap || seatMap.length === 0) await loadSeatMap(selectedFlight);

      const available = (seatMap || []).filter(s => (s.status === 'AVAILABLE' || s.status === 'available'));
      const need = Number(seatsToBook) || 1;
      
      if (available.length < need) {
        throw new Error(`Not enough seats! Only ${available.length} left.`);
      }
      
      const chosenLabels = available.slice(0, need).map(s => s.seatLabel);
      const totalAmount = (selectedFlight.price || 0) * need;

      const body = {
        userId: 1,
        flightId: selectedFlight.id,
        seats: chosenLabels,
        amount: totalAmount
      };

      const res = await fetch('http://localhost:8080/api/book/hold', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      const data = await res.json();
      if (!res.ok) throw new Error(typeof data === 'string' ? data : JSON.stringify(data));

      setCurrentBooking({
        bookingId: data.bookingId,
        pnr: data.pnr,
        amount: data.amount,
        expiresAt: data.expiresAt
      });
      
      await fetchAll();
      await loadSeatMap(selectedFlight);
    } catch (err) {
      setMessage('Booking failed: ' + (err.message || err));
    } finally {
      setLoading(false);
    }
  }

  async function confirmPayment(success = true) {
    if (!currentBooking) return;
    setLoading(true);
    try {
      const res = await fetch('http://localhost:8080/api/payment/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookingId: currentBooking.bookingId, success })
      });
      
      if (!res.ok) throw new Error('Payment processing failed');
      
      await fetchAll();
      await loadSeatMap(selectedFlight);

      if (success) {
        setSuccessData({ bookingId: currentBooking.bookingId, pnr: currentBooking.pnr });
        setShowSuccess(true);
        setCurrentBooking(null);
        setSelectedFlight(null); // Reset selection after success
      } else {
        setMessage('Payment failed. Booking cancelled.');
        setCurrentBooking(null);
      }
    } catch (err) {
      setMessage('Error: ' + err.message);
    } finally {
      setLoading(false);
    }
  }

  // Visualization of Seats (Mini Map)
  const renderSeatVisuals = () => {
    if(!seatMap || seatMap.length === 0) return <div className="text-muted text-sm">Loading seat map...</div>;
    // Just show first 30 seats as dots to visualize density
    const displaySeats = seatMap.slice(0, 24); 
    return (
      <div className="seat-grid-mini">
        {displaySeats.map((seat, i) => (
          <div 
            key={i} 
            className={`seat-dot ${seat.status.toLowerCase() === 'available' ? 'available' : 'booked'}`}
            title={seat.seatLabel}
          />
        ))}
        {seatMap.length > 24 && <span style={{fontSize:10, color:'#999'}}>+{seatMap.length - 24} more</span>}
      </div>
    );
  };

  return (
    <div className="app-root">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand" onClick={fetchAll} style={{cursor:'pointer'}}>
            <img src="/logo.png" alt="logo" className="logo" onError={(e) => e.target.style.display='none'} />
            <div className="brand-text">
              <h1>SkyPass <span style={{color:'var(--primary)', fontWeight:400}}>Booking</span></h1>
            </div>
          </div>
          <div className="actions">
            <button className="btn ghost" onClick={fetchAll}>All Flights</button>
          </div>
        </div>
      </header>

      <div className="container">
        <main>
          <div className="hero-banner" role="img" aria-label="airplane banner">
             <div className="hero-overlay"></div>
             <div className="hero-content">
               <h2>Where will you go next?</h2>
               <p>Discover student discounts and exclusive flight deals.</p>
             </div>
          </div>

          <div style={{position:'relative', zIndex: 10, marginTop: '-25px'}}>
             <SearchBar onSearch={search} />
          </div>

          {message && <div className="alert">{message}</div>}

          {loading && !flights.length ? (
            <div className="center">
               <LuPlane className="spin" /> Loading flights...
            </div>
          ) : (
            <div className="grid">
              
              {/* Flight Results List */}
              <section className="list">
                <h3 style={{marginBottom:10, fontSize:'1.2rem'}}>Available Flights</h3>
                {flights.length === 0 ? <div className="card center">No flights found.</div> : flights.map(f => (
                  <div key={f.id} className={`flight-card ${selectedFlight?.id === f.id ? 'active-card' : ''}`}>
                    <div className="airline-logo">
                       <LuPlane />
                    </div>

                    <div className="flight-main">
                      <div className="route-info">
                        <div className="time-group">
                           <div className="time">{f.depart}</div>
                           <div className="code">{f.origin}</div>
                        </div>
                        <div className="duration-line"></div>
                        <div className="time-group">
                           <div className="time">{f.arrive}</div>
                           <div className="code">{f.destination}</div>
                        </div>
                      </div>
                      <div style={{marginTop:10, fontSize:14, color:'var(--text-muted)'}}>
                        {f.airline} • {f.flightNo}
                      </div>
                    </div>

                    <div className="price-section">
                      <span className="seats-tag">{f.seatsAvailable} seats left</span>
                      <div className="price">₹{f.price}</div>
                      <button className="btn" style={{marginTop:8, width:'100%'}} onClick={() => handleSelectFlight(f)}>
                        {selectedFlight?.id === f.id ? 'Selected' : 'Select'}
                      </button>
                    </div>
                  </div>
                ))}
              </section>

              {/* Sidebar: Booking Logic */}
              <aside className="sidebar">
                <h3>Booking Summary</h3>
                {selectedFlight ? (
                  <div className="selected-details">
                    <div style={{display:'flex', justifyContent:'space-between', marginBottom:12}}>
                        <strong>{selectedFlight.origin} → {selectedFlight.destination}</strong>
                        <span style={{color:'var(--primary)'}}>{selectedFlight.airline}</span>
                    </div>

                    {!currentBooking ? (
                      <>
                        <div style={{marginBottom:12}}>
                          <label className="muted" style={{display:'block', marginBottom:5, fontSize:14}}>Availability Preview</label>
                          {renderSeatVisuals()}
                        </div>

                        <div style={{background:'#f1f5f9', padding:15, borderRadius:8}}>
                          <label style={{display:'block', marginBottom:8, fontSize:14, fontWeight:600}}>Number of Passengers</label>
                          <div style={{display:'flex', gap:10}}>
                             <input 
                               type="number" min="1" max={selectedFlight.seatsAvailable} 
                               value={seatsToBook}
                               onChange={e => setSeatsToBook(e.target.value)} 
                             />
                             <button className="btn" onClick={book}>Book</button>
                          </div>
                          <div style={{fontSize:12, marginTop:8, color:'var(--text-muted)'}}>
                             Total: <strong>₹{selectedFlight.price * seatsToBook}</strong>
                          </div>
                        </div>
                      </>
                    ) : (
                      /* Payment State */
                      <div className="payment-box" style={{background:'#fff', border:'2px solid var(--primary)', padding:15, borderRadius:8}}>
                        <div style={{textAlign:'center', marginBottom:15}}>
                           <LuTicket style={{fontSize:30, color:'var(--primary)'}}/>
                           <h4 style={{margin:'10px 0'}}>Seat Held!</h4>
                           <div style={{fontSize:20, fontWeight:700}}>{currentBooking.pnr}</div>
                           <small style={{color:'var(--danger)'}}>Expires: {new Date(currentBooking.expiresAt).toLocaleTimeString()}</small>
                        </div>
                        
                        <div style={{display:'flex', flexDirection:'column', gap:8}}>
                          <button className="btn" style={{background:'var(--success)'}} onClick={() => confirmPayment(true)}>
                            <LuCheckCircle /> Confirm Payment
                          </button>
                          <button className="btn ghost" style={{color:'var(--danger)'}} onClick={() => confirmPayment(false)}>
                            <LuXCircle /> Cancel
                          </button>
                        </div>
                      </div>
                    )}

                  </div>
                ) : (
                   <div className="center" style={{padding:20}}>
                      <LuPlaneTakeoff style={{fontSize:40, opacity:0.2, marginBottom:10}} />
                      <p>Select a flight to view booking options.</p>
                   </div>
                )}
              </aside>

            </div>
          )}
        </main>
      </div>

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
