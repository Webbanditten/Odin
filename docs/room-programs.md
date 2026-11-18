# Public room shows (`SHOWPROGRAM`) and `:program`

Some public rooms have disco floors, lights or lamps that the client only changes when the server tells it to, with
the `SHOWPROGRAM` message (header 71). Its body is three words: `<dest> <command> [<param>]`. The client keeps only the
first three words, so a value can't contain spaces. The room's client script (`<model> Class` in the room's cast)
receives it as `showprogram`.

Odin drives these rooms by itself (`RoomProgramTrigger` + `RoomProgramTask`, in `game/room/programs/`). The first
visitor starts the room's show, it changes every `room.programs.seconds` (setting, default 45; `0` = never by itself),
and everyone who walks in gets the current state at once. The Space Cafe has its own task (`SpaceCafeTask`, boards and
lamps) and the Lido its pool programs (splashes, the lift door, the big screen).

## The rooms

| Room (model) | Message | Values | What it does |
|---|---|---|---|
| Club Mammoth (`club_mammoth`) | `discofloor <n>` | 1–9 | The disco floor's pattern: 1 chessboard, 2 scrolling arrows, 3 random colours, 4 rotating centre, 5 chessboard (other colours), 6 random (other colours), 7 chessboard (other colours), 8 rotating centre moving around, 9 scrolling text. The first word is ignored but must be there. |
| Café Solnedgang (`sunset_cafe`) | `discofloor <n>` | 1–6 | The same disco engine on the café's lights. |
| Club Orient (`orient`) | `lights <n>` | 1–10 | Light shows: the spotlights always turn; 2–10 also switch and blink the lamps. |
| Club 57 – Dansegulv (`malja_bar_b`) | `ambient1 fade <RRGGBB>` | a colour | Tints the whole room (subtracted, so dark colours are subtle: `330000` takes some red away). Odin fades between `330000`, `003333`, `330033`, `000033`, `333300`, `003300`, `220022` in 5 seconds. |
| Det orange Rum (`bar_b`) | `lamp setlamp <n>` | 1–5 | The floor lamp's colours. |
| Space Cafe (`space_cafe`) | `flipflop<0-7> visible <0/1>`, `lmpa ufol <n>`, `liga litecol <n>` (and b, c) | | The flip board and the three lamps; Odin runs these. |
| Habbo Poolen (`pool_a`, `pool_b`) | `cam1 showtext <text>`, `cam1 setcamera <n>`, `cam1 targetcamera <id>`, `Splash0 …`, `door open/close` | | The big screen (lines split on `/`), splashes and the lift door; Odin sends these from the pool code. |

Every other public room animates by itself or has nothing the server changes.

## `:program` (staff, `fuse_room_alert`)

Type it in the room's chat:

- `:program` shows what the room is showing now.
- `:program discofloor 9`, `:program ambient1 fade 003300`, `:program lamp setlamp 3`, `:program lights 7` sets a
  show. In a room Odin drives, that part then stays as you set it (also for people who come in later) until
  `:program auto`.
- `:program auto` lets the room change by itself again.

It works in any room, so it can also test a room's script, for example `:program cam1 showtext Hej/med/dig` in the
pool. A room that is empty for a minute is unloaded, and its show starts again from the beginning on the next visit.

## Adding a room

Give the model's row in `rooms_models` the `trigger_class` `room_program_trigger`, and add the model to
`RoomProgramTask.channelsFor` with what its client script understands (a numbered style, a fading colour). Read the
room's `<model> Class` in the decompiled cast (`habbo-vibe-playground/decompiled/<cast>`; the room's casts are in
`rooms.ccts`) for the message format.
