import { useEffect, useState } from "react";
import { getTrips } from "../services/tripService";

export default function TripList() {
  const [trips, setTrips] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    getTrips()
      .then(setTrips)
      .catch(() => setError("Could not load trips"));
  }, []);

  return (
    <main>
      <h1>Trips</h1>
      {error ? (
        <p role="alert">{error}</p>
      ) : (
        <ul id="trips">
          {trips.map((trip) => (
            <li key={trip.id}>
              {trip.name} - {trip.destination} ({trip.startDate} to{" "}
              {trip.endDate})
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
