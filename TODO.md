# To do

Things a blog like this should have, from a review of the site on
2026-10-16. Done so far: the RSS/Atom feed (`/feed.xml`) and removing
`content/dummy.md`.

## Should have

- [x] **Favicon.** `/favicon.ico` doesn't exist, so the browser tab shows
  a blank icon. The "EARTH Q" lettering or the yellow T from the banner
  could work.
- [x] **Page descriptions and link previews.** Every page has a meta
  description and Open Graph tags; see WRITING.md.
- [ ] **Write the real tagline.** "Reading the DC Finest line, one brick
  at a time." is a placeholder. It's `site-tagline` in
  `src/finest/views/components.clj`.
- [x] **About page.** The "Hello there" post is the only intro, it says
  "to be continued", and it will slide down the list as more posts go
  up. Nothing explains what DC Finest is to someone arriving cold.

## Before going public

- [x] **`robots.txt` and a sitemap.** `/robots.txt` welcomes every
  crawler and points at `/sitemap.xml`, which lists every public page.
- [x] **Drafts.** `draft: true` hides a page on the live server; the
  dev server shows it with a Draft badge.
- [x] **Answer HEAD requests.** Answered like GET, without the body.
- [x] **Browser caching.** On the production server, CSS, images and
  icons may be reused for a day, then re-checked (an unchanged file
  gets a tiny "304 Not Modified"). Off in dev, so CSS edits show at once.
- [ ] **Set `SITE_URL`** on the server once there's a public address,
  so the feed, sitemap and `robots.txt` stop pointing at
  `http://localhost:3000`. See the README's Deployment section.

## Smaller things

- [ ] **Real contact address.** The footer's Contact link still goes to
  the placeholder `contact@example.com` (in `src/finest/views/layout.clj`).
- [x] **Copyright / trademark line** in the footer: unofficial fan site,
  DC trademarks, cover art shown for review, and © for the writing.
- [x] **Paging.** Latest, Reviews, Articles and tag pages show 10 posts
  a page (`?page=2`), with Newer/Older links once there's more than one.
