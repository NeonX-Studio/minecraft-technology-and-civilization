# Technology and Civilization

A family of independent NeoForge 26.3 mods for Minecraft.

The modules are designed to be enjoyable on their own. The core modules provide stable contracts and saved-world state so optional modules can cooperate without requiring the full family.

## Modules

- `tcivilization-core`: world history, shared events, persistent civilization state
- `tctechnology`: technology progression and integration contracts
- `tcskills`: player proficiency, skills, skill points, talents, and proficiency bonuses
- `tcwildlands`: geography and natural resource foundation
- `tcecology`: species, habitats, and ecological change
- `tccultivation`: crops, soil, irrigation, livestock, and preservation
- `tcindustry`: manufacturing, energy, maintenance, and automation
- `tcnetworks`: rail, shipping, stations, storage, and routing
- `tcsettlements`: households, professions, housing, and town growth
- `tcexchange`: regional supply, demand, orders, and reputation
- `tcdiscovery`: observation, experiments, archaeology, and research
- `tcechoes`: ruins, local history, legends, and exploration events
- `tccommons`: cooperative building, land permissions, and civic projects
- `tcfrontiers`: outposts, expeditions, supplies, and dangerous zones
- `tcbeyond`: optional magic ecology, other dimensions, and anomalous resources
- `tcatlas`: minimap, world map, thematic layers, surveying, waypoints, and HUD layout editor
- `tcnations`: AI civilizations, territory, diplomacy, and border conflict
- `tcorbit`: rocketry, satellites, orbital stations, the Moon, and Mars colonization

Each module currently ships a small independent status command. The commands are intentional vertical-slice placeholders and will grow into gameplay systems without changing the module boundaries.

## Design documentation

The authoritative design specification (Chinese, with English IDs) lives in [`docs/design/`](docs/design/README.md). Start with the index, then the vision, era progression, and architecture chapters before reading individual module chapters.

## Skills and growth

`tcskills` owns player progression independently from the civilization world-history core. It persists total proficiency, per-skill experience, growth levels, skill points, talents, and derived proficiency multipliers. Useful commands are `/tcskills status`, `/tcskills train <skill>`, and `/tcskills learn <talent>`. Other modules can call `SkillsApi.addProficiency(...)` and `SkillsApi.getProficiencyMultiplier(...)` without requiring their systems to be merged into the skills module.
