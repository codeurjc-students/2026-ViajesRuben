import Badge from 'react-bootstrap/Badge';
import ProgressBar from 'react-bootstrap/ProgressBar';
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { TripSpending } from '../types';
import { CATEGORY_NAMES, NO_CATEGORY_NAME } from '../utils/categoryNames';
import { formatBudget } from '../utils/tripFormat';
import './SpendingCharts.css';

/** Room for each bar plus the axis, so many categories never squash the bars. */
const BAR_HEIGHT = 44;
const AXIS_HEIGHT = 36;

/** How much of the budget is spent, 0 to 100 with one decimal; a budget of 0 is full at any cost. */
function filledPercent(totalCost: number, budget: number): number {
  if (budget <= 0) return totalCost > 0 ? 100 : 0;
  return Math.min(100, Math.round((totalCost / budget) * 1000) / 10);
}

/**
 * Spending by category (I5): a bar per category with some cost, and below it the whole cost
 * against the budget, so it is clear at a glance whether the trip goes over it. The budget is
 * not drawn on the category axis: one category is usually far below the whole budget and a line
 * there would squash the bars. A hidden table gives the same figures to screen readers.
 */
export default function SpendingByCategoryChart({
  spending,
}: Readonly<{ spending: TripSpending }>) {
  const rows = spending.byCategory.map((row) => ({
    name: row.category ? CATEGORY_NAMES[row.category] : NO_CATEGORY_NAME,
    total: row.total,
  }));
  const { budget, totalCost } = spending;
  const over = budget !== null && totalCost > budget;
  const percent = budget === null ? 0 : filledPercent(totalCost, budget);

  return (
    <figure className="spending-chart" aria-labelledby="spending-by-category-title">
      <figcaption id="spending-by-category-title" className="spending-chart__title">
        Gasto por categoría
      </figcaption>
      <div aria-hidden="true" style={{ height: rows.length * BAR_HEIGHT + AXIS_HEIGHT }}>
        <ResponsiveContainer
          width="100%"
          height="100%"
          initialDimension={{ width: 600, height: 300 }}
        >
          <BarChart
            data={rows}
            layout="vertical"
            margin={{ top: 4, right: 16, bottom: 4, left: 8 }}
          >
            <XAxis
              type="number"
              tickFormatter={(value: number) => formatBudget(value)}
              tick={{ fill: 'var(--tm-via)', fontSize: 12 }}
            />
            <YAxis
              type="category"
              dataKey="name"
              width={96}
              tick={{ fill: 'var(--tm-tinta)', fontSize: 13 }}
            />
            <Tooltip formatter={(value) => formatBudget(Number(value))} />
            <Bar dataKey="total" name="Gastado" fill="var(--tm-linea)" isAnimationActive={false} />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <table className="visually-hidden">
        <caption>Gasto por categoría</caption>
        <thead>
          <tr>
            <th scope="col">Categoría</th>
            <th scope="col">Gastado</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.name}>
              <th scope="row">{row.name}</th>
              <td>{formatBudget(row.total)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="spending-chart__total">
        {budget === null ? (
          <p className="spending-chart__figures">
            Total gastado: <strong>{formatBudget(totalCost)}</strong>
          </p>
        ) : (
          <>
            <p className="spending-chart__figures">
              Gastado <strong>{formatBudget(totalCost)}</strong> de {formatBudget(budget)}
              {over && (
                <Badge bg="danger" className="ms-2">
                  Presupuesto superado
                </Badge>
              )}
            </p>
            <ProgressBar
              now={percent}
              variant={over ? 'danger' : 'primary'}
              aria-label="Gasto total frente al presupuesto"
            />
            {over && (
              <p className="spending-chart__over">
                {`Te pasas ${formatBudget(totalCost - budget)} del presupuesto.`}
              </p>
            )}
          </>
        )}
      </div>
    </figure>
  );
}
