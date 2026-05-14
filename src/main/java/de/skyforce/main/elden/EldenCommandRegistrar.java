package de.skyforce.main.elden;

import de.skyforce.main.elden.armor.ArmorCommand;
import de.skyforce.main.elden.armor.registry.ArmorRegistry;
import de.skyforce.main.elden.armor.service.ArmorItemFactory;
import de.skyforce.main.elden.ashes.AshOfWarCommand;
import de.skyforce.main.elden.ashes.AshOfWarTabCompleter;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.boss.BossCommand;
import de.skyforce.main.elden.boss.BossManager;
import de.skyforce.main.elden.boss.BossPortalManager;
import de.skyforce.main.elden.boss.BossTabCompleter;
import de.skyforce.main.elden.boss.remembrance.RemembranceStationService;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import de.skyforce.main.elden.classes.ClassCommand;
import de.skyforce.main.elden.classes.ClassGuiService;
import de.skyforce.main.elden.classes.ClassManager;
import de.skyforce.main.elden.enemy.EnemyCommand;
import de.skyforce.main.elden.enemy.EnemyManager;
import de.skyforce.main.elden.enemy.EnemySpawnerManager;
import de.skyforce.main.elden.enemy.EnemyTabCompleter;
import de.skyforce.main.elden.enemy.registry.EnemyRegistry;
import de.skyforce.main.elden.grace.GraceCommand;
import de.skyforce.main.elden.grace.GraceManager;
import de.skyforce.main.elden.level.LevelCommand;
import de.skyforce.main.elden.level.LevelGuiService;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.mount.TorrentCommand;
import de.skyforce.main.elden.mount.TorrentManager;
import de.skyforce.main.elden.player.PlayerCommand;
import de.skyforce.main.elden.player.PlayerGuiService;
import de.skyforce.main.elden.runes.RuneCommand;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.SmithingCommand;
import de.skyforce.main.elden.smithing.SmithingTabCompleter;
import de.skyforce.main.elden.smithing.service.SmithingAnvilService;
import de.skyforce.main.elden.smithing.service.SmithingService;
import de.skyforce.main.elden.smithing.service.SmithingStoneService;
import de.skyforce.main.elden.spell.SpellCommand;
import de.skyforce.main.elden.spell.SpellTabCompleter;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import de.skyforce.main.elden.spirit.SpiritAshCommand;
import de.skyforce.main.elden.spirit.SpiritAshTabCompleter;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import de.skyforce.main.elden.spiritspring.SpiritSpringCommand;
import de.skyforce.main.elden.spiritspring.SpiritSpringManager;
import de.skyforce.main.elden.spiritspring.SpiritSpringTabCompleter;
import de.skyforce.main.elden.talisman.TalismanCommand;
import de.skyforce.main.elden.talisman.gui.TalismanMenu;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import de.skyforce.main.elden.talisman.service.TalismanItemFactory;
import de.skyforce.main.elden.talisman.service.TalismanManager;
import de.skyforce.main.elden.weapon.WeaponCommand;
import de.skyforce.main.elden.weapon.WeaponTabCompleter;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import java.util.Objects;

final class EldenCommandRegistrar {

    private final Elden plugin;

    EldenCommandRegistrar(Elden plugin) {
        this.plugin = plugin;
    }

    void register(
            GraceManager graceManager,
            RuneManager runeManager,
            ClassManager classManager,
            ClassGuiService classGuiService,
            LevelManager levelManager,
            LevelGuiService levelGuiService,
            WeaponRegistry weaponRegistry,
            WeaponItemFactory weaponItemFactory,
            SmithingService smithingService,
            SmithingAnvilService smithingAnvilService,
            SmithingStoneService smithingStoneService,
            ArmorRegistry armorRegistry,
            ArmorItemFactory armorItemFactory,
            PlayerGuiService playerGuiService,
            AshOfWarRegistry ashOfWarRegistry,
            AshOfWarItemFactory ashOfWarItemFactory,
            SpellRegistry spellRegistry,
            SpellItemFactory spellItemFactory,
            SpiritAshRegistry spiritAshRegistry,
            SpiritAshItemFactory spiritAshItemFactory,
            SpiritSpringManager spiritSpringManager,
            TalismanRegistry talismanRegistry,
            TalismanItemFactory talismanItemFactory,
            TalismanManager talismanManager,
            TalismanMenu talismanMenu,
            TorrentManager torrentManager,
            BossRegistry bossRegistry,
            BossManager bossManager,
            BossPortalManager bossPortalManager,
            RemembranceStationService remembranceStationService,
            EnemyRegistry enemyRegistry,
            EnemyManager enemyManager,
            EnemySpawnerManager enemySpawnerManager
    ) {
        command("grace").setExecutor(new GraceCommand(graceManager));
        command("runes").setExecutor(new RuneCommand(runeManager));
        command("eldenclass").setExecutor(new ClassCommand(classManager, classGuiService));
        command("level").setExecutor(new LevelCommand(levelManager, levelGuiService));

        WeaponCommand weaponCommand = new WeaponCommand(weaponRegistry, weaponItemFactory);
        command("weapon").setExecutor(weaponCommand);
        command("weapon").setTabCompleter(new WeaponTabCompleter(weaponRegistry));

        SmithingCommand smithingCommand = new SmithingCommand(smithingService, smithingAnvilService, smithingStoneService);
        command("smithing").setExecutor(smithingCommand);
        command("smithing").setTabCompleter(new SmithingTabCompleter());

        command("armor").setExecutor(new ArmorCommand(armorRegistry, armorItemFactory));
        command("profile").setExecutor(new PlayerCommand(playerGuiService));

        AshOfWarCommand ashOfWarCommand = new AshOfWarCommand(ashOfWarRegistry, ashOfWarItemFactory);
        command("ash").setExecutor(ashOfWarCommand);
        command("ash").setTabCompleter(new AshOfWarTabCompleter(ashOfWarRegistry));

        SpellCommand spellCommand = new SpellCommand(spellRegistry, spellItemFactory);
        command("spell").setExecutor(spellCommand);
        command("spell").setTabCompleter(new SpellTabCompleter(spellRegistry));

        SpiritAshCommand spiritAshCommand = new SpiritAshCommand(spiritAshRegistry, spiritAshItemFactory);
        command("spiritash").setExecutor(spiritAshCommand);
        command("spiritash").setTabCompleter(new SpiritAshTabCompleter(spiritAshRegistry));
        command("spiritspring").setExecutor(new SpiritSpringCommand(spiritSpringManager));
        command("spiritspring").setTabCompleter(new SpiritSpringTabCompleter(spiritSpringManager));

        command("talisman").setExecutor(new TalismanCommand(talismanRegistry, talismanItemFactory, talismanManager, talismanMenu));
        command("torrent").setExecutor(new TorrentCommand(torrentManager));

        command("boss").setExecutor(new BossCommand(bossRegistry, bossManager, bossPortalManager, remembranceStationService));
        command("boss").setTabCompleter(new BossTabCompleter(bossRegistry, bossManager, bossPortalManager));

        EnemyCommand enemyCommand = new EnemyCommand(enemyRegistry, enemyManager, enemySpawnerManager);
        command("enemy").setExecutor(enemyCommand);
        command("enemy").setTabCompleter(new EnemyTabCompleter(enemyRegistry, enemySpawnerManager));
    }

    private org.bukkit.command.PluginCommand command(String name) {
        return Objects.requireNonNull(plugin.getCommand(name), "Command '" + name + "' is missing in plugin.yml");
    }
}
