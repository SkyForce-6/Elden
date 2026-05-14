package de.skyforce.main.elden;

import de.skyforce.main.elden.ashes.AshOfWarListener;
import de.skyforce.main.elden.ashes.gui.AshApplyMenu;
import de.skyforce.main.elden.ashes.gui.AshApplyMenuListener;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarBindingService;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.boss.BossListener;
import de.skyforce.main.elden.boss.BossManager;
import de.skyforce.main.elden.boss.BossPortalListener;
import de.skyforce.main.elden.boss.BossPortalManager;
import de.skyforce.main.elden.boss.remembrance.RemembranceListener;
import de.skyforce.main.elden.boss.remembrance.RemembranceMenu;
import de.skyforce.main.elden.boss.remembrance.RemembranceMenuListener;
import de.skyforce.main.elden.boss.remembrance.RemembranceStationService;
import de.skyforce.main.elden.classes.ClassGuiListener;
import de.skyforce.main.elden.classes.ClassGuiService;
import de.skyforce.main.elden.classes.ClassListener;
import de.skyforce.main.elden.classes.ClassManager;
import de.skyforce.main.elden.combat.CombatListener;
import de.skyforce.main.elden.combat.DodgeManager;
import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.enemy.EnemyListener;
import de.skyforce.main.elden.enemy.EnemyManager;
import de.skyforce.main.elden.enemy.EnemySpawnerManager;
import de.skyforce.main.elden.equipment.EquipmentWeightListener;
import de.skyforce.main.elden.equipment.EquipmentWeightService;
import de.skyforce.main.elden.flask.FlaskListener;
import de.skyforce.main.elden.flask.FlaskService;
import de.skyforce.main.elden.flask.FlaskSoulboundListener;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.grace.GraceGuiListener;
import de.skyforce.main.elden.grace.GraceGuiService;
import de.skyforce.main.elden.grace.GraceListener;
import de.skyforce.main.elden.grace.GraceManager;
import de.skyforce.main.elden.level.LevelGuiListener;
import de.skyforce.main.elden.level.LevelGuiService;
import de.skyforce.main.elden.level.LevelListener;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.mount.TorrentListener;
import de.skyforce.main.elden.mount.TorrentManager;
import de.skyforce.main.elden.player.PlayerGuiListener;
import de.skyforce.main.elden.player.PlayerGuiService;
import de.skyforce.main.elden.runes.RuneListener;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.SmithingListener;
import de.skyforce.main.elden.smithing.gui.SmithingMenu;
import de.skyforce.main.elden.smithing.gui.SmithingMenuListener;
import de.skyforce.main.elden.smithing.service.SmithingAnvilService;
import de.skyforce.main.elden.spell.SpellListener;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import de.skyforce.main.elden.spirit.SpiritAshListener;
import de.skyforce.main.elden.spirit.SpiritAshManager;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import de.skyforce.main.elden.talisman.gui.TalismanMenu;
import de.skyforce.main.elden.talisman.gui.TalismanMenuListener;
import de.skyforce.main.elden.talisman.service.TalismanManager;
import de.skyforce.main.elden.visual.VisualEffectService;
import de.skyforce.main.elden.weapon.WeaponSkillListener;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponDamageService;
import de.skyforce.main.elden.weapon.service.WeaponGameplayService;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import org.bukkit.event.Listener;

final class EldenListenerRegistrar {

    private final Elden plugin;

    EldenListenerRegistrar(Elden plugin) {
        this.plugin = plugin;
    }

    SpellListener register(
            GraceManager graceManager,
            GraceGuiService graceGuiService,
            FlaskService flaskService,
            FlaskSoulboundListener flaskSoulboundListener,
            EnemySpawnerManager enemySpawnerManager,
            VisualEffectService visualEffectService,
            RuneManager runeManager,
            StaminaManager staminaManager,
            DodgeManager dodgeManager,
            LevelManager levelManager,
            WeaponDamageService weaponDamageService,
            WeaponGameplayService weaponGameplayService,
            FocusManager focusManager,
            TalismanManager talismanManager,
            SmithingAnvilService smithingAnvilService,
            SmithingMenu smithingMenu,
            EquipmentWeightService equipmentWeightService,
            ClassManager classManager,
            ClassGuiService classGuiService,
            LevelGuiService levelGuiService,
            PlayerGuiService playerGuiService,
            TorrentManager torrentManager,
            AshApplyMenu ashApplyMenu,
            AshOfWarRegistry ashOfWarRegistry,
            AshOfWarItemFactory ashOfWarItemFactory,
            AshOfWarBindingService ashOfWarBindingService,
            WeaponRegistry weaponRegistry,
            WeaponItemFactory weaponItemFactory,
            SpellRegistry spellRegistry,
            SpellItemFactory spellItemFactory,
            SpiritAshRegistry spiritAshRegistry,
            SpiritAshItemFactory spiritAshItemFactory,
            SpiritAshManager spiritAshManager,
            TalismanMenu talismanMenu,
            BossManager bossManager,
            BossPortalManager bossPortalManager,
            RemembranceStationService remembranceStationService,
            RemembranceMenu remembranceMenu,
            EnemyManager enemyManager
    ) {
        register(flaskSoulboundListener);
        register(new GraceListener(graceManager, graceGuiService, flaskService, enemySpawnerManager, visualEffectService));
        register(new GraceGuiListener(graceGuiService));
        register(new RuneListener(runeManager));

        CombatListener combatListener = new CombatListener(
                staminaManager,
                dodgeManager,
                levelManager,
                weaponDamageService,
                weaponGameplayService,
                focusManager
        );
        combatListener.setPhysicalDefenseMultiplierProvider(talismanManager::physicalDefenseMultiplier);
        register(combatListener);

        weaponGameplayService.setJumpAttackMultiplierProvider(talismanManager::jumpAttackDamageMultiplier);
        weaponGameplayService.setStatusBuildupMultiplierProvider(talismanManager::statusBuildupMultiplier);
        weaponGameplayService.setCastSpeedMultiplierProvider(talismanManager::castSpeedMultiplier);
        register(new WeaponSkillListener(weaponGameplayService));
        register(new SmithingListener(smithingAnvilService, smithingMenu));
        register(new SmithingMenuListener(smithingMenu));
        register(new EquipmentWeightListener(plugin, equipmentWeightService));
        register(new FlaskListener(plugin, flaskService));
        register(new ClassListener(plugin, classManager, classGuiService));
        register(new ClassGuiListener(classGuiService));
        register(new LevelListener(levelManager));
        register(new LevelGuiListener(levelGuiService));
        register(new PlayerGuiListener(playerGuiService));
        register(new TorrentListener(torrentManager));
        register(new AshApplyMenuListener(ashApplyMenu));
        register(new AshOfWarListener(
                plugin,
                ashOfWarRegistry,
                ashOfWarItemFactory,
                ashOfWarBindingService,
                focusManager,
                weaponRegistry,
                weaponItemFactory,
                ashApplyMenu
        ));

        SpellListener spellListener = new SpellListener(
                plugin,
                spellRegistry,
                spellItemFactory,
                focusManager,
                levelManager
        );
        spellListener.setCastSpeedMultiplierProvider(talismanManager::castSpeedMultiplier);
        register(spellListener);

        register(new SpiritAshListener(
                spiritAshRegistry,
                spiritAshItemFactory,
                spiritAshManager,
                focusManager
        ));
        register(new TalismanMenuListener(plugin, talismanMenu, talismanManager));
        register(new BossListener(bossManager));
        register(new BossPortalListener(bossPortalManager));
        register(new RemembranceListener(remembranceStationService, remembranceMenu));
        register(new RemembranceMenuListener(remembranceMenu));
        register(new EnemyListener(enemyManager));

        return spellListener;
    }

    private void register(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
