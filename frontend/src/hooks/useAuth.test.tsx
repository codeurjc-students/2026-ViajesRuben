import { renderHook } from '@testing-library/react';
import { test, expect, vi } from 'vitest';
import { useAuth } from './useAuth';

test('B2 useAuth refuses to work outside an AuthProvider', () => {
  vi.spyOn(console, 'error').mockImplementation(() => {});

  expect(() => renderHook(() => useAuth())).toThrow('useAuth must be used inside an AuthProvider');
});
