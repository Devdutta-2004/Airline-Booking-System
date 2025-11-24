import React, { useEffect } from "react";

export default function BookingSuccessPopup({ open, pnr, bookingId, onClose, onDownload }) {
  useEffect(() => {
    if (!open) return;
    const timer = setTimeout(onClose, 10000); // auto close in 10s
    return () => clearTimeout(timer);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="popup-backdrop">
      <div className="popup">
        <h2>🎉 Booking Successful!</h2>
        <p>Your ticket has been confirmed.</p>

        <div className="popup-details">
          <div><strong>PNR:</strong> {pnr}</div>
          <div><strong>Booking ID:</strong> {bookingId}</div>
        </div>

        <div className="popup-buttons">
          <button className="btn primary" onClick={onDownload}>Download Ticket</button>
          <button className="btn ghost" onClick={onClose}>Close</button>
        </div>
      </div>

      <style>{`
        .popup-backdrop{position:fixed;inset:0;background:rgba(0,0,0,0.35);display:flex;align-items:center;justify-content:center;z-index:9999}
        .popup{background:#fff;padding:18px;border-radius:12px;min-width:320px;box-shadow:0 12px 30px rgba(2,6,23,0.2);text-align:left}
        .popup-details{margin:12px 0}
        .popup-buttons{display:flex;gap:8px;justify-content:flex-end}
        .btn{padding:8px 12px;border-radius:8px;border:1px solid transparent;cursor:pointer}
        .btn.primary{background:#0b74ff;color:#fff}
        .btn.ghost{background:transparent;border:1px solid #e6eef8}
      `}</style>
    </div>
  );
}
