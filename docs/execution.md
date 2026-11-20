[← Back to README](../README.md)

# Execution

This page explains how to run TripMate from the published Docker images, without cloning the repository. To run it from the source code and edit it, see [Running and editing the code](development-guide.md#running-and-editing-the-code).

## Index

- [Requirements](#requirements)
- [Running the application](#running-the-application)
- [Accessing the application](#accessing-the-application)
- [Sample data](#sample-data)
- [Running with and without sample data](#running-with-and-without-sample-data)
- [Emails](#emails)
  - [Seeing the emails: the test inbox](#seeing-the-emails-the-test-inbox)
  - [Email settings](#email-settings)
  - [Sending real emails with Gmail](#sending-real-emails-with-gmail)
  - [Links that work from another device](#links-that-work-from-another-device)
- [Stopping and resetting](#stopping-and-resetting)
- [Deploying on an external server](#deploying-on-an-external-server)

## Requirements

The only requirement is Docker with Docker Compose **2.34.0 or later** (older versions cannot run a Compose file from a registry), and port **443** free on the machine.

| System        | What to install                                                                                                                                                                                                       |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Windows / Mac | [Docker Desktop](https://docs.docker.com/desktop/) (it includes Docker Compose). Start it before running the commands                                                                                                 |
| Linux         | [Docker Engine](https://docs.docker.com/engine/install/) and the [Docker Compose plugin](https://docs.docker.com/compose/install/linux/). On Linux, `docker` commands may need `sudo`, or add your user to the `docker` group |

Check the version with `docker compose version`.

## Running the application

The Compose file is published on DockerHub as an OCI artifact ([`rubenruiz7/tripmate-compose`](https://hub.docker.com/r/rubenruiz7/tripmate-compose)). It starts the application (image [`rubenruiz7/tripmate`](https://hub.docker.com/r/rubenruiz7/tripmate), which contains the web client and the server) and a MySQL database. Nothing else has to be downloaded.

**Last release (`latest`):**

```bash
docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes
```

**A specific version (`0.1`):**

```bash
docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:0.1 up -d --yes
```

Compose lists the variables of the file and asks for confirmation before running an application downloaded from a registry; `--yes` answers yes. Remove it to review the variables first. Every release is published with the tags `x.y.0`, `x.y` and `latest`, for example `0.1.0`, `0.1` and `latest`. `dev` is the last version of the `main` branch.

The first start downloads the images and initializes MySQL, so it can take one or two minutes. To see when the application is ready:

```bash
docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest logs -f app
```

**The default configuration works as it is.** All the settings are environment variables with a default value, so no file has to be created or edited. The defaults are for development and demos only; see [Deploying on an external server](#deploying-on-an-external-server) for what to change outside your own machine.

## Accessing the application

Open **https://localhost/** in the browser. The certificate is self-signed, so the browser shows a warning: accept it to continue (in Chrome, _Advanced → Proceed to localhost_). The REST API is at https://localhost/api/v1/ and is documented [here](https://raw.githack.com/codeurjc-students/2026-ViajesRuben/main/docs/api/api-docs.html). The emails that the application sends (invitations) are caught by a test inbox at http://localhost:8025, see [Emails](#emails).

| Account       | Email                 | Password    | Role          |
| ------------- | --------------------- | ----------- | ------------- |
| Administrator | `admin@tripmate.com`  | `admin`     | Administrator |
| Ana García    | `ana@tripmate.com`    | `Demo-2026` | Regular user  |
| Carlos Ruiz   | `carlos@tripmate.com` | `Demo-2026` | Regular user  |
| Laura Pérez   | `laura@tripmate.com`  | `Demo-2026` | Regular user  |

Without logging in you see the public home page. Any person can also register their own account at `/register`.

## Sample data

While `SAMPLE_DATA` is `true` (the default), the application creates at start-up, if they do not exist:

- **Three regular users** with the trips shown below. All their passwords are the same (`SAMPLE_USERS_PASSWORD`, `Demo-2026` by default).

  | User   | Trips                                                                                                           |
  | ------ | --------------------------------------------------------------------------------------------------------------- |
  | Ana    | 12 (so "My trips" has a second page of "Más resultados"). "Lisbon getaway" has 7 activities and "Summer in Rome" 4 |
  | Carlos | 3, one of them ("Andalusia route") with 3 activities                                                            |
  | Laura  | 2, one of them ("Weekend in Porto") with 4 activities, with its third day left empty                            |

- **Three example trips** created by the administrator (Rome, the Maldives and Tokyo), with budget, description, cover photo and activities on every day. They are the itineraries that the public home page shows to everyone.

A trip that already has activities is never modified, so restarting the application does not duplicate or overwrite anything.

## Running with and without sample data

With sample data (default): run the commands above.

Without sample data, for an empty application that only has the administrator:

```bash
SAMPLE_DATA=false docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes
```

On Windows PowerShell: `$env:SAMPLE_DATA="false"; docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes`.

`SAMPLE_DATA` is read when the application starts and it only adds data. To go from sample data to an empty database, delete the data first (see below). The public home page has no example itineraries without sample data, because they are trips of the administrator.

## Emails

When the creator of a trip invites someone (the "Invitaciones" section of the trip page), the application sends an email in Spanish to that address: who invites, which trip, and a link `{APP_PUBLIC_URL}/invitations/{token}` to answer the invitation. The page behind the link is a placeholder until answering invitations is built, but the link already identifies the invitation.

The invitation is saved first and the email is sent afterwards, in the background. If the email cannot be sent (the mail server is down, the password is wrong, the address is refused) the invitation is still created and stays pending, the error is written to the log of the application (`... could not be sent (MailAuthenticationException)`, with the invitation number and the kind of error, never the address or the link) and the email is **not retried**. To see the log: `docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest logs app`.

### Seeing the emails: the test inbox

You do not need an email account to try invitations. The Compose file also starts **Mailpit**, a test mail server that catches every email the application sends: nothing leaves your machine, so no real person receives anything. Open its inbox at **http://localhost:8025** (port 8025 must be free). The inbox is published only on your own computer (`127.0.0.1`), because the emails contain the links of the invitations: on a server you cannot open it from another machine, so use a real SMTP server there (see below).

To try it, log in as Ana (`ana@tripmate.com`, `Demo-2026`), open one of her trips, type any email in "Invitar por email" and press "Enviar invitación". The invitation appears in the list as "Pendiente" and the email appears in the Mailpit inbox in about a second. The link in it opens a placeholder page of the application (the default `APP_PUBLIC_URL` is `https://localhost`).

Mailpit keeps the emails only while its container exists: `down` deletes them (see [Stopping and resetting](#stopping-and-resetting)).

### Email settings

All of them are environment variables with a default, and the defaults send everything to Mailpit:

| Variable             | Default                                | Description                                                                                                                                 |
| -------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `MAIL_HOST`          | `mailpit`                              | SMTP server. `smtp.gmail.com` for Gmail                                                                                                     |
| `MAIL_PORT`          | `1025`                                 | SMTP port. `587` for Gmail                                                                                                                  |
| `MAIL_USERNAME`      | empty                                  | Account that sends the emails (the full address in Gmail)                                                                                   |
| `MAIL_PASSWORD`      | empty                                  | Password of the account. In Gmail, an _app password_, never the normal password                                                             |
| `MAIL_SMTP_AUTH`     | `false`                                | `true` if the server asks for `MAIL_USERNAME` and `MAIL_PASSWORD`                                                                           |
| `MAIL_SMTP_STARTTLS` | `false`                                | `true` to encrypt the connection with STARTTLS                                                                                              |
| `MAIL_FROM`          | `TripMate <no-reply@tripmate.local>`   | Sender shown in the emails. Gmail replaces it with the account itself, so use the same address                                              |
| `APP_PUBLIC_URL`     | `https://localhost`                    | Address that the links in the emails start with. It must be reachable from where the email is read (see [below](#links-that-work-from-another-device)) |

### Sending real emails with Gmail

To send the invitations to real addresses through Gmail:

1. **Use a dedicated Google account** for TripMate, not your personal one: its password will be stored in a file on your machine, and Gmail limits how many emails an account may send per day (about 500 for a normal account).
2. **Turn on 2-step verification** in that account (Google Account → Security). Google only offers app passwords when it is on.
3. **Create an app password**: in the same Security page, search for _App passwords_, create one named `TripMate` and copy the 16 characters it shows (without the spaces). It is shown only once. It works only for this use and can be revoked at any time without changing the account password.
4. **Put the values in a `.env` file** in the folder where you run the Docker command. This file holds a secret: **never commit it**. The `.gitignore` of the repository ignores `.env` and `.env.*`; if you work in another folder or repository, check that it is ignored before the first commit (`git check-ignore -v .env` prints the rule that ignores it).

   ```properties
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-tripmate-account@gmail.com
   MAIL_PASSWORD=xxxxxxxxxxxxxxxx
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS=true
   MAIL_FROM=TripMate <your-tripmate-account@gmail.com>
   APP_PUBLIC_URL=https://localhost
   ```

   Port 587 with STARTTLS is the one Gmail uses for applications. Replace the account and the password with yours; do not use quotes.

5. **Start the application with that file** (it needs a version of the application that has the invitation emails, 0.2 or later):

   ```bash
   docker compose --env-file .env -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes
   ```

   `--env-file` makes Compose read the file whatever the Compose file is, so do not rely on it being picked up by itself. The Mailpit container still starts, but nothing is sent to it while `MAIL_HOST` is `smtp.gmail.com`.

6. **Check it**: invite an address you can read. If the email does not arrive within a minute, look at the log of the application (command above): `MailAuthenticationException` means that Gmail refused the account or the app password (a wrong password, 2-step verification off, or the password was revoked), and `MailSendException` that the server could not be reached (no internet, a firewall blocking port 587, or a wrong `MAIL_HOST` or `MAIL_PORT`). Also look in the spam folder.

The values only have to be in the environment when the application starts: after changing the file, run the same command again to recreate the `app` container.

### Links that work from another device

The link of an email starts with `APP_PUBLIC_URL`, and the default `https://localhost` means "this computer" for whoever opens it. The link therefore works from the same computer where TripMate runs, but **from another device (a phone, another computer) it only works once `APP_PUBLIC_URL` points to a public address** that the device can reach, for example `https://tripmate.example.com` or the public address of the server (see [Deploying on an external server](#deploying-on-an-external-server)), with port 443 open. Set it in the `.env` file or in the environment before the command, without a trailing path:

```properties
APP_PUBLIC_URL=https://tripmate.example.com
```

The link of an invitation is written when it is created and the email is not sent again, so set `APP_PUBLIC_URL` before inviting: an invitation created with the default address keeps a link to `localhost`. The certificate of the application is self-signed unless you provide your own, so the browser also warns about it when it opens the link.

## Stopping and resetting

```bash
# Stop the application, keeping the data (the next "up" finds the same data)
docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest down

# Stop and delete all the data (users, trips and images)
docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest down -v
```

## Deploying on an external server

The same commands run the application on any server with Docker, for example a Linux virtual machine.

1. Install Docker Engine and the Docker Compose plugin (see [Requirements](#requirements)).
2. Open port **443** (TCP) in the firewall of the server, and make sure nothing else uses it.
3. Replace the development secrets, because the defaults are public. Set them as environment variables before the command:

   | Variable              | What to set                                                                                                                                     |
   | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
   | `ADMIN_PASSWORD_HASH` | BCrypt hash of the administrator password. Create it with `docker run --rm httpd:2 htpasswd -bnBC 10 "" 'my-password' \| tr -d ':\n'`           |
   | `JWT_SECRET`          | A new key, with `openssl rand -base64 32`                                                                                                       |
   | `MYSQL_ROOT_PASSWORD` | A private password for the database                                                                                                             |
   | `SAMPLE_DATA`         | `false`, so there are no accounts with a known password                                                                                         |
   | `ADMIN_EMAIL`         | Optional: the email of the administrator                                                                                                        |
   | `APP_PUBLIC_URL`      | The public address of the server, for example `https://tripmate.example.com`: the links in the invitation emails start with it                  |
   | `MAIL_*`              | Optional: the SMTP server that sends the invitation emails, instead of the test inbox. See [Emails](#emails)                                    |

4. Run the application:

   ```bash
   export ADMIN_PASSWORD_HASH='<hash>' JWT_SECRET='<key>' MYSQL_ROOT_PASSWORD='<password>' SAMPLE_DATA=false
   docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes
   ```

   Use single quotes: a BCrypt hash contains `$` characters that the shell would expand otherwise.

5. Open `https://<address of the server>/`. The image includes the same self-signed certificate as the development environment, so the browser warns about it. For a real domain, provide a certificate: create a PKCS12 keystore with it, mount it in the `app` container with a Compose override file and set `SSL_KEYSTORE=file:/path/in/the/container/keystore.p12` and `SSL_KEYSTORE_PASSWORD`.

Every variable, with its default value, is described in the [development guide](development-guide.md#docker-deployment). The data of the database is kept in the Docker volume `db-data`, so back it up (or use `down` without `-v`) before upgrading to another version.
