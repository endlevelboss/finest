# To do

Things a blog like this should have, from a review of the site on
2026-10-16. Done so far: the RSS/Atom feed (`/feed.xml`) and removing
`content/dummy.md`.

## Should have

- [x] **Favicon.** `/favicon.ico` doesn't exist, so the browser tab shows
  a blank icon. The "EARTH Q" lettering or the yellow T from the banner
  could work.
- [ ] **Page descriptions and link previews.** Pages only set a `<title>`.
  Add a meta description for search results, plus Open Graph tags so a
  link shared in a chat or on social media shows a title, summary and
  image instead of bare text. Reviews could use the volume's cover.
- [x] **About page.** The "Hello there" post is the only intro, it says
  "to be continued", and it will slide down the list as more posts go
  up. Nothing explains what DC Finest is to someone arriving cold.

## Before going public

- [ ] **`robots.txt` and a sitemap.** Neither exists. Not strictly
  needed, but they help search engines find every review. Decided:
  allow every crawler (no AI-training opt-out), with a `Sitemap:` line;
  drafts need no rule, since the live server never serves them. Wait
  for a public address, as the sitemap needs `SITE_URL`.
- [x] **Drafts.** `draft: true` hides a page on the live server; the
  dev server shows it with a Draft badge.
- [ ] **Answer HEAD requests.** The server answers them with
  "405 Method Not Allowed". Link checkers, uptime monitors and
  link-preview tools often send one first, and would think the site is
  broken.
- [ ] **Browser caching.** Images and the CSS file are sent with no
  caching instructions, so returning visitors may download them again.
  It matters most for the banner (285 KB) and `batman.jpg` (190 KB).
- [ ] **Set `SITE_URL`** on the server once there's a public address,
  so feed links stop pointing at `http://localhost:3000`. See the
  README's Deployment section.

## Smaller things

- [ ] **Real contact address.** The footer's Contact link still goes to
  the placeholder `contact@example.com` (in `src/finest/views/layout.clj`).
- [ ] **Copyright / trademark line** in the footer, e.g. "DC characters
  are trademarks of DC Comics". Common on fan sites that use cover art.
- [ ] **Paging on the home page.** It lists every post, which is fine
  with three but will need paging eventually.
