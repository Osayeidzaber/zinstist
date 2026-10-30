# 006 — Now Playing card (Spotify) and Nick Hider

**Status:** idea only (recorded 2026-10-30)
**Source:** `assets/006-music-player-and-nick-hider-01.png`

## What it is

Two cards from the same reference page. The left is a compact music player with live lyrics; the right is
a module that hides the player's own name everywhere in the UI.

## Elements observed

**Spotify Player (left)**
- Above the card: two lines of lyrics. The first line is partly bright and partly dimmed (`we car let the
  busy world go by`), the second is dim (`in the violet hour's gentle glow`) — reads like karaoke fill,
  current line highlighted with the next line previewed.
- The player itself: rounded dark card, square album art on the left, `Violet Hour` title, `Purixy` artist,
  previous / play / next controls on the right, a progress bar with `0:12` and `0:46` at the ends.
- Card caption: "The song you're playing, with lyrics in time."

**Nick Hider (right)**
- Chat box with three lines: `Purixy joined the game`, `<Purixy> gg, rematch?`, `Purixy was slain by Nightfall`.
- Below it, three small name chips: `Purixy` (purple), `Nightfall` (dark), `kairo` (dark).
- Card caption: "Your name, hidden everywhere. Chat, tab list, name tags, even books."

## Current state in Jilaton

- Music is already a deep subsystem: `music/MusicCenter`, `SpotifyConnect`, `LocalMusicPlayer`,
  `OnlineLyrics`, `LyricsMatcher`, `Lyrics`, cover lookup, a media-session bridge script, a
  `MusicCenterScreen`, plus `NowPlayingModule`, `SpatialLyricsModule`, `MusicVisualsModule` and tests.
  The functionality exists; the gap is this compact card design and the lyric-fill presentation.
- Nick Hider: nothing found in source. That part is new.

## Likely work areas

- Now Playing card: new compact widget layout over the existing `MusicCenter` state; karaoke-style lyric
  line with current-word highlight; progress and controls wired to the existing playback controls.
- Nick Hider: intercept the displayed name in chat, tab list, nametags, and written-book text (and decide
  about server list / container titles). This is display-only; the server still sees the real name.
- A setting for the replacement name (fixed alias vs. another player's name vs. blank).

## Open questions

- What are the three chips under the Nick Hider chat — friends, party, or examples of other players?
- Should the hidden name be replaced with a fixed alias, a random name, or removed entirely?
- Does Nick Hider cover only the local UI, or also outgoing chat? (It can only affect what is displayed;
  messages still carry the real name to the server.)
- Should the lyric line be word-by-word progress (as the dimmed words suggest) or line-by-line?

## Boundaries

- Do not claim Nick Hider hides the name from the server or from other players; it only changes local display.
- Reference only; no Purixy assets, fonts or copy.

## Handoff prompt

> Working in `C:\Users\zaber\Desktop\zinstist` (Jilaton Client, Fabric 1.21.11). Read `JilatonClient/agent.md`,
> the checkpoint, then `ideas/006-music-player-and-nick-hider.md` and
> `ideas/assets/006-music-player-and-nick-hider-01.png`.
>
> Goal, part A: a compact "now playing" card matching the reference — karaoke lyric line above, album art,
> title, artist, prev/play/next, progress with elapsed/total. Reuse `music/MusicCenter`, `LyricsMatcher`
> and `NowPlayingModule` rather than adding a second player.
>
> Goal, part B: a Nick Hider module that replaces the local display of the player's own name in chat,
> tab list, nametags and written books (display-only).
>
> Inspect the music package and existing name-rendering paths first, then ask me the open questions
> (replacement name, chips, lyric granularity) and give me a short plan. Reference is inspiration only —
> no Purixy assets or copy.
