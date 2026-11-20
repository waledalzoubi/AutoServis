# AutoServis Oracle Cloud deployment

This deployment runs the Spring app, PostgreSQL, and Caddy HTTPS proxy on one Oracle VM. PostgreSQL and HTTPS certificates use Docker volumes so they survive app container updates. The Android/iPhone experience stays the existing installable web app.

## Important: preserve the current data

Do not point customers to this deployment until the current Render database has been identified and backed up. This Compose file creates a new PostgreSQL database; it does not copy Render data automatically. Restore a verified backup before switching the public link. Keep Render running until the new URL and migrated records have been checked.

The repository contains a small local H2 file under `data/`, but the production Render service may use a separate database configured in its environment. Do not assume the checked-in file is the latest production data.

## Oracle VM requirements

- Use an Always Free eligible Ampere A1 VM with Ubuntu 24.04 or Oracle Linux ARM64.
- Allocate only Always Free eligible compute and storage resources.
- Assign a reserved public IPv4 address.
- Allow inbound TCP ports 22 (SSH, restrict to your IP if possible), 80, and 443 in both the Oracle network security rules and the VM firewall.
- Install Docker Engine and the Docker Compose plugin. Do not expose PostgreSQL port 5432 publicly.
- Choose a hostname that points to the VM IP and supports HTTPS. A free DuckDNS hostname is one option; set its DNS record to the reserved public IP before starting Caddy.

## Configure and start

1. Clone this repository on the VM.
2. Copy `.env.example` to `.env` in this directory and fill in the manager email, a long random database password, the Resend settings, and the hostname.
3. Set `APP_DOMAIN` to the hostname only (no `https://`) and `AUTOSERVIS_BASE_URL` to `https://` plus that hostname.
4. Restore the current production database backup into the `database` service before opening the new URL to users.
5. Run `docker compose up -d --build` from this directory. Caddy will request and renew the HTTPS certificate automatically once DNS and ports 80/443 work.
6. Verify login, manager dashboard, customer tracking, email delivery, and existing records on the new HTTPS URL. Only then update the shared AutoServis link.

## Updates and backups

- Update the app with `git pull` and `docker compose up -d --build app`.
- Back up PostgreSQL regularly to storage outside this VM. A Docker volume protects against container replacement, not VM loss.
- Keep `.env` private and keep the Oracle VM on Always Free eligible resources. Oracle may have no free capacity in a chosen region, and its policy permits reclaiming sufficiently idle Always Free compute instances.
