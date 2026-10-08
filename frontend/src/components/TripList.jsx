import { useEffect, useState } from "react";
import { getTrips } from "../services/tripService";

export default function TripList() {
  const [trips, setTrips] = useState([]);

  useEffect(() => {
    getTrips().then(setTrips);
  }, []);

  return (
    <main>
      <h1>Trips</h1>
      <ul id="trips">
        {trips.map((trip) => (
          <li key={trip.id}>
            {trip.name} - {trip.destination} ({trip.startDate} to {trip.endDate}
            )
          </li>
        ))}
      </ul>
    </main>
  );
}
