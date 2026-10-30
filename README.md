# Comic Blog

A small Clojure blog for comic-book news and reviews. Posts are Markdown
files with YAML frontmatter under `content/`, loaded into memory — no
database. News and reviews live under `content/posts/`; standalone
descriptions of the collections (trades/omnibi) themselves live under
`content/collections/`; creator biographies live under `content/creators/`;
franchise/character lines (Batman, Superman, etc.) live under
`content/lines/`.

`type:` is inferred from which of these folders a file lives in, so it
only needs to be written explicitly under `content/posts/`, where it
distinguishes `news` from `review`. An explicit `type:` elsewhere still
wins over the inferred one, if you ever need it.

## Development

```sh
clj -M:dev
```

At the REPL:

```clojure
(go)             ; load content, start Jetty on :3000
(reset)          ; reload changed code (tools.namespace) and restart
(stop)           ; stop the server
(reload-content! ) ; force a manual content reload
```

While `(go)` is running, editing a file under `content/` and
refreshing the browser is enough — content is reloaded automatically
in dev (cheap mtime check per request).

Run tests: `clj -M:test`

## Writing content

See [WRITING.md](WRITING.md) for templates and every supported field,
for each kind of page.

## Deployment

Build an uberjar:

```sh
clj -T:build uber
```

This produces `target/finest-standalone.jar`. Copy it, along with
`content/`, to the server (e.g. `/opt/finest/`).

Run it directly:

```sh
PORT=3000 CONTENT_DIR=/opt/finest/content SITE_URL=https://example.com \
  VIEWS_FILE=/opt/finest/data/views.edn \
  java -jar /opt/finest/finest-standalone.jar
```

`SITE_URL` is the site's public address. The Atom feed (`/feed.xml`),
the sitemap (`/sitemap.xml`) and `robots.txt` need it for their
absolute links; without it, they point at `http://localhost:3000`.

### Page views

The server counts views of every page under `/posts/` (reviews,
articles, volumes, lines, creators) per day, and writes them to
`VIEWS_FILE` (default `data/views.edn`, relative to the working
directory) once a minute and when it stops:

```clojure
{"2026-11-03" {"silver-age-gl" 14, "green-lantern-defeat-green-lantern" 6}}
```

Only a count per page per day is kept: no IP addresses, no cookies.
Bots that name themselves in their user agent, HEAD requests and
"not found" pages aren't counted, and the dev server counts nothing.
Keep the file outside `content/` so deploys don't touch it; restarts
add to what's already there. For a quick top list, copy the file
somewhere and at the REPL:

```clojure
(require '[finest.views-count :as vc] '[clojure.edn :as edn])
(vc/top (edn/read-string (slurp "views.edn")) 20)               ; all time
(vc/top (edn/read-string (slurp "views.edn")) 20 "2026-11-01")  ; since a date
```

### systemd unit

`/etc/systemd/system/finest.service`:

```ini
[Unit]
Description=Comic Blog
After=network.target

[Service]
WorkingDirectory=/opt/finest
Environment=PORT=3000
Environment=CONTENT_DIR=/opt/finest/content
Environment=SITE_URL=https://example.com
Environment=VIEWS_FILE=/opt/finest/data/views.edn
ExecStart=/usr/bin/java -jar /opt/finest/finest-standalone.jar
Restart=on-failure
User=finest

[Install]
WantedBy=multi-user.target
```

```sh
sudo systemctl enable --now finest
```

### nginx reverse proxy

```nginx
server {
    listen 80;
    server_name example.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Add TLS with `certbot --nginx`.

### Publishing a new post

Edit/add a Markdown file, commit, then on the server: pull the change,
rebuild the jar (or just `git pull` if you deploy the `content/` dir
independently of the jar), and `sudo systemctl restart finest`.
