import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { tripOf } from '../testHelpers';
import TripForm from './TripForm';

const png = () => new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function fillRequired() {
  type('Nombre del viaje', 'Escapada');
  type('Destino', 'Lisboa');
  type('Fecha de inicio', '2027-05-01');
  type('Fecha de fin', '2027-05-05');
}

function renderForm(props: Partial<Parameters<typeof TripForm>[0]> = {}) {
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  render(<TripForm submitting={false} onSubmit={onSubmit} onCancel={onCancel} {...props} />);
  return { onSubmit, onCancel };
}

beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:preview');
  URL.revokeObjectURL = vi.fn();
});

describe('B4 TripForm creating', () => {
  test('starts empty with the creation button', () => {
    renderForm();

    expect(screen.getByLabelText('Nombre del viaje')).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Crear viaje' })).toBeEnabled();
    expect(screen.queryByText('Quitar portada')).not.toBeInTheDocument();
  });

  test('sends the trimmed data with no cover change', () => {
    const { onSubmit } = renderForm();
    fillRequired();
    type('Nombre del viaje', '  Escapada  ');
    type('Presupuesto (€)', '1200,50');
    type('Descripción', '  Pastéis  ');

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(onSubmit).toHaveBeenCalledWith(
      {
        name: 'Escapada',
        destination: 'Lisboa',
        startDate: '2027-05-01',
        endDate: '2027-05-05',
        budget: 1200.5,
        description: 'Pastéis',
      },
      { kind: 'keep' },
    );
  });

  test('shows a Spanish message under every missing required field and sends nothing', () => {
    const { onSubmit } = renderForm();

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(screen.getByText('Escribe el nombre del viaje.')).toBeInTheDocument();
    expect(screen.getByText('Escribe el destino del viaje.')).toBeInTheDocument();
    expect(screen.getByText('Elige la fecha de inicio.')).toBeInTheDocument();
    expect(screen.getByText('Elige la fecha de fin.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  test('explains an end date before the start date', () => {
    const { onSubmit } = renderForm();
    fillRequired();
    type('Fecha de fin', '2027-04-30');

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(
      screen.getByText('La fecha de fin no puede ser anterior a la de inicio.'),
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  test.each([
    [
      'Presupuesto (€)',
      'abc',
      'Escribe un importe válido, con hasta 2 decimales. Por ejemplo, 1200,50.',
    ],
    ['Presupuesto (€)', '-3', 'El presupuesto no puede ser negativo.'],
    ['Presupuesto (€)', '999999999', 'El presupuesto es demasiado alto.'],
    ['Nombre del viaje', 'n'.repeat(101), 'El nombre admite como máximo 100 caracteres.'],
    ['Destino', 'd'.repeat(101), 'El destino admite como máximo 100 caracteres.'],
    ['Descripción', 'x'.repeat(2001), 'La descripción admite como máximo 2000 caracteres.'],
  ])('rejects %s = "%.12s"', (label, value, message) => {
    const { onSubmit } = renderForm();
    fillRequired();
    type(label, value);

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(screen.getByText(message)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  test('clears the messages once the data is fixed', () => {
    const { onSubmit } = renderForm();
    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));
    fillRequired();

    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));

    expect(screen.queryByText('Escribe el nombre del viaje.')).not.toBeInTheDocument();
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  test('previews a chosen cover and sends it, and can discard it', () => {
    const { onSubmit } = renderForm();
    fillRequired();
    const file = png();

    fireEvent.change(screen.getByLabelText('Portada'), { target: { files: [file] } });
    expect(screen.getByAltText('Vista previa')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));
    expect(onSubmit.mock.calls[0][1]).toEqual({ kind: 'replace', file });

    fireEvent.click(screen.getByRole('button', { name: 'Descartar la imagen elegida' }));
    expect(screen.queryByAltText('Vista previa')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Crear viaje' }));
    expect(onSubmit.mock.calls[1][1]).toEqual({ kind: 'keep' });
  });

  test('cancel calls its handler', () => {
    const { onCancel } = renderForm();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  test('while submitting the buttons are disabled and say "Guardando…"', () => {
    renderForm({ submitting: true });

    expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });

  test('shows the error of the last attempt and a notice above the fields', () => {
    renderForm({ submitError: 'Revisa los datos del formulario.', notice: 'Aviso del viaje.' });

    expect(screen.getByText('Revisa los datos del formulario.')).toBeInTheDocument();
    expect(screen.getByText('Aviso del viaje.')).toBeInTheDocument();
  });

  test('counts the characters of the description', () => {
    renderForm();

    type('Descripción', 'Hola');

    expect(screen.getByText('Opcional. 4/2000')).toBeInTheDocument();
  });
});

describe('B4 TripForm editing', () => {
  const trip = tripOf({
    id: 5,
    name: 'Verano',
    destination: 'Roma',
    budget: 300.5,
    description: 'Ruinas',
    coverImageId: 'abc-123',
  });

  test('starts with the data of the trip and the save button', () => {
    renderForm({ trip });

    expect(screen.getByLabelText('Nombre del viaje')).toHaveValue('Verano');
    expect(screen.getByLabelText('Destino')).toHaveValue('Roma');
    expect(screen.getByLabelText('Fecha de inicio')).toHaveValue('2027-05-01');
    expect(screen.getByLabelText('Presupuesto (€)')).toHaveValue('300.5');
    expect(screen.getByLabelText('Descripción')).toHaveValue('Ruinas');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeInTheDocument();
    expect(screen.getByAltText('Portada actual')).toHaveAttribute('src', '/api/v1/images/abc-123');
  });

  test('saves the edited data keeping the cover', () => {
    const { onSubmit } = renderForm({ trip });
    type('Nombre del viaje', 'Verano 2027');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Verano 2027', budget: 300.5 }),
      { kind: 'keep' },
    );
  });

  test('removing the cover is announced, can be undone and is sent on save', () => {
    const { onSubmit } = renderForm({ trip });

    fireEvent.click(screen.getByRole('button', { name: 'Quitar portada' }));
    expect(
      screen.getByText('La portada se quitará al guardar el viaje.', { exact: false }),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('Cambiar portada')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Conservarla' }));
    expect(screen.getByAltText('Portada actual')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Quitar portada' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit.mock.calls[0][1]).toEqual({ kind: 'remove' });
  });

  test('choosing another image replaces the cover', () => {
    const { onSubmit } = renderForm({ trip });
    const file = png();

    fireEvent.change(screen.getByLabelText('Cambiar portada'), { target: { files: [file] } });
    expect(screen.queryByAltText('Portada actual')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit.mock.calls[0][1]).toEqual({ kind: 'replace', file });
  });

  test('a trip without cover offers no way to remove it', () => {
    renderForm({ trip: { ...trip, coverImageId: null } });

    expect(screen.queryByRole('button', { name: 'Quitar portada' })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Portada')).toBeInTheDocument();
  });
});
