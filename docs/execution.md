[← Back to README](../README.md)

# Execution

This page explains how to run TripMate from the published Docker images, without cloning the repository. To run it from the source code and edit it, see [Running and editing the code](development-guide.md#running-and-editing-the-code).

## Index

- [Requirements](#requirements)
- [Running the application](#running-the-application)
- [Accessing the application](#accessing-the-application)
- [Sample data](#sample-data)
- [Running with and without sample data](#running-with-and-without-sample-data)
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

Open **https://localhost/** in the browser. The certificate is self-signed, so the browser shows a warning: accept it to continue (in Chrome, _Advanced → Proceed to localhost_). The REST API is at https://localhost/api/v1/ and is documented [here](https://raw.githack.com/codeurjc-students/2026-ViajesRuben/main/docs/api/api-docs.html).

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

4. Run the application:

   ```bash
   export ADMIN_PASSWORD_HASH='<hash>' JWT_SECRET='<key>' MYSQL_ROOT_PASSWORD='<password>' SAMPLE_DATA=false
   docker compose -f oci://docker.io/rubenruiz7/tripmate-compose:latest up -d --yes
   ```

   Use single quotes: a BCrypt hash contains `$` characters that the shell would expand otherwise.

5. Open `https://<address of the server>/`. The image includes the same self-signed certificate as the development environment, so the browser warns about it. For a real domain, provide a certificate: create a PKCS12 keystore with it, mount it in the `app` container with a Compose override file and set `SSL_KEYSTORE=file:/path/in/the/container/keystore.p12` and `SSL_KEYSTORE_PASSWORD`.

Every variable, with its default value, is described in the [development guide](development-guide.md#docker-deployment). The data of the database is kept in the Docker volume `db-data`, so back it up (or use `down` without `-v`) before upgrading to another version.
