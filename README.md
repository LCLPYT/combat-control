# combat-control
A Fabric mod that aims brings to bring back 1.8 combat to newer Minecraft versions.

## Commands
Toggle combat for certain players with the `/combat` command:
```
# set your own combat to the old 1.8 pvp
/combat old @s

# set everyones combat to 1.8 
/combat old @a

# set modern (1.9+) combat for a certain player
/combat modern <player>
```

## Config settings
You can set the default combat mode in the `config/combat-control/config.json` file:

```json
{
  "combat-style": "old" 
}
```

## Acknowledgements
This mod is heavily inspired by [Golden Age Combat](https://github.com/Fuzss/goldenagecombat) by Fuzss.
Some files and methods are literally copied from it and remapped into yarn mappings.
Code in this project that is originally from Golden Age Combat is annotated with a comment or doc-comment.

The reason why this project exists as a separate mod, is to make the combat configurable for each player, alongside with some other adjustments.

This project is licensed under the terms of the MPL-2.0 license.

## API for developers
Combat-control offers an API for developers that allows for changing each players' combat style and features.
The API offers fine-grained control over every combat-control feature, for every player individually.

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
    modImplementation "work.lclpnet.mods:combat-control:1.0.0+1.20.1"
}
```

### Accessing the API
You can easily access all API features through the `CombatControl` class.
To get an instance, you need a `MinecraftServer` instance:

```java
import work.lclpnet.combatctl.api.CombatControl;

// ...
CombatControl control = CombatControl.get(server);
```

### Setting the global default combat style
Combat-control offers a lot of features that can enabled individually.
To simplify the configuration process, combat-control has the concept of `CombatStyle`s.
Each combat style defines a preset of enabled or disabled features.
E.g. `CombatStyle.OLD` defines `attackCooldown=false` etc.

To change the current global default combat style, you can use:
```java
CombatControl control = CombatControl.get(server);
control.setStyle(CombatStyle.OLD);
```

### Setting the combat style for a specific player
Combat styles can also be configured for each `ServerPlayerEntity` individually:
```java
CombatControl control = CombatControl.get(server);
control.setStyle(player, CombatStyle.OLD);
```

### Toggle specific features for a player
As mentioned before, the API offers fine-grained control over every single feature.
All features are configured using a `CombatConfig` instance.
Each `ServerPlayerEntity` has their own config, you can get it via:
```java
CombatControl control = CombatControl.get(server);
CombatConfig config = control.getConfig(player);
```

You can then enable or disable each feature individually, using the corresponding setters.

### Combat features
Here is a list of features that can be modified in a `CombatConfig`:

| name                       | default (modern) | description                                                                                                          |
|----------------------------|------------------|----------------------------------------------------------------------------------------------------------------------|
| attackCooldown             | true             | Whether attacking should have a cooldown or not. Disabling the cooldown removes the cooldown indicator.              |
| modernHitSounds            | true             | Controls whether the modern hit sounds are played or not.                                                            |
| modernHitParticle          | true             | Shows modern attacking particle, such as damage_indicator or sweep_attack.                                           |
| sweepAttack                | true             | Enables the sweep attack when using a sword and standing still.                                                      |
| modernRegeneration         | true             | Use modern regeneration with fast health gain when saturation is full and overall faster healing.                    |
| modernNotchApple           | true             | The enchanted golden apple gives Absorption 4 and Regeneration 2.                                                    |
| noWeakAttackKnockBack      | true             | Disables the knock back from attacks with zero damage, such as snowballs or eggs hitting entities.                   |
| noFishingRodKnockBack      | true             | Disables the knock back from fishing rods when hitting entities.                                                     |
| strongKnockBackInAir       | false            | Apply stronger knock back to airborne targets.                                                                       |
| noSprintCriticalHits       | true             | Disable sprinting and attacking at the same time.                                                                    |
| noAttackSprinting          | true             | Stop players from sprinting when attacking entities.                                                                 |
| fishingRodLaunch           | false            | Entities pulled by fishing rods are boosted slightly upwards.                                                        |
| modernFishingRodDurability | true             | Pulling entities with fishing rods reduces the durability by 5.                                                      |
| attackWhileUsing           | false            | Allow attacking while using items like a bow or food. Enables the old animation when mod is installed on the client. |
| modernItemDurability       | true             | Reduce the durability of tools by 2 when attacking.                                                                  |
