import { test, expect, vi, afterEach } from 'vitest';
import { getTrips } from './tripService';

afterEach(() => {
  vi.restoreAllMocks();
});

test('fetches trips from the REST API', async () => {
  const trips = [
    {
      id: 1,
      name: 'Trip 1',
      destination: 'Lisbon',
      startDate: '2027-05-01',
      endDate: '2027-05-05',
    },
  ];
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(trips),
  } as Response);

  const result = await getTrips();

  expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/v1/trips'));
  expect(result).toEqual(trips);
});
