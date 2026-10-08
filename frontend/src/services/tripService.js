const API_URL = import.meta.env.VITE_API_URL ?? "";

export async function getTrips() {
  const response = await fetch(`${API_URL}/api/trips`);
  return response.json();
}
