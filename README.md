# Comic Blog

A small Clojure blog for comic-book news and reviews. Posts are Markdown
files with YAML frontmatter under `content/`, loaded into memory — no
database. News and reviews live under `content/posts/`; standalone
descriptions of the comics themselves live under `content/comics/`.

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

## Writing a post

Add a Markdown file under `content/posts/`, e.g.
`content/posts/2026-03-01-some-slug.md`:

```markdown
---
title: "Post Title"
date: 2026-03-01
tags: [tag-a, tag-b]
type: news             # or: review
rating: 4.5            # required when type is review, out of 5
comics: [some-comic]   # optional — slugs of comics this post is about
---

Body in Markdown.
```

The slug is derived from the filename (date prefix stripped), or set
explicitly with a `slug:` field. Commit the file — content ships with
the code.

## Writing a comic description

Add a Markdown file under `content/comics/`, e.g.
`content/comics/some-comic.md`:

```markdown
---
title: "Comic Title"
slug: some-comic
date: 1986-09-01   # the comic's original publication date, not a post date
tags: [tag-a, tag-b]
type: comic
---

Body in Markdown — series summary, creators, publisher, etc.
```

Comics get their own `/comics` listing and aren't shown on the home feed.
Any review or news post can point back to one or more comics with a
`comics: [slug, ...]` field, which renders as an "About:" link on the post
and lists the post under "Reviews & News" on the comic's page.

## Deployment

Build an uberjar:

```sh
clj -T:build uber
```

This produces `target/finest-standalone.jar`. Copy it, along with
`content/`, to the server (e.g. `/opt/finest/`).

Run it directly:

```sh
PORT=3000 CONTENT_DIR=/opt/finest/content \
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
Environment=CONTENT_DIR=/opt/finest/content
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
