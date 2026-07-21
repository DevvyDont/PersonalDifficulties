# Personal Difficulties

A Fabric mod that allows players to choose personalized difficulties for themselves, independent of the server's
difficulty setting. This mod provides a way for players to tailor their gameplay experience to their preferences,
allowing for a more enjoyable and customized Minecraft SMP experience.

Players can also set a personal keep inventory override with `/pd keepinventory true|false`, independent of the
server's `keep_inventory` game rule. Players without an override simply follow the server's game rule, and
`/pd keepinventory reset` returns to that default.

Running `/pd` on its own opens a settings menu — a chest UI where players click to pick their difficulty and
cycle keep inventory, no command arguments needed. The menu works with completely vanilla clients; the
subcommands remain available for consoles and power users.
