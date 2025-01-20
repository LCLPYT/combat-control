# 🗡️ combat-control 🛡️
Bring back classic combat from 1.8.9 and earlier. Remove the attack cooldown and more!

> [!IMPORTANT]
> This mod doesn't change the combat by default.
> You have to use `/combat style minecraft:classic` to enable classic combat. 

All features of this mod can be configured individually, on a per-player basis.
You can have some players using attack cooldown while others don't.

The mod is **only required on the server**, but having the mod installed on your client makes the experience better.

## Requirements
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) (client only)

## Features
- remove the attack cooldown
- reintroduce sword blocking
- axes and other tools deal less damage, like in 1.8.9
- revert food mechanics and fast regeneration
- make snowballs inflict knockback again
- make fishing rods inflict knockback
- disable fishing rod pulling, like on many PvP servers (opt-in)
- allow attacking while using a bow or eating, like in 1.7.10 (opt-in)
- disable sweep attack with a sword when standing still. The Sweeping Edge enchantment will still trigger a sweep attack
- make critical hits possible while sprinting
- attacking no longer stops you from sprinting
- buff sharpness enchantment to add 1.25 attack damage per level, like in 1.8.9 and earlier
- remove modern combat sounds
- remove modern combat particles, such as the damage indicator
- revert enchanted golden apple ("notch apple") effects
- fishing rods pull entities slightly more upwards
- fishing rods take less durability damage when pulling entities
- make fishing rods play the old sounds
- the fishing rod bobber moves faster 
- tools will take less durability damage when attacking with them
- remove the re-equip animation when using items like shields
- old bobbing animation with head tilt when jumping
- instant sneak animation
- old attribute style on weapons
- remove flashing hearts animation

## Commands
Change the combat style with the `/combat style` command:
```
# set the global combat style to classic 
/combat style minecraft:classic

# switch back to modern (1.9+) combat with attack cooldown etc.
/combat style minecraft:modern

# you can also change the combat style only for certain players, e.g. yourself in this example
/combat style minecraft:classic @s
```

Setting a style will configure a set of features at once.
The `minecraft:classic` style, for example, disables attack cooldown, reverts the food regeneration system, changes behaviour of fishing rods and more.

However, you can also configure each feature individually, per-player:
```
# disable only attack cooldown globally
/combat set attackCooldown false

# make the enchanted golden apple grant regeneration 5 and absorption 1, instead of regeneration 2 and absorption 4
/combat set modernNotchApple false

# use 1.8 and earlier regeneration food mechanics
/combat set modernRegeneration false

# as with the combat style, you can also only configure features for certain players, e.g. disable attack cooldown for yourself:
/combat set attackCooldown false @s

# you can get the current setting of a feature like this:
/combat get attackCooldown

# get a setting of a specific player
/combat get attackCooldown @s
```

Some features, like `modernDamageValues`, don't work for individual players and can only be set globally.

### Permissions
To execute those commands, **you need to be a server operator** (have OP) or have the corresponding permissions:

| Feature                                       | Required permission                                      |
|-----------------------------------------------|----------------------------------------------------------|
| Use combat command                            | combat-control.command.combat                            |
| Set combat features                           | combat-control.command.combat.set                        |
| Set a combat feature                          | combat-control.command.combat.set.&lt;feature&gt;        |
| Set a combat feature globally                 | combat-control.command.combat.set.global.&lt;feature&gt; |
| Get combat features                           | combat-control.command.combat.get                        |
| Get a combat feature (globally, individually) | combat-control.command.combat.get.&lt;feature&gt;        |
| Set combat styles                             | combat-control.command.combat.style                      |
| Set combat style globally                     | combat-control.command.combat.style.global               |


## Config
You can configure all features in `config/combat-control/config.toml`.
Once you save the config, any changes will automatically be loaded. 
No need to restart your game or reload anything.

If you have ModMenu installed on the client, you can also configure all features from there.

## Acknowledgements
This mod is heavily inspired by [Golden Age Combat](https://github.com/Fuzss/goldenagecombat) by Fuzss.
Some methods are literally copied from it and remapped into yarn mappings.
Code in this project that is originally from Golden Age Combat is annotated with a comment or doc-comment.
The MPL-2.0 license from Golden Age Combat is respected; this project uses the same license. 

The reason why this project exists as a separate mod, is to make the combat configurable for each player, alongside with some other adjustments.

Also, the `PING_ADJUSTED` knockback variant is heavily inspired in functionality by the awesome [knockback-sync](https://github.com/CASELOAD7000/knockback-sync) plugin by CASELOAD7000 for Bukkit, Folia and Fabric.
It calculates a player's knockback depending on their network latency.
The values for the gravity simulation are taken from the Minecraft movement code and from [Minecraft Movement Tools](https://github.com/OrHy3/MinecraftMotionTools) by OrHy3.

I got the motivation for implementing different knockback variants from [this video](https://www.youtube.com/watch?v=SVokpr3v-TA) by Intel Edits. 

## API for developers
Combat-control offers an API for developers that allows for changing each players' combat style and features.
The API offers fine-grained control of every combat-control feature, for every player.

### Gradle Dependency
To get started, add the following repository to your gradle build script:
```groovy
repositories {
    maven {
        url "https://repo.lclpnet.work/repository/internal"
    }
}
```

Now you can define the following mod dependency:
```groovy
dependencies {
    modImplementation "work.lclpnet.mods:combat-control:1.5.0+1.21.4"
}
```

### Accessing the API
You can easily access all API features through the `CombatControl` class.
To get an instance, you need a `MinecraftServer` instance:

```java
import work.lclpnet.combatctl.api.CombatControl;

CombatControl control = CombatControl.get(server);
```

### Setting the global default combat style
Combat-control offers a lot of features that can enabled individually.
To simplify the configuration process, combat-control has the concept of `CombatStyle`s.
Each combat style defines a preset of enabled or disabled features.
E.g. `CombatStyle.OLD` defines `attackCooldown=false` etc.

To change the current global default combat style, you can use:
```java
import work.lclpnet.combatctl.impl.CombatStyles;

CombatControl control = CombatControl.get(server);
control.setStyle(CombatStyles.CLASSIC);
```

### Setting the combat style for a specific player
Combat styles can also be configured for each `ServerPlayerEntity` individually:
```java
import work.lclpnet.combatctl.impl.CombatStyles;

CombatControl control = CombatControl.get(server);
control.setStyle(player, CombatStyles.CLASSIC);
```

### Toggle specific features for a player
As mentioned before, the API offers fine-grained control over every single feature.
Each `ServerPlayerEntity` has their own config, you can configure it with:
```java
CombatControl control = CombatControl.get(server);
control.configurePlayer(player, config -> {
    // change player config here
    config.setAttackCooldown(false);    
});
```