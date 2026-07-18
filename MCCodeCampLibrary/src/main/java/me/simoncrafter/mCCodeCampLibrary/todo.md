# Todo

___

## Over All
- [ ] Placing and creating inputs
- [ ] Removing inputs
- [ ] Input types
  - [x] Button
  - [x] Pressure Plate
  - [ ] Chat (Simple prompt, to Questioner)
  - [ ] Sign
  - [x] Area Trigger
  - [ ] NPC / Entity Click & kill trigger
  - [ ] NPC Dialog
- [ ] Outputs
  - [ ] Chat
  - [ ] Text Display
  - [ ] NPC Dialog
  - [ ] Sound
  - [ ] Particles
  - [ ] Console
  - [ ] Misc
    - [ ] Explosions
- [ ] obstacles
  - [ ] Door
    - [ ] Sliding door
    - [ ] Swinging door
    - [ ] Fallover door (kick in style)
  - [ ] Bridge
    - [ ] Drawbridge
    - [ ] Extendable Bridge
  - [ ] Moveable blocks (for labyrinth style puzzles)
  - [ ] Elevator
- [ ] NPC
  - [ ] Movements
  - [ ] Dialog
  - [ ] Creating
  - [ ] Removing
- [ ] Utilities
  - [ ] Cooldowns

___

## Button
~~Make arrow/snowball activations actually set the entity~~ done via ProjectileHitEvent

## Entity marking
No author-facing way to set/remove "npc" markers yet (EntityMarkerRegistry only has
the read side) — needs an equivalent of ButtonHandler's placement flow once
"Placing and creating inputs" is tackled.