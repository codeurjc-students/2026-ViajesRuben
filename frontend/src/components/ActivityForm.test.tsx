import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { activityOf, tripOf } from '../testHelpers';
import ActivityForm from './ActivityForm';

const trip = tripOf({ id: 3, startDate: '2027-05-01', endDate: '2027-05-05' });
const png = () => new File([new Uint8Array(4)], 'a.png', { type: 'image/png' });

function type(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function renderForm(props: Partial<Parameters<typeof ActivityForm>[0]> = {}) {
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  render(
    <ActivityForm
      trip={trip}
      initialDate="2027-05-02"
      submitting={false}
      onSubmit={onSubmit}
      onCancel={onCancel}
      {...props}
    />,
  );
  return { onSubmit, onCancel };
}

beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:preview');
  URL.revokeObjectURL = vi.fn();
});

describe('B5 ActivityForm creating', () => {
  test('starts empty on the given day with the creation button', () => {
    renderForm();

    expect(screen.getByLabelText('Nombre de la actividad')).toHaveValue('');
    expect(screen.getByLabelText('Día')).toHaveValue('2027-05-02');
    expect(screen.getByLabelText('Categoría')).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Crear actividad' })).toBeEnabled();
    expect(screen.queryByText('Quitar imagen')).not.toBeInTheDocument();
  });

  test('limits the day to the days of the trip', () => {
    renderForm();

    expect(screen.getByLabelText('Día')).toHaveAttribute('min', '2027-05-01');
    expect(screen.getByLabelText('Día')).toHaveAttribute('max', '2027-05-05');
  });

  test('offers every category in Spanish', () => {
    renderForm();

    const names = screen.getAllByRole('option').map((option) => option.textContent);
    expect(names).toEqual([
      'Sin categoría',
      'Visita',
      'Comida',
      'Transporte',
      'Alojamiento',
      'Ocio',
      'Otra',
    ]);
  });

  test('sends the trimmed data with no image change', () => {
    const { onSubmit } = renderForm();
    type('Nombre de la actividad', '  Museo  ');
    type('Categoría', 'FOOD');
    type('Hora de inicio', '10:00');
    type('Hora de fin', '12:30');
    type('Coste estimado (€)', '16,50');
    type('Ubicación', ' Alfama ');
    type('Descripción', '  Visita guiada ');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(onSubmit).toHaveBeenCalledWith(
      {
        name: 'Museo',
        date: '2027-05-02',
        startTime: '10:00',
        endTime: '12:30',
        location: 'Alfama',
        description: 'Visita guiada',
        cost: 16.5,
        category: 'FOOD',
      },
      { kind: 'keep' },
    );
  });

  test('only the name and the day are needed', () => {
    const { onSubmit } = renderForm();
    type('Nombre de la actividad', 'Paseo');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Paseo', startTime: null, cost: null, category: null }),
      { kind: 'keep' },
    );
  });

  test('shows each problem under its field and does not submit', () => {
    const { onSubmit } = renderForm();
    type('Día', '2027-06-01');
    type('Hora de fin', '10:00');
    type('Coste estimado (€)', 'mucho');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(screen.getByText('Escribe el nombre de la actividad.')).toBeInTheDocument();
    expect(
      screen.getByText('El día debe estar dentro de las fechas del viaje.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Indica también la hora de inicio.')).toBeInTheDocument();
    expect(
      screen.getByText('Escribe un importe válido, con hasta 2 decimales. Por ejemplo, 16,50.'),
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  test('an end that is not after the start is explained', () => {
    const { onSubmit } = renderForm();
    type('Nombre de la actividad', 'Paseo');
    type('Hora de inicio', '12:00');
    type('Hora de fin', '12:00');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(
      screen.getByText('La hora de fin debe ser posterior a la de inicio.'),
    ).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  test('a missing day is asked for', () => {
    renderForm({ initialDate: undefined });
    type('Nombre de la actividad', 'Paseo');

    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));

    expect(screen.getByText('Elige el día de la actividad.')).toBeInTheDocument();
  });

  test('shows a counter for the description', () => {
    renderForm();
    type('Descripción', 'hola');

    expect(screen.getByText('Opcional. 4/2000')).toBeInTheDocument();
  });

  test('hands over a chosen image, previews it and can discard it', () => {
    const { onSubmit } = renderForm();
    type('Nombre de la actividad', 'Paseo');
    const file = png();

    fireEvent.change(screen.getByLabelText('Imagen'), { target: { files: [file] } });
    expect(screen.getByAltText('Vista previa')).toHaveAttribute('src', 'blob:preview');
    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.anything(), { kind: 'replace', file });

    fireEvent.click(screen.getByRole('button', { name: 'Descartar la imagen elegida' }));
    fireEvent.click(screen.getByRole('button', { name: 'Crear actividad' }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.anything(), { kind: 'keep' });
  });

  test('shows the notice and the error of the last save', () => {
    renderForm({ notice: 'La actividad se ha guardado', submitError: 'Revisa los datos' });

    expect(screen.getByText('La actividad se ha guardado')).toBeInTheDocument();
    expect(screen.getByText('Revisa los datos')).toBeInTheDocument();
  });

  test('disables the buttons and says "Guardando…" while saving', () => {
    renderForm({ submitting: true });

    expect(screen.getByRole('button', { name: 'Guardando…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });

  test('cancelling reports it without submitting', () => {
    const { onSubmit, onCancel } = renderForm();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});

describe('B5 ActivityForm editing', () => {
  const activity = activityOf({
    id: 7,
    name: 'Museo',
    date: '2027-05-03',
    startTime: '10:00:00',
    endTime: '11:30:00',
    category: 'SIGHTSEEING',
    cost: 12.5,
    location: 'Alfama',
    description: 'Visita',
    imageId: 'img-1',
  });

  test('starts with the values of the activity and the save button', () => {
    renderForm({ activity });

    expect(screen.getByLabelText('Nombre de la actividad')).toHaveValue('Museo');
    expect(screen.getByLabelText('Día')).toHaveValue('2027-05-03');
    expect(screen.getByLabelText('Hora de inicio')).toHaveValue('10:00');
    expect(screen.getByLabelText('Hora de fin')).toHaveValue('11:30');
    expect(screen.getByLabelText('Categoría')).toHaveValue('SIGHTSEEING');
    expect(screen.getByLabelText('Coste estimado (€)')).toHaveValue('12.5');
    expect(screen.getByLabelText('Ubicación')).toHaveValue('Alfama');
    expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeEnabled();
  });

  test('shows the current image and keeps it unless told otherwise', () => {
    const { onSubmit } = renderForm({ activity });

    expect(screen.getByAltText('Imagen actual')).toHaveAttribute('src', '/api/v1/images/img-1');
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.anything(), { kind: 'keep' });
  });

  test('removes the image on request and can take it back', () => {
    const { onSubmit } = renderForm({ activity });

    fireEvent.click(screen.getByRole('button', { name: 'Quitar imagen' }));
    expect(
      screen.getByText('La imagen se quitará al guardar la actividad', { exact: false }),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.anything(), { kind: 'remove' });

    fireEvent.click(screen.getByRole('button', { name: 'Conservarla' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(onSubmit).toHaveBeenLastCalledWith(expect.anything(), { kind: 'keep' });
  });

  test('replaces the image with a new one', () => {
    const { onSubmit } = renderForm({ activity });
    const file = png();

    fireEvent.change(screen.getByLabelText('Cambiar imagen'), { target: { files: [file] } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.anything(), { kind: 'replace', file });
  });

  test('clears the optional fields it had when they are emptied', () => {
    const { onSubmit } = renderForm({ activity });
    type('Hora de fin', '');
    type('Hora de inicio', '');
    type('Coste estimado (€)', '');
    type('Categoría', '');

    fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ startTime: null, endTime: null, cost: null, category: null }),
      { kind: 'keep' },
    );
  });
});
