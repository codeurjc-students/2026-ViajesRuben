import { afterEach, describe, expect, test, vi } from 'vitest';
import { invitationOf, pageOf, receivedInvitationOf } from '../testHelpers';
import {
  createInvitation,
  getInvitationByToken,
  getInvitations,
  getMyInvitations,
  respondToInvitation,
} from './invitationService';

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

describe('I2 invitationService', () => {
  test('getMyInvitations asks for the first page by default and for the requested one', async () => {
    const page = pageOf([receivedInvitationOf()]);
    answer(page);

    await expect(getMyInvitations()).resolves.toEqual(page);
    await getMyInvitations(2, 5);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/invitations?page=0&size=10');
    expect(vi.mocked(fetch).mock.calls[1][0]).toContain('/api/v1/invitations?page=2&size=5');
  });

  test('getInvitationByToken reads the invitation of the token, encoded in the path', async () => {
    const invitation = receivedInvitationOf({ id: 4 });
    answer(invitation);

    await expect(getInvitationByToken('a-b_c/d')).resolves.toEqual(invitation);

    expect(vi.mocked(fetch).mock.calls[0][0]).toContain('/api/v1/invitations/token/a-b_c%2Fd');
  });

  test('respondToInvitation puts the answer to the status of the invitation', async () => {
    const answered = receivedInvitationOf({ id: 4, status: 'ACCEPTED' });
    answer(answered);

    await expect(respondToInvitation(4, 'ACCEPTED')).resolves.toEqual(answered);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain('/api/v1/invitations/4/status');
    expect(init?.method).toBe('PUT');
    expect(init?.body).toBe(JSON.stringify({ status: 'ACCEPTED' }));
  });

  test.each([403, 404, 409])(
    'a %i of the server becomes an ApiError with its status',
    async (status) => {
      vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status } as Response);

      await expect(respondToInvitation(4, 'REJECTED')).rejects.toMatchObject({ status });
      await expect(getInvitationByToken('x')).rejects.toMatchObject({ status });
    },
  );
});
