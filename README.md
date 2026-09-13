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

## Writing a post

Add a Markdown file under `content/posts/`, e.g.
`content/posts/2026-03-01-some-slug.md`:

```markdown
---
title: "Post Title"
date: 2026-03-01
tags: [tag-a, tag-b]
type: news                     # or: review
rating: 4.5                    # required when type is review, out of 5
collections: [some-collection] # optional — slugs of collections this post is about
---

Body in Markdown.
```

The slug is derived from the filename (date prefix stripped), or set
explicitly with a `slug:` field. Commit the file — content ships with
the code.

## Writing a collection

Add a Markdown file under `content/collections/`, e.g.
`content/collections/some-collection.md`:

```markdown
---
title: "Collection Title"
slug: some-collection
date: "1986-1987"  # freeform — a year, a range, or "Jan 1990 - Aug 1990"
tags: [tag-a, tag-b]
line: some-franchise   # optional — links to a line (see below)
creators:
  - slug: some-writer
    role: Writer
  - slug: some-artist
    role: Artist
---

Body in Markdown — series summary, publisher, etc.
```

`date` is freeform text, shown exactly as written — quote it if it isn't a
bare number (YAML would otherwise choke on something like `1986-1987`, which
looks like a malformed timestamp). Chronological sorting falls back to the
first 4-digit year found in the string, so it doesn't need to be precise.

Collections get their own `/collections` listing and aren't shown on the
home feed. Any review or news post can point back to one or more collections
with a `collections: [slug, ...]` field, which renders as an "About:" link
on the post and lists the post under "Reviews & News" on the collection's
page. The `creators:` field links a collection to creator profiles (see
below) and renders as "Name — Role" credits on the collection's page. The
`line:` field links a collection to a single franchise/character page
(see below) and renders as a "Part of:" link.

## Writing a creator profile

Add a Markdown file under `content/creators/`, e.g.
`content/creators/some-writer.md`:

```markdown
---
title: "Creator Name"
slug: some-writer
tags: [writer]
---

Body in Markdown — biography.
```

`date` is optional for creators (birth dates are often unknown or private).
Creators get their own `/creators` listing and aren't shown on the home
feed. A creator's page lists every collection that credits them via that
collection's `creators:` field.

## Writing a line

Add a Markdown file under `content/lines/`, e.g.
`content/lines/some-franchise.md`:

```markdown
---
title: "Franchise Name"
slug: some-franchise
tags: [tag-a]
---

Body in Markdown — what this franchise/character line is about.
```

`date` is optional for lines, same as creators. Lines get their own
`/lines` listing and aren't shown on the home feed. A line's page lists
every collection filed under it via that collection's `line:` field.

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
