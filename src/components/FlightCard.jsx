import React from 'react';

export default function FlightCard({ flight, onSelect }){
  const dep = new Date(flight.departure).toLocaleString();
  const arr = new Date(flight.arrival).toLocaleString();

  return (
    <div className="card">
      <div className="card-left">
        <div className="title">{flight.airline} — {flight.flightNo}</div>
        <div className="sub">{flight.origin} → {flight.destination}</div>
        <div className="muted">Depart: {dep} · Arrive: {arr}</div>
      </div>

      <div className="card-right">
        <div className="price">₹{flight.price}</div>
        <div className="seats">{flight.seatsAvailable} left</div>
        <button className="btn" onClick={onSelect}>Select</button>
      </div>
    </div>
  );
}
