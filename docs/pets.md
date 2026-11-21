# Pets

Dogs, cats and crocodiles (`items_pets`, one row per pet; the pet lives in its basket, the `nest` item, "Kurv",
whose owner owns the pet). Code: `game/pets/` (needs, nature, commands, texts) and `game/room/tasks/PetTask.java`
(what the pet does, once a second from `StatusTask`).

## Needs

| Need | Levels | Status window | Drops one level every | Filled by |
|---|---|---|---|---|
| Hunger | 0-6 | `pet_hung_N` | `pet.hunger.step.minutes` (60) | a bowl meal +3 (`petfood3` "Lavt Energiindhold" +2), a treat +1 |
| Thirst | 0-3 | `pet_thir_N` | `pet.thirst.step.minutes` (90) | a water bowl: full |
| Happiness | 0-6 | `pet_mood_N` | `pet.happiness.step.minutes` (60) | playing with the ball: full; a treat +2, praise +1, a meal when hungry +1; scolding -1 |
| Energy | 0-6 | (not shown) | `pet.energy.step.minutes` (15) | sleeping: +1 every `pet.energy.recover.seconds` (90); playing and following cost 1 |

Each level is a timestamp (`last_eat`, `last_drink`, `last_playtoy`, `last_kip`): the level is max minus the steps since
it, so needs keep dropping while nobody is in the room. A pet is rested whenever its room loads.

## Nature and friendship

The status window's "Natur:" words are the pet's personality, fixed when it is bought (0-11 each):
`nature_negative` = `pet_enrg_N` (Træt ... Utrættelig, Sur, Gal), `nature_positive` = `pet_frnd_N` (Fjendtlig ... Ivrig).
The energy word sets how active it is (walks, plays, decides more often; Sur grumbles, Gal pulls crazy faces), the
friendliness word how much it seeks people (Fjendtlig/Vred/Mistænksom growl at visitors).

`friendship` (0-100, with the owner; new pets 30) rises when the pet obeys its owner (+1), is praised (+3 right after an
obeyed command, else +1) or greets its owner, falls when scolded (-2) and by 2 a day without any attention
(`last_playuser`, applied when the room loads, not below 10).

## What a pet does

Needs first: tired (energy 1 or less, or now and then at night) it walks to its basket and sleeps (in the basket's place
if it can't get there) until rested; at night (`pet.night.start.hour` 23 to `pet.night.end.hour` 7, hotel time) it sleeps
on until morning unless woken. Thirsty or hungry, it goes to water or food (its own kind's: `pet_dog_food` etc., or
`pet_food` for all); with none it begs its owner or looks sad. Unhappy, it plays with a ball. Otherwise it idles by its
nature: short walks nearby, sniffing, scratching, stretching, sitting, lying down, looking around, wagging when someone
is near, jumping, a sound now and then (rarely the same thing twice in a row).

People: it greets its owner when they come in (walks over, happy face, wagging tail; it may wake for it), sniffs friendly
visitors or growls at them, and looks at anyone who says its name. Putting food or water in the room calls hungry
pets over.

Furni: a food bowl lasts four meals (`custom_data` counts them, then it is gone); a treat (`goodie1/2`) is one bite; a
water bowl never runs dry ("Uendeligt vand", always state 5); the ball (`toy1`) turns while a pet plays with it (its 6
frames, not saved).

## Commands

"&lt;name&gt; &lt;command&gt;" or "&lt;command&gt; &lt;name&gt;" in chat or shout; case and punctuation don't matter
("Rex, sit!"). Only the owner, Habbos with rights and staff with `fuse_any_room_controller` give commands. Each command
answers to the hotel's word (`pet.hotwords.*`) and a few others (`PetCommand`):

| Command | Hotel word | Does |
|---|---|---|
| sit / lie / play_dead / jump / beg | sit, læg dig, spil død, hop op, tig | the pose for a while |
| voice | gi' lyd | a sound |
| come_here / follow_me / go_away | kom her, følg mig, gå væk | comes next to you / follows you for 1-2 minutes / walks far away |
| nest | sov | goes to its basket and sleeps (a short nap when rested) |
| good / bad | godt / fy | praise / scold (always "heard") |
| eat / drink | spis / drik | goes to food or water (refuses when full) |

Whether it obeys: owner 55 + friendship × 0.4 %, others 30 + friendliness × 30 + friendship × 0.1 %; less when unhappy,
tired (active commands), very hungry or Sur; more for Hengiven/Loyal/Nuttet/Ivrig; 10-97 %. A refusal is a puzzled
or angry face. A sleeping pet may wake up for it; an eating one finishes first.

## Texts

Pets speak lines from the hotel's own external texts (`pet.saying.<generic|eat|play|sleep|sniff|beg|angry>.<dog|cat|croco>.N`)
and answer to `pet.hotwords.*`; Odin reads that file from `pet.texts.url` at start, on `:reload texts` and when the setting
changes (on prod the asset host on the host itself, `http://127.0.0.1:3001/gamedata/external_texts.txt`). Without it pets
use a few lines of their own (`PetTexts`).

## What the client draws

Statuses (hh_pets_common "Pet Class", `action_<code>`): `mv`, `sit h`, `lay h`, `slp h`, `snf`, `scr`, `bnd`, `beg`,
`pla`, `rdy`, `jmp`, `ded`, `eat`, `wav` (tail), `talk`, and faces as `gst <face>` (`sml`, `joy`, `agr`, `sad`, `puz`, `srp`,
`tng`, `crz`, `eyb`; `sml`/`agr`/`sad`/`puz` also move the tail). Per status update the first action for a body part wins.
The eating head and the playing body exist only for direction 2 (mirrored for 4): a pet eats, drinks and plays facing
2 or 4, or the client leaves out its head or body.

## Staff

With `fuse_debug_window` the pet's info stand has a Control button: its window sends `:passivate <id>` (the pet stops on
its own), `:activate <id>`, `:setdir <id> <head> <body>` and `:actionon|:actionoff <id> Dev <status> [value]`
(`PetControlCommand`; id = the room instance id). `room.pets.max` (0 = no limit) caps pets per room when a basket is placed.
