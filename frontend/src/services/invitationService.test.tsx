import { afterEach, describe, expect, test, vi } from 'vitest';
import { invitationOf, pageOf } from '../testHelpers';
import { createInvitation, getInvitations } from './invitationService';

afterEach(() => {
  vi.restoreAllMocks();
});

function answer(body: unknown, status = 200) {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    status,
    json: () => Promise.resolve(body),
  } as Response);
}

describe('I1 invitationService', () => {
  test('getInvitations asks for the first page of the trip by default', async () => {
    const page = pageOf([invitationOf()]);
    answer(page);

    await expect(getInvitations(3)).resolves.toEqual(page);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/invitations?page=0&size=10',
    );
  });

  test('getInvitations asks for the requested page and size', async () => {
    answer(pageOf([]));

    await getInvitations(3, 2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain(
      '/api/v1/trips/3/invitations?page=2&size=5',
    );
  });

  test('createInvitation posts the email as JSON and returns the invitation', async () => {
    const invitation = invitationOf({ id: 9 });
    answer(invitation, 201);

    await expect(createInvitation(3, 'friend@example.com')).resolves.toEqual(invitation);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain('/api/v1/trips/3/invitations');
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ email: 'friend@example.com' }));
  });

  test('a refusal of the server becomes an ApiError with its status', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 409 } as Response);

    await expect(createInvitation(3, 'friend@example.com')).rejects.toMatchObject({ status: 409 });
  });
});
