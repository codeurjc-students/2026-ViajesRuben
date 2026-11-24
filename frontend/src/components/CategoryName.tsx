import type { ActivityCategory } from '../types';
import { CATEGORY_NAMES } from '../utils/categoryNames';

/** The Spanish name of an activity category, for the card and for the form's select. */
export default function CategoryName({ category }: Readonly<{ category: ActivityCategory }>) {
  return CATEGORY_NAMES[category];
}
