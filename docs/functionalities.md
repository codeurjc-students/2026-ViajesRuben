[← Back to README](../README.md)

# Detailed features

Each feature has an identifier so that it can be traced to its automated tests.

## Basic functionality (version 0.1)

| ID  | Feature                                                                                                                                                  | User type                 |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| B1  | Browse the public home page with example itineraries (preview only; sample trips created by the administrator).                                          | Anonymous                 |
| B2  | Register a new account and log in.                                                                                                                       | Anonymous                 |
| B3  | Log out, and view and edit one's own profile (name, email, password, avatar).                                                                            | Registered                |
| B4  | Create, view, edit, and delete one's own trips, including a cover image.                                                                                 | Registered (trip creator) |
| B5  | Create, view, edit, and delete activities in a trip's itinerary, including an image, and browse the itinerary day by day.                                | Registered (trip creator) |
| B6  | Conflict detection: overlapping activities on the same day, activities outside the trip's date range, and total activity cost exceeding the trip budget. | Registered (trip members) |
| B7  | User management: list and search users, edit their account details, and block or unblock accounts.                                                       | Administrator             |
| B8  | Trip moderation: list and search all trips, view any trip's details, and delete inappropriate trips.                                                     | Administrator             |

## Intermediate functionality (version 0.2)

| ID  | Feature                                                                                                                                        | User type                      |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| I1  | Invite users to a trip by email; the invited person receives an email with a link to respond.                                                  | Trip creator                   |
| I2  | Accept or reject an invitation, from the email link or from the pending invitations section in "My Trips".                                     | Registered (invited user)      |
| I3  | Manage participants: the creator can view and remove participants; a participant can leave the trip.                                           | Trip creator, trip participant |
| I4  | Collaborative itinerary: participants create activities and edit or delete their own; the creator can edit or delete any activity of the trip. | Trip creator, trip participant |
| I5  | Spending charts on the trip page (spending by category against the budget, cumulative spending per day).                                       | Trip creator, trip participant |
| I6  | Platform metrics dashboard (total users, trips, activities, and, from version 1.0, AI requests).                                               | Administrator                  |

## Advanced functionality (version 1.0)

| ID  | Feature                                                                                                                                                                                                                                                                      | User type                      |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| A1  | Generation of itinerary proposals via AI, based on destination, dates, budget, and the group's preferences, with manual review and acceptance before the proposed activities are added.                                                                                      | Trip creator, trip participant |
| A2  | Reorganization of an existing itinerary via AI, taking already-detected conflicts into account, with manual review and acceptance.                                                                                                                                           | Trip creator                   |
| A3  | Connect a Telegram account from the user profile (through a one-time link to the TripMate bot) and disconnect it.                                                                                                                                                            | Registered                     |
| A4  | Create the trip's Telegram group with a single button: Telegram opens to create the group with the TripMate bot as administrator, and the bot links the group to the trip.                                                                                                   | Trip creator                   |
| A5  | Invitations through Telegram: invited users with a connected account receive a private message from the bot with the group invite link and a link to the trip. The bot approves join requests only from invited users or participants, and removes users who leave the trip. | Trip creator, invited user     |
| A6  | Notifications in the trip's Telegram group when the dates, the itinerary, or the participants change.                                                                                                                                                                        | Trip participants              |

The Telegram integration is optional for users: every feature of the trip works without a Telegram account.
