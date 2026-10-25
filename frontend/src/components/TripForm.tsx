import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import type { CoverChange, Trip, TripData } from '../types';
import {
  EMPTY_TRIP_VALUES,
  MAX_DESCRIPTION_LENGTH,
  MAX_DESTINATION_LENGTH,
  MAX_TRIP_NAME_LENGTH,
  toTripData,
  validateTrip,
  valuesFromTrip,
  type TripFieldError,
  type TripFieldErrors,
  type TripFormValues,
} from '../utils/tripForm';
import ImageUpload from './ImageUpload';
import StoredImage from './StoredImage';
import './TripForm.css';

const MESSAGES: Record<keyof TripFormValues, Partial<Record<TripFieldError, string>>> = {
  name: {
    required: 'Escribe el nombre del viaje.',
    tooLong: `El nombre admite como máximo ${MAX_TRIP_NAME_LENGTH} caracteres.`,
  },
  destination: {
    required: 'Escribe el destino del viaje.',
    tooLong: `El destino admite como máximo ${MAX_DESTINATION_LENGTH} caracteres.`,
  },
  startDate: { required: 'Elige la fecha de inicio.' },
  endDate: {
    required: 'Elige la fecha de fin.',
    beforeStart: 'La fecha de fin no puede ser anterior a la de inicio.',
  },
  budget: {
    invalidAmount: 'Escribe un importe válido, con hasta 2 decimales. Por ejemplo, 1200,50.',
    negative: 'El presupuesto no puede ser negativo.',
    tooBig: 'El presupuesto es demasiado alto.',
  },
  description: {
    tooLong: `La descripción admite como máximo ${MAX_DESCRIPTION_LENGTH} caracteres.`,
  },
};

interface TripFormProps {
  /** The trip being edited; without it the form creates a new one. */
  trip?: Trip;
  submitting: boolean;
  /** Why the last save failed, shown above the fields. */
  submitError?: string;
  /** Something the user should know when the form opens, e.g. that the cover failed. */
  notice?: string;
  /** Receives the validated data and what to do with the cover. */
  onSubmit: (data: TripData, cover: CoverChange) => void;
  onCancel: () => void;
}

/** The form to create a trip and to edit one: same fields, same rules, same cover handling. */
export default function TripForm({
  trip,
  submitting,
  submitError,
  notice,
  onSubmit,
  onCancel,
}: Readonly<TripFormProps>) {
  const [values, setValues] = useState<TripFormValues>(
    trip ? valuesFromTrip(trip) : EMPTY_TRIP_VALUES,
  );
  const [errors, setErrors] = useState<TripFieldErrors>({});
  const [cover, setCover] = useState<CoverChange>({ kind: 'keep' });
  /** Changing it empties the file picker, which keeps the chosen file inside. */
  const [pickerKey, setPickerKey] = useState(0);

  const hasCover = !!trip?.coverImageId;

  function message(field: keyof TripFormValues): string | undefined {
    const code = errors[field];
    return code ? MESSAGES[field][code] : undefined;
  }

  function change(field: keyof TripFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  function discardChosenImage() {
    setCover({ kind: 'keep' });
    setPickerKey((key) => key + 1);
  }

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateTrip(values);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    onSubmit(toTripData(values), cover);
  }

  return (
    <Form noValidate onSubmit={handleSubmit}>
      {notice && <Alert variant="warning">{notice}</Alert>}
      {submitError && <Alert variant="danger">{submitError}</Alert>}
      <Form.Group className="mb-3" controlId="trip-name">
        <Form.Label>Nombre del viaje</Form.Label>
        <Form.Control
          type="text"
          value={values.name}
          isInvalid={!!message('name')}
          onChange={(event) => change('name', event.target.value)}
        />
        <Form.Control.Feedback type="invalid">{message('name')}</Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3" controlId="trip-destination">
        <Form.Label>Destino</Form.Label>
        <Form.Control
          type="text"
          value={values.destination}
          isInvalid={!!message('destination')}
          onChange={(event) => change('destination', event.target.value)}
        />
        <Form.Control.Feedback type="invalid">{message('destination')}</Form.Control.Feedback>
      </Form.Group>
      <Row className="mb-3 gy-3">
        <Col md={6}>
          <Form.Group controlId="trip-start-date">
            <Form.Label>Fecha de inicio</Form.Label>
            <Form.Control
              type="date"
              value={values.startDate}
              isInvalid={!!message('startDate')}
              onChange={(event) => change('startDate', event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{message('startDate')}</Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="trip-end-date">
            <Form.Label>Fecha de fin</Form.Label>
            <Form.Control
              type="date"
              value={values.endDate}
              isInvalid={!!message('endDate')}
              onChange={(event) => change('endDate', event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{message('endDate')}</Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Form.Group className="mb-3" controlId="trip-budget">
        <Form.Label>Presupuesto (€)</Form.Label>
        <Form.Control
          type="text"
          inputMode="decimal"
          value={values.budget}
          isInvalid={!!message('budget')}
          onChange={(event) => change('budget', event.target.value)}
        />
        <Form.Text>Opcional. Se usará para avisarte si las actividades lo superan.</Form.Text>
        <Form.Control.Feedback type="invalid">{message('budget')}</Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3" controlId="trip-description">
        <Form.Label>Descripción</Form.Label>
        <Form.Control
          as="textarea"
          rows={4}
          value={values.description}
          isInvalid={!!message('description')}
          onChange={(event) => change('description', event.target.value)}
        />
        <Form.Text>{`Opcional. ${values.description.length}/${MAX_DESCRIPTION_LENGTH}`}</Form.Text>
        <Form.Control.Feedback type="invalid">{message('description')}</Form.Control.Feedback>
      </Form.Group>
      <div className="mb-4">
        {hasCover && cover.kind === 'keep' && (
          <div className="trip-form__cover">
            <StoredImage
              id={trip?.coverImageId ?? undefined}
              alt="Portada actual"
              className="img-fluid rounded"
            />
            <Button
              variant="outline-danger"
              size="sm"
              disabled={submitting}
              onClick={() => setCover({ kind: 'remove' })}
            >
              Quitar portada
            </Button>
          </div>
        )}
        {cover.kind === 'remove' && (
          <p className="trip-form__cover-note">
            {'La portada se quitará al guardar el viaje. '}
            <Button
              variant="link"
              className="p-0 align-baseline"
              onClick={() => setCover({ kind: 'keep' })}
            >
              Conservarla
            </Button>
          </p>
        )}
        {cover.kind !== 'remove' && (
          <ImageUpload
            key={pickerKey}
            label={hasCover ? 'Cambiar portada' : 'Portada'}
            onSelect={(file) => setCover({ kind: 'replace', file })}
          />
        )}
        {cover.kind === 'replace' && (
          <Button
            variant="link"
            className="p-0 mt-2"
            disabled={submitting}
            onClick={discardChosenImage}
          >
            Descartar la imagen elegida
          </Button>
        )}
      </div>
      <div className="d-flex flex-wrap gap-2">
        <Button type="submit" disabled={submitting}>
          {submitting ? 'Guardando…' : trip ? 'Guardar cambios' : 'Crear viaje'}
        </Button>
        <Button variant="outline-secondary" disabled={submitting} onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </Form>
  );
}
