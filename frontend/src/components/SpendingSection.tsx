import Card from 'react-bootstrap/Card';
import type { TripSpending } from '../types';
import SpendingByCategoryChart from './SpendingByCategoryChart';
import './SpendingSection.css';

/**
 * The trip's "Gastos" section (I5), for its creator and its participants: spending by category
 * against the budget. `spending` is undefined while it loads; `failed` says the server could not
 * give it. It is not part of the public preview, which never shows costs or members.
 */
export default function SpendingSection({
  spending,
  failed,
}: Readonly<{ spending?: TripSpending; failed: boolean }>) {
  const hasCosts = spending !== undefined && spending.byCategory.length > 0;

  return (
    <Card as="section" className="spending" aria-labelledby="spending-title">
      <h2 id="spending-title" className="h5 spending__title">
        Gastos
      </h2>
      {failed && !spending && (
        <p className="spending__message" role="alert">
          No se pudieron cargar los gastos.
        </p>
      )}
      {!failed && !spending && <output className="d-block">Cargando gastos…</output>}
      {spending && !hasCosts && (
        <p className="spending__message">
          Aún no hay gastos. Añade un coste a una actividad para ver los gráficos.
        </p>
      )}
      {spending && hasCosts && <SpendingByCategoryChart spending={spending} />}
    </Card>
  );
}
