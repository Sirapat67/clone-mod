CLONE MOD (Minecraft Java 1.20.1, Forge)
=========================================

HOW IT WORKS
- Every time you level up, a clone of you appears (with your skin).
- Right-click a clone to cycle its job:
    Following you -> Standing still -> Cutting wood -> Mining ore -> Farming crops -> Guarding
- Shift + right-click = "follow me" again.
- The spot where the clone is standing when you pick a job becomes its work area.
- Drops fall on the ground near where the work happens. Pick them up yourself.
- Max 20 clones per player (change MAX_CLONES_PER_PLAYER in ForgeEvents.java).

HOW TO BUILD IT
1. Install Java 17 (Temurin 17 is fine).
2. Download the Forge MDK for 1.20.1 from https://files.minecraftforge.net  (Minecraft 1.20.1 -> "Mdk").
3. Unzip the MDK somewhere, e.g. C:\clone-mod-build
4. In the MDK folder, open gradle.properties and set:
       mod_id=clonemod
       mod_group_id=com.clonemod
       mod_name=Clone Mod
5. Delete the folder  src/main/java/com/example  (the MDK's example code).
6. Copy this zip's  src  folder into the MDK folder and let it REPLACE the existing src folder
   (so src/main/java/com/clonemod/... and src/main/resources/... end up there).
7. Open a terminal in the MDK folder and run:
       Windows:  gradlew build
       Mac/Linux: ./gradlew build
8. Your mod jar is in  build/libs/  -> put it in your Minecraft  mods  folder
   (needs Forge 1.20.1 installed). To test quickly without installing: run  gradlew runClient
   and then in game:  /xp add @s 5 levels
