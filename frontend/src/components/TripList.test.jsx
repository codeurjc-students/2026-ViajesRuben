import { render, screen } from "@testing-library/react";
import { vi, test, expect } from "vitest";
import TripList from "./TripList";
import { getTrips } from "../services/tripService";

vi.mock("../services/tripService", () => ({ getTrips: vi.fn() }));

test("renders the list of trips", async () => {
  getTrips.mockResolvedValue([
    {
      id: 1,
      name: "Trip 1",
      destination: "Lisbon",
      startDate: "2027-05-01",
      endDate: "2027-05-05",
    },
    {
      id: 2,
      name: "Trip 2",
      destination: "Rome",
      startDate: "2027-07-10",
      endDate: "2027-07-17",
    },
  ]);

  render(<TripList />);

  expect(await screen.findByText(/Trip 1 - Lisbon/)).toBeInTheDocument();
  expect(screen.getByText(/Trip 2 - Rome/)).toBeInTheDocument();
});
