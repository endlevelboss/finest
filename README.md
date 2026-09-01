# Comic Blog

A small Clojure blog for comic-book news and reviews. Posts are Markdown
files with YAML frontmatter under `content/posts/`, loaded into memory —
no database.

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

While `(go)` is running, editing a file under `content/posts/` and
refreshing the browser is enough — content is reloaded automatically
in dev (cheap mtime check per request).

Run tests: `clj -M:test`

## Writing a post

Add a Markdown file under `content/posts/`, e.g.
`content/posts/2026-03-01-some-slug.md`:

```markdown
---
title: "Post Title"
date: 2026-03-01
tags: [tag-a, tag-b]
type: news        # or: review
rating: 4.5       # required when type is review, out of 5
---

Body in Markdown.
```

The slug is derived from the filename (date prefix stripped), or set
explicitly with a `slug:` field. Commit the file — content ships with
the code.

## Deployment

Build an uberjar:

```sh
clj -T:build uber
```

This produces `target/finest-standalone.jar`. Copy it, along with
`content/`, to the server (e.g. `/opt/finest/`).

Run it directly:

```sh
PORT=3000 CONTENT_DIR=/opt/finest/content/posts \
  java -jar /opt/finest/finest-standalone.jar
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
Environment=CONTENT_DIR=/opt/finest/content/posts
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
