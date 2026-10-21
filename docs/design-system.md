[← Back to README](../README.md)

# Design system

## Direction: transit line map

TripMate is used by a group that follows one plan, so the interface borrows from transit maps: a trip is a
line, and each day or activity is a stop on it. The look is flat and quiet (white surfaces, thin borders, no
shadows); the line is the only bold element on each screen.

## Palette

All values live in `frontend/src/styles/theme.css`. Contrast is calculated with the WCAG formula.

| Token              | Hex       | Role                              | Contrast on `#F5F6F2` |
| ------------------ | --------- | --------------------------------- | --------------------- |
| `--tm-plano`       | `#F5F6F2` | Page background                   | n/a                   |
| `--tm-blanco`      | `#FFFFFF` | Surfaces (cards, forms)           | n/a                   |
| `--tm-tinta`       | `#1D2521` | Text, primary buttons             | 14.5:1                |
| `--tm-linea`       | `#1D5FA8` | The line, links, focus ring       | 5.9:1                 |
| `--tm-corte`       | `#C0262D` | Conflicts and errors (a cut line) | 5.4:1                 |
| `--tm-enlace`      | `#2F7A3B` | Success                           | 4.9:1                 |
| `--tm-via`         | `#5B6660` | Secondary text                    | 5.5:1                 |
| `--tm-linea-suave` | `#E3ECF6` | Soft blue tile behind images      | n/a                   |
| `--tm-via-border`  | `#CFD5CF` | Borders (decorative, not text)    | n/a                   |

## Type

- **Overpass** (400, 600, 800): interface and headings.
- **Overpass Mono** (400): dates, times and amounts, with tabular numbers.
- Scale (rem): 0.875, 1, 1.25, 1.6, 2.2. Headings use weight 800, body uses 400.

## Layout principles

- Follow the wireframes in `docs/images/wireframes/`; this document only defines the look.
- Bootstrap grid, one column at 375px and two from `md`. Content width is a `Container`.
- No shadows. Cards have a 1px border and a 4px radius. Fully round shapes are only used for stops.
- Primary actions are solid ink (`--tm-tinta`); the blue is reserved for the line, links and focus.

- A trip card follows the My Trips wireframe: an inset image tile on the left (soft blue when there is no
  cover) and the info on the right: "Viaje a <destination>", dates, participants and the action buttons. Delete is an outline red button and edit an outline blue one, both
  keeping Bootstrap's fill-on-hover behaviour; edit is only shown to the trip's creator.

- The page background is the fixed photo `frontend/public/trip_background_image.jpg` (cover, fixed), darkened with a grey veil (`--tm-velo`) so the white page title keeps its contrast. Content
  always sits on white surfaces so text keeps its contrast.

## The memorable element: the line

- **My Trips**: the line is not drawn on the trip cards; it appears in the header's underline and the blue
  accents.
- **Trip Details** (planned, B5/B6): the activities of the selected day are stops on a vertical line, and a
  conflict (overlap, outside the dates, over budget) cuts the line with a gap in `--tm-corte`.

## How Bootstrap is themed

1. `bootstrap/dist/css/bootstrap.min.css` is imported first in `src/main.tsx`.
2. `src/styles/theme.css` defines the tokens (the only file with hex values).
3. `src/styles/bootstrap-theme.css` maps the tokens to Bootstrap variables (`--bs-body-bg`, `--bs-primary`,
   `--bs-border-radius`, button, card and alert variables) using `var(--token)`.

Components are never restyled one by one; change the theme files instead. Component CSS files only handle
layout of custom parts.

## Custom components

None yet. Everything uses react-bootstrap; the itinerary line (B5/B6) will be the first custom component.

## What we avoid

Cream background with serif and terracotta; near-black with one acid accent; identical rounded cards with grey
shadows; decorative gradients; all-caps tracked eyebrow labels; "A · B · C" meta strings; arrows on every
button; one word of a headline in another colour; 01/02/03 markers on things that are not a sequence;
entrance animations everywhere; hover animation on every card; Inter/Roboto/system fonts by default;
generic "wanderlust" copy; the stock Bootstrap look (blue primary, grey cards, default navbar and radius).

## Copy rules (Spanish UI)

- Sentence case and plain verbs. A button says what it does ("Guardar viaje", not "Enviar").
- The same name for the same thing across the flow (viaje, actividad, participante).
- Errors say what happened and how to fix it. Empty states invite the next action ("Aún no tienes viajes.
  Crea el primero.").
- User-visible text is written directly in the component that shows it.
