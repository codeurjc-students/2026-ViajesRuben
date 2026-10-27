import { useState, type SubmitEvent } from 'react';
import Alert from 'react-bootstrap/Alert';
import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Row from 'react-bootstrap/Row';
import type { Activity, ActivityCategory, ActivityData, CoverChange, Trip } from '../types';
import {
  ACTIVITY_CATEGORIES,
  MAX_ACTIVITY_DESCRIPTION_LENGTH,
  MAX_ACTIVITY_NAME_LENGTH,
  MAX_LOCATION_LENGTH,
  emptyActivityValues,
  toActivityData,
  validateActivity,
  valuesFromActivity,
  type ActivityFieldError,
  type ActivityFieldErrors,
  type ActivityFormValues,
} from '../utils/activityForm';
import CategoryName from './CategoryName';
import ImageUpload from './ImageUpload';
import StoredImage from './StoredImage';
import './ActivityForm.css';

const MESSAGES: Record<keyof ActivityFormValues, Partial<Record<ActivityFieldError, string>>> = {
  name: {
    required: 'Escribe el nombre de la actividad.',
    tooLong: `El nombre admite como máximo ${MAX_ACTIVITY_NAME_LENGTH} caracteres.`,
  },
  category: {},
  date: {
    required: 'Elige el día de la actividad.',
    outsideTrip: 'El día debe estar dentro de las fechas del viaje.',
  },
  startTime: {},
  endTime: {
    endBeforeStart: 'La hora de fin debe ser posterior a la de inicio.',
    endWithoutStart: 'Indica también la hora de inicio.',
  },
  cost: {
    invalidAmount: 'Escribe un importe válido, con hasta 2 decimales. Por ejemplo, 16,50.',
    negative: 'El coste no puede ser negativo.',
    tooBig: 'El coste es demasiado alto.',
  },
  location: {
    tooLong: `La ubicación admite como máximo ${MAX_LOCATION_LENGTH} caracteres.`,
  },
  description: {
    tooLong: `La descripción admite como máximo ${MAX_ACTIVITY_DESCRIPTION_LENGTH} caracteres.`,
  },
};

interface ActivityFormProps {
  /** The trip the activity belongs to: its dates limit the day that can be chosen. */
  trip: Trip;
  /** The activity being edited; without it the form creates a new one. */
  activity?: Activity;
  /** The day a new activity starts on, usually the day the user was looking at. */
  initialDate?: string;
  submitting: boolean;
  /** Why the last save failed, shown above the fields. */
  submitError?: string;
  /** Something the user should know when the form opens, e.g. that the image failed. */
  notice?: string;
  /** Receives the validated data and what to do with the image. */
  onSubmit: (data: ActivityData, image: CoverChange) => void;
  onCancel: () => void;
}

/** The form to create an activity and to edit one: same fields, same rules, same image handling. */
export default function ActivityForm({
  trip,
  activity,
  initialDate,
  submitting,
  submitError,
  notice,
  onSubmit,
  onCancel,
}: Readonly<ActivityFormProps>) {
  const [values, setValues] = useState<ActivityFormValues>(
    activity ? valuesFromActivity(activity) : emptyActivityValues(initialDate),
  );
  const [errors, setErrors] = useState<ActivityFieldErrors>({});
  const [image, setImage] = useState<CoverChange>({ kind: 'keep' });
  /** Changing it empties the file picker, which keeps the chosen file inside. */
  const [pickerKey, setPickerKey] = useState(0);

  const hasImage = !!activity?.imageId;

  function message(field: keyof ActivityFormValues): string | undefined {
    const code = errors[field];
    return code ? MESSAGES[field][code] : undefined;
  }

  function change(field: keyof ActivityFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
  }

  function discardChosenImage() {
    setImage({ kind: 'keep' });
    setPickerKey((key) => key + 1);
  }

  function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validateActivity(values, trip);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    onSubmit(toActivityData(values), image);
  }

  return (
    <Form noValidate onSubmit={handleSubmit}>
      {notice && <Alert variant="warning">{notice}</Alert>}
      {submitError && <Alert variant="danger">{submitError}</Alert>}
      <Form.Group className="mb-3" controlId="activity-name">
        <Form.Label>Nombre de la actividad</Form.Label>
        <Form.Control
          type="text"
          value={values.name}
          isInvalid={!!message('name')}
          onChange={(event) => change('name', event.target.value)}
        />
        <Form.Control.Feedback type="invalid">{message('name')}</Form.Control.Feedback>
      </Form.Group>
      <Row className="mb-3 gy-3">
        <Col md={6}>
          <Form.Group controlId="activity-category">
            <Form.Label>Categoría</Form.Label>
            <Form.Select
              value={values.category}
              onChange={(event) => change('category', event.target.value as ActivityCategory | '')}
            >
              <option value="">Sin categoría</option>
              {ACTIVITY_CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  <CategoryName category={category} />
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="activity-date">
            <Form.Label>Día</Form.Label>
            <Form.Control
              type="date"
              min={trip.startDate}
              max={trip.endDate}
              value={values.date}
              isInvalid={!!message('date')}
              onChange={(event) => change('date', event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{message('date')}</Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Row className="mb-3 gy-3">
        <Col md={6}>
          <Form.Group controlId="activity-start-time">
            <Form.Label>Hora de inicio</Form.Label>
            <Form.Control
              type="time"
              value={values.startTime}
              onChange={(event) => change('startTime', event.target.value)}
            />
            <Form.Text>Opcional. Sin hora, la actividad dura todo el día.</Form.Text>
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group controlId="activity-end-time">
            <Form.Label>Hora de fin</Form.Label>
            <Form.Control
              type="time"
              value={values.endTime}
              isInvalid={!!message('endTime')}
              onChange={(event) => change('endTime', event.target.value)}
            />
            <Form.Control.Feedback type="invalid">{message('endTime')}</Form.Control.Feedback>
          </Form.Group>
        </Col>
      </Row>
      <Form.Group className="mb-3" controlId="activity-cost">
        <Form.Label>Coste estimado (€)</Form.Label>
        <Form.Control
          type="text"
          inputMode="decimal"
          value={values.cost}
          isInvalid={!!message('cost')}
          onChange={(event) => change('cost', event.target.value)}
        />
        <Form.Text>Opcional.</Form.Text>
        <Form.Control.Feedback type="invalid">{message('cost')}</Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3" controlId="activity-location">
        <Form.Label>Ubicación</Form.Label>
        <Form.Control
          type="text"
          value={values.location}
          isInvalid={!!message('location')}
          onChange={(event) => change('location', event.target.value)}
        />
        <Form.Control.Feedback type="invalid">{message('location')}</Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3" controlId="activity-description">
        <Form.Label>Descripción</Form.Label>
        <Form.Control
          as="textarea"
          rows={4}
          value={values.description}
          isInvalid={!!message('description')}
          onChange={(event) => change('description', event.target.value)}
        />
        <Form.Text>{`Opcional. ${values.description.length}/${MAX_ACTIVITY_DESCRIPTION_LENGTH}`}</Form.Text>
        <Form.Control.Feedback type="invalid">{message('description')}</Form.Control.Feedback>
      </Form.Group>
      <div className="mb-4">
        {hasImage && image.kind === 'keep' && (
          <div className="activity-form__image">
            <StoredImage
              id={activity?.imageId ?? undefined}
              alt="Imagen actual"
              className="img-fluid rounded"
              fallback={null}
            />
            <Button
              variant="outline-danger"
              size="sm"
              disabled={submitting}
              onClick={() => setImage({ kind: 'remove' })}
            >
              Quitar imagen
            </Button>
          </div>
        )}
        {image.kind === 'remove' && (
          <p className="activity-form__image-note">
            {'La imagen se quitará al guardar la actividad. '}
            <Button
              variant="link"
              className="p-0 align-baseline"
              onClick={() => setImage({ kind: 'keep' })}
            >
              Conservarla
            </Button>
          </p>
        )}
        {image.kind !== 'remove' && (
          <ImageUpload
            key={pickerKey}
            label={hasImage ? 'Cambiar imagen' : 'Imagen'}
            onSelect={(file) => setImage({ kind: 'replace', file })}
          />
        )}
        {image.kind === 'replace' && (
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
          {submitting ? 'Guardando…' : activity ? 'Guardar cambios' : 'Crear actividad'}
        </Button>
        <Button variant="outline-secondary" disabled={submitting} onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </Form>
  );
}
