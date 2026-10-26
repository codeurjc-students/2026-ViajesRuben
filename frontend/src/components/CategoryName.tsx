import type { ActivityCategory } from '../types';

const NAMES: Record<ActivityCategory, string> = {
  SIGHTSEEING: 'Visita',
  FOOD: 'Comida',
  TRANSPORT: 'Transporte',
  ACCOMMODATION: 'Alojamiento',
  LEISURE: 'Ocio',
  OTHER: 'Otra',
};

/** The Spanish name of an activity category, for the card and for the form's select. */
export default function CategoryName({ category }: Readonly<{ category: ActivityCategory }>) {
  return NAMES[category];
}
