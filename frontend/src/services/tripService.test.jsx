import { test, expect, vi, afterEach } from "vitest";
import { getTrips } from "./tripService";

afterEach(() => {
  vi.restoreAllMocks();
});

test("fetches trips from the REST API", async () => {
  const trips = [{ id: 1, name: "Trip 1", destination: "Lisbon" }];
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(trips),
  });

  const result = await getTrips();

  expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/api/trips"));
  expect(result).toEqual(trips);
});
