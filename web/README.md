# Jboard Web / iOS PWA

Responsive installable browser client. Host this folder on HTTPS. On iPhone/iPad, open the site in Safari, use Share → Add to Home Screen.

## Prototype features
- One large pan-and-zoom board
- Sticky notes: create, edit, move, delete
- User-drawn connections between notes
- Attach images, videos, PDFs and other files (IndexedDB, local to this browser/device)
- Multiple local projects, history, JSON board export
- PWA manifest, offline app shell and Apple touch icon metadata

## Important boundary
This is a local-first client. Data and attachments remain in the browser on this device. It does not yet provide sign-in, cloud backup, cross-device synchronization, real-time collaboration, invitations or shared permissions. Those require a secure backend and account system. Android currently has its own local storage format; automatic cross-device sync/import conversion is not yet implemented.

Enable GitHub Pages with GitHub Actions as the source in repository Settings → Pages. The workflow publishes this folder on pushes to prototype-rebuild.
