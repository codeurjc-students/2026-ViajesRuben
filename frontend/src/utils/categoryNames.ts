import type { ActivityCategory } from '../types';

/** The Spanish name of every activity category, for the card, the form's select and the charts. */
export const CATEGORY_NAMES: Record<ActivityCategory, string> = {
  SIGHTSEEING: 'Visita',
  FOOD: 'Comida',
  TRANSPORT: 'Transporte',
  ACCOMMODATION: 'Alojamiento',
  LEISURE: 'Ocio',
  OTHER: 'Otra',
};

/** What the activities without a category are called in the spending chart. */
export const NO_CATEGORY_NAME = 'Sin categoría';
