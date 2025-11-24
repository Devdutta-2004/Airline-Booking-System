import React, { useState } from 'react';

export default function SearchBar({ onSearch }){
  const [origin, setOrigin] = useState('Mumbai');
  const [destination, setDestination] = useState('Delhi');
  const [date, setDate] = useState('2025-12-01');

  function submit(e){ e.preventDefault(); onSearch(origin, destination, date); }

  return (
    <form className="search" onSubmit={submit}>
      <div className="field"><label>Origin</label><input value={origin} onChange={e=>setOrigin(e.target.value)} /></div>
      <div className="field"><label>Destination</label><input value={destination} onChange={e=>setDestination(e.target.value)} /></div>
      <div className="field"><label>Date</label><input type="date" value={date} onChange={e=>setDate(e.target.value)} /></div>
      <div className="actions"><button className="btn primary" type="submit">Search</button></div>
    </form>
  );
}
