import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { TripSpending } from '../types';
import { formatBudget, formatShortDay } from '../utils/tripFormat';
import './SpendingCharts.css';

/** Beyond this many days the dots would touch each other, so only the line is drawn. */
const MAX_DAYS_WITH_DOTS = 31;
const CHART_HEIGHT = 260;

/**
 * Cumulative spending per day (I5): a line over every day of the trip, 0 included, with the
 * budget as a reference line when the trip has one. The server gives the running sum; this only
 * draws it. A hidden table gives the same figures to screen readers.
 */
export default function CumulativeSpendingChart({
  spending,
}: Readonly<{ spending: TripSpending }>) {
  const { budget, byDay } = spending;

  return (
    <figure className="spending-chart" aria-labelledby="spending-by-day-title">
      <figcaption id="spending-by-day-title" className="spending-chart__title">
        Gasto acumulado por día
      </figcaption>
      <div aria-hidden="true" style={{ height: CHART_HEIGHT }}>
        <ResponsiveContainer
          width="100%"
          height="100%"
          initialDimension={{ width: 600, height: CHART_HEIGHT }}
        >
          <LineChart data={byDay} margin={{ top: 8, right: 16, bottom: 4, left: 8 }}>
            <CartesianGrid stroke="var(--tm-via-border)" strokeDasharray="3 3" />
            <XAxis
              dataKey="date"
              tickFormatter={(date: string) => formatShortDay(date)}
              tick={{ fill: 'var(--tm-via)', fontSize: 12 }}
              minTickGap={16}
            />
            <YAxis
              // The budget always fits in the chart, even when the spending is far below it.
              domain={[0, (dataMax: number) => Math.max(dataMax, budget ?? 0)]}
              tickFormatter={(value: number) => formatBudget(value)}
              tick={{ fill: 'var(--tm-via)', fontSize: 12 }}
              width={84}
            />
            <Tooltip
              labelFormatter={(date) => formatShortDay(String(date))}
              formatter={(value) => formatBudget(Number(value))}
            />
            {budget !== null && (
              <ReferenceLine
                y={budget}
                stroke="var(--tm-corte)"
                strokeDasharray="6 4"
                label={{ value: 'Presupuesto', position: 'insideTopLeft', fill: 'var(--tm-corte)' }}
              />
            )}
            <Line
              type="monotone"
              dataKey="cumulative"
              name="Acumulado"
              stroke="var(--tm-linea)"
              strokeWidth={2}
              dot={byDay.length <= MAX_DAYS_WITH_DOTS}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <table className="visually-hidden">
        <caption>Gasto acumulado por día</caption>
        <thead>
          <tr>
            <th scope="col">Día</th>
            <th scope="col">Gastado</th>
            <th scope="col">Acumulado</th>
          </tr>
        </thead>
        <tbody>
          {byDay.map((day) => (
            <tr key={day.date}>
              <th scope="row">{formatShortDay(day.date)}</th>
              <td>{formatBudget(day.total)}</td>
              <td>{formatBudget(day.cumulative)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
