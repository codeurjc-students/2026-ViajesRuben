import { render, screen } from "@testing-library/react";
import { test, expect } from "vitest";
import TripList from "./TripList";

test("renders trips fetched from the REST API", async () => {
  render(<TripList />);

  expect(
    await screen.findByText(/Trip 1 - Lisbon/, {}, { timeout: 5000 }),
  ).toBeInTheDocument();
  expect(screen.getByText(/Trip 2 - Rome/)).toBeInTheDocument();
});
