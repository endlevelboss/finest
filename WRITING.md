# Writing for Transmissions

A practical guide to adding content to the site. Every page is a
Markdown file under `content/`. The folder a file is in decides what
kind of page it becomes, and the block between the two `---` lines at
the top (the *frontmatter*) holds its details.

The templates below show **every option** each kind of page supports.
Copy one, fill it in, and delete the lines you don't need: everything
marked *optional* can simply be left out.

## The everyday workflow

1. Start the dev server (see the README's Development section) and
   open <http://localhost:3000>.
2. Add or edit a file under `content/`.
3. Refresh the browser. Content reloads on its own, with no restart.
4. If a page doesn't show up, check the terminal running the server:
   problems with a file are printed there as a `WARN:` line naming
   the file.
5. When you're happy, commit the file. Content ships with the code.

## Which folder?

| You're writing…                     | Put it in            | Shows up on            |
| ----------------------------------- | -------------------- | ---------------------- |
| A review of a volume                | `content/posts/`     | Latest, Reviews, feed  |
| Any other article (news, opinion)   | `content/posts/`     | Latest, Articles, feed |
| A volume (one DC Finest book)       | `content/collections/` | Volumes, its line    |
| A creator (writer, artist…)         | `content/creators/`  | Creators, their page   |
| A line (Batman, Green Lantern…)     | `content/lines/`     | Lines                  |
| The About page                      | `content/pages/about.md` | `/about`, footer link |

Inside `posts/`, `collections/`, `creators/` and `lines/` you can make
any subfolders you like, as deep as you like (`content/collections/g/…`,
`content/posts/2026/10/…`); they're only for your own tidiness. They
don't change a page's address or its date, so two files with the same
name in different folders would fight over one address. The site warns
about that when it loads.

## File names and slugs

A page's address is `/posts/<slug>`. The slug comes from the file
name, minus `.md` and any leading date:

- `content/posts/2026-10-06-gl-first.md` → `/posts/gl-first`

Or set it yourself with a `slug:` line. Slugs are lowercase letters,
digits and dashes only. **Slugs are how pages point at each other**,
so once a page is out, avoid renaming its slug.

## Drafts

Add `draft: true` to any page's frontmatter to keep it unpublished:

```markdown
---
title: "Half-finished thoughts on Doom Patrol"
date: 2026-10-20
type: review
rating: 2
draft: true
---
```

On your own dev server a draft shows up as normal, with a yellow
**Draft** badge, so you can see exactly how it will look. On the live
server it doesn't exist at all: it's not listed anywhere, not in the
feed, not linked from the volume it reviews, and its address returns
"not found". Delete the `draft: true` line (or set it to `false`) to
publish.

---

## A review

`content/posts/2026-10-20-doom-patrol-review.md`

```markdown
---
title: "Doom Patrol is gloriously weird"
date: 2026-10-20
type: review
rating: 2.5
collections: [doom-patrol-end-sacrifices-means]
tags: [silver-age]
cover: /images/doom-patrol.jpg
---

The review itself, in Markdown.

![Robotman](/images/robotman.jpg "feature")
```

| Field          | Required? | What it does |
| -------------- | --------- | ------------ |
| `title`        | yes       | The heading. |
| `date`         | yes       | Publication date, `YYYY-MM-DD`, unquoted. Orders the Latest list and the feed. |
| `type`         | yes       | `review`. |
| `rating`       | yes       | **Out of 3 stars.** Halves work (`2.5`); `0` is allowed. |
| `collections`  | optional  | Slugs of the volume(s) reviewed. Adds the "A review of: …" line above the title, and lists this review on each volume's page and next to it on the Volumes page. |
| `tags`         | optional  | A list, e.g. `[silver-age, batman]`. Each tag gets its own page under `/tags`. |
| `cover`        | optional  | An image shown large at the top of the post. |

## An article

`content/posts/2026-10-25-new-solicits.md`

```markdown
---
title: "January solicitations are out"
date: 2026-10-25
type: news
collections: [superman-man-of-steel]
tags: [solicits]
cover: /images/solicits.jpg
---

The article, in Markdown.
```

Same fields as a review, except `type: news` and no `rating`. Articles
are badged "Article" on the site.

## A volume

`content/collections/s/superman-man-of-steel.md`

```markdown
---
title: "The Man of Steel"
slug: superman-man-of-steel
line: superman
date: "1986-1987"
release-date: 2026-11-10
cover: /images/superman-man-of-steel.jpg
tags: [bronze-age-post-crisis]
creators:
  - slug: john-byrne
    role: Writer
  - slug: dick-giordano
    role: Inker
double-dips:
  - issue: "Action Comics #584"
    part: "Superman lead story"
    date: "Jan 1987"
    creators:
      - slug: some-fill-in-artist
        role: Artist
---

Collects The Man of Steel #1-6, Superman #1-3 and Action Comics #584.
```

| Field          | Required? | What it does |
| -------------- | --------- | ------------ |
| `title`        | yes       | Just the volume's own name ("The Man of Steel"). When `line:` is set, the site shows "Superman: The Man of Steel". |
| `slug`         | recommended | Reviews and articles point at this. |
| `line`         | optional  | The slug of a line page. Groups the volume under that line. |
| `date`         | yes       | When the *original comics* came out, as free text: `"1986-1987"`, `"Jan 1990 - Aug 1990"`, shown exactly as written. Put it in quotes. Only the first year is used for sorting. |
| `release-date` | optional  | The book's street date, `YYYY-MM-DD`, unquoted. Puts it on the Volumes page by year and in the "Recently released" / "Next up" sidebar. Leave it out if nothing's announced. |
| `cover`        | optional  | Cover image, shown beside the title and as a thumbnail on the line's page. |
| `tags`         | optional  | As for posts. |
| `creators`     | optional  | Who made it. `role` is optional. A plain list of slugs works too: `creators: [john-byrne, dick-giordano]`. |
| `double-dips`  | optional  | Issues this volume shares with another volume. See below. |

The body is the volume's contents: "Collects …", plus anything else
worth saying.

### Double-dips

Only list the *overlap*: issues, or parts of issues, that another
volume also reprints. Write the same `issue:` text in both volumes and
the site links them up with an "Also collected in" note. Case and
spacing don't matter (`"Catwoman (vol 2) #14"` matches
`"catwoman (vol 2) # 14"`). Only `issue:` is required; `part:`,
`date:` and `creators:` are optional.

## A creator

`content/creators/john-byrne.md`

```markdown
---
title: "John Byrne"
tags: [writer, artist]
---

A short biography.
```

Only `title` is required. **You don't have to write creator files at
all**: anyone named in a volume's `creators:` gets a page automatically,
with a name guessed from the slug (`john-byrne` → "John Byrne") and a
list of their volumes. Write a file only when you want to add a bio.

## A line

`content/lines/superman.md`

```markdown
---
title: "Superman"
tags: [superman]
---

What this line is about.
```

Only `title` is required. The line's page lists every volume whose
`line:` points at it, oldest comics first.

## The About page

`content/pages/about.md`

```markdown
---
title: "About"
---

Who's writing, and what the site is for.
```

Only `title` is required. It's served at `/about` and linked from the
footer, and it never shows up in the Latest list or the feed.
Everything under "Tricks inside the text" works here too.

---

## Tricks inside the text

### Link to another page by slug

```markdown
As I said in [[gl-defeat]], the ring can do anything.
```

`[[slug]]` becomes a link with that page's title, and the stars if
it's a review. Works for any page: review, article, volume, line or
creator.

### A review box

```markdown
{{review gl-defeat}}
```

On a line of its own, this becomes a small box showing the review's
volume, title, stars and featured image. Handy for round-up articles.

If a slug in `[[…]]` or `{{review …}}` doesn't match any page, it's
left as written, so a typo stays visible on the page instead of
vanishing.

### Images

Put the image file in `resources/public/images/`, then use it in the
text:

```markdown
![Hal Jordan](/images/hal-jordan.jpg)
```

To make an image the post's **featured image** (the thumbnail on the
Latest list and in review boxes), add `"feature"` after the path:

```markdown
![Hal Jordan](/images/hal-jordan.jpg "feature")
```

It still shows in the article as normal. If you mark several, the
first one wins.

### Everyday Markdown

```markdown
*italic*  **bold**  [a link](https://example.com)

## A subheading

- a list item
- another

> A quote
```

---

## Search results and link previews

Each page carries a short **description** and a **preview image** in
its hidden header. Visitors never see them on the page itself. They
show up in two places:

- **Search results**: the description is the text under the page title.
  Without one, search engines grab whatever text they find, sometimes
  the menu or the sidebar.
- **Link previews**: the card that appears when someone pastes a link
  into Discord, Slack, Messenger, Facebook, Bluesky and so on, with the
  title, the description and the image. These are called *Open Graph*
  tags. Links without them often show as bare text and get far fewer
  clicks.

Nothing extra to write by default. Each page takes:

| Page                     | Description                      | Image |
| ------------------------ | -------------------------------- | ----- |
| Review                   | its first paragraph              | the featured image, else the reviewed volume's cover |
| Article                  | its first paragraph              | the featured image, else its `cover:` |
| Volume                   | its first paragraph ("Collects …") | its cover |
| Line, creator, About     | its first paragraph              | the site's fallback image |
| Home page and listings   | the site's tagline               | the site's fallback image |

The fallback image is the banner. The tagline lives next to the site's
name in `src/finest/views/components.clj` (`site-tagline`), and is also
the RSS feed's subtitle.

The description is cut to about 150 characters, at a word boundary.

**What this means when writing:**

- **Make the first paragraph count.** It's what people see in search
  results and shared links. A review that opens with a one-line verdict
  previews better than one that opens with a joke or an aside.
- **Override when the opening doesn't fit.** An optional `description:`
  line in the frontmatter replaces the first paragraph:

  ```yaml
  description: "Hal Jordan gets a ring that can do anything, and the stories never recover."
  ```

- **Mark a featured image** (see Images above) on reviews and articles
  you expect to share. Otherwise the preview falls back to the volume's
  cover, or the site's general image.

**Good to know:**

- Previews only work once the site is public. The apps fetch the page
  from their own servers, so they can't see `localhost`.
- Apps cache previews, sometimes for days. Fix a description *before*
  sharing a link, not after.
- Preview images look best at 1200×630 pixels (a 1.91:1 shape). Wider
  or taller images get their edges cropped.

---

## Gotchas

- **Volumes need a `date`.** Without one the volume is skipped (the
  terminal shows a `WARN: Post is missing :date` line).
- **Quote free-text dates.** `date: "1986-1987"`. Quotes keep text with
  colons or other odd characters from confusing the frontmatter.
- **Don't quote real dates.** `date: 2026-10-06` and
  `release-date: 2025-06-10` go unquoted.
- **Ratings are out of 3**, not 5.
- **Reviews and articles need `type:`** (`review` or `news`). Nothing
  else does: the folder already says what it is.
- **Keep files in their folders.** A file loose in `content/` itself
  has no folder to say what it is, so it won't become a page.
- **Use spaces, not tabs**, to indent the frontmatter lists.
