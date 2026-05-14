package de.skyforce.main.elden;

import de.skyforce.main.elden.armor.registry.ArmorRegistry;
import de.skyforce.main.elden.armor.service.ArmorItemFactory;
import de.skyforce.main.elden.ashes.gui.AshApplyMenu;
import de.skyforce.main.elden.ashes.registry.AshOfWarRegistry;
import de.skyforce.main.elden.ashes.service.AshOfWarBindingService;
import de.skyforce.main.elden.ashes.service.AshOfWarItemFactory;
import de.skyforce.main.elden.boss.BossManager;
import de.skyforce.main.elden.boss.BossPortalManager;
import de.skyforce.main.elden.boss.remembrance.RemembranceMenu;
import de.skyforce.main.elden.boss.remembrance.RemembranceStationService;
import de.skyforce.main.elden.boss.registry.BossRegistry;
import de.skyforce.main.elden.classes.ClassLoadoutService;
import de.skyforce.main.elden.classes.ClassGuiService;
import de.skyforce.main.elden.classes.ClassManager;
import de.skyforce.main.elden.combat.DodgeManager;
import de.skyforce.main.elden.combat.StaminaManager;
import de.skyforce.main.elden.compass.CompassBarService;
import de.skyforce.main.elden.enemy.EnemyManager;
import de.skyforce.main.elden.enemy.EnemySpawnerManager;
import de.skyforce.main.elden.enemy.registry.EnemyRegistry;
import de.skyforce.main.elden.equipment.EquipmentWeightService;
import de.skyforce.main.elden.flask.FlaskService;
import de.skyforce.main.elden.flask.FlaskSoulboundListener;
import de.skyforce.main.elden.focus.FocusManager;
import de.skyforce.main.elden.grace.GraceGuiService;
import de.skyforce.main.elden.grace.GraceManager;
import de.skyforce.main.elden.item.CustomModelDataRegistry;
import de.skyforce.main.elden.level.LevelGuiService;
import de.skyforce.main.elden.level.LevelManager;
import de.skyforce.main.elden.mount.TorrentManager;
import de.skyforce.main.elden.persistence.PlayerDataRepository;
import de.skyforce.main.elden.player.PlayerGuiService;
import de.skyforce.main.elden.runes.RuneManager;
import de.skyforce.main.elden.smithing.gui.SmithingMenu;
import de.skyforce.main.elden.smithing.service.SmithingAnvilService;
import de.skyforce.main.elden.smithing.service.SmithingService;
import de.skyforce.main.elden.smithing.service.SmithingStoneService;
import de.skyforce.main.elden.spell.SpellListener;
import de.skyforce.main.elden.spell.registry.SpellRegistry;
import de.skyforce.main.elden.spell.service.SpellItemFactory;
import de.skyforce.main.elden.spirit.SpiritAshManager;
import de.skyforce.main.elden.spirit.registry.SpiritAshRegistry;
import de.skyforce.main.elden.spirit.service.SpiritAshItemFactory;
import de.skyforce.main.elden.spiritspring.SpiritSpringManager;
import de.skyforce.main.elden.talisman.gui.TalismanMenu;
import de.skyforce.main.elden.talisman.registry.TalismanRegistry;
import de.skyforce.main.elden.talisman.service.TalismanItemFactory;
import de.skyforce.main.elden.talisman.service.TalismanManager;
import de.skyforce.main.elden.visual.VisualEffectService;
import de.skyforce.main.elden.weapon.registry.WeaponRegistry;
import de.skyforce.main.elden.weapon.service.WeaponDamageService;
import de.skyforce.main.elden.weapon.service.WeaponGameplayService;
import de.skyforce.main.elden.weapon.service.WeaponItemFactory;
import org.bukkit.plugin.java.JavaPlugin;

public final class Elden extends JavaPlugin {

    private PlayerDataRepository playerDataRepository;
    private GraceManager graceManager;
    private GraceGuiService graceGuiService;
    private RuneManager runeManager;
    private StaminaManager staminaManager;
    private FocusManager focusManager;
    private DodgeManager dodgeManager;
    private CompassBarService compassBarService;
    private FlaskService flaskService;
    private ClassManager classManager;
    private ClassGuiService classGuiService;
    private LevelManager levelManager;
    private LevelGuiService levelGuiService;
    private FlaskSoulboundListener flaskSoulboundListener;
    private WeaponRegistry weaponRegistry;
    private WeaponItemFactory weaponItemFactory;
    private WeaponDamageService weaponDamageService;
    private WeaponGameplayService weaponGameplayService;
    private SmithingService smithingService;
    private SmithingStoneService smithingStoneService;
    private SmithingAnvilService smithingAnvilService;
    private SmithingMenu smithingMenu;
    private ArmorRegistry armorRegistry;
    private ArmorItemFactory armorItemFactory;
    private EquipmentWeightService equipmentWeightService;
    private PlayerGuiService playerGuiService;
    private AshOfWarRegistry ashOfWarRegistry;
    private AshOfWarItemFactory ashOfWarItemFactory;
    private AshOfWarBindingService ashOfWarBindingService;
    private AshApplyMenu ashApplyMenu;
    private SpellRegistry spellRegistry;
    private SpellItemFactory spellItemFactory;
    private SpellListener spellListener;
    private SpiritAshRegistry spiritAshRegistry;
    private SpiritAshItemFactory spiritAshItemFactory;
    private SpiritAshManager spiritAshManager;
    private SpiritSpringManager spiritSpringManager;
    private TalismanRegistry talismanRegistry;
    private TalismanItemFactory talismanItemFactory;
    private TalismanManager talismanManager;
    private TalismanMenu talismanMenu;
    private TorrentManager torrentManager;
    private CustomModelDataRegistry customModelDataRegistry;
    private BossRegistry bossRegistry;
    private BossManager bossManager;
    private BossPortalManager bossPortalManager;
    private RemembranceStationService remembranceStationService;
    private RemembranceMenu remembranceMenu;
    private EnemyRegistry enemyRegistry;
    private EnemyManager enemyManager;
    private EnemySpawnerManager enemySpawnerManager;
    private VisualEffectService visualEffectService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        playerDataRepository = new PlayerDataRepository(this);
        playerDataRepository.initialize();
        customModelDataRegistry = new CustomModelDataRegistry(this);
        visualEffectService = new VisualEffectService(this);

        staminaManager = new StaminaManager(this);
        focusManager = new FocusManager(playerDataRepository);
        flaskService = new FlaskService(this, staminaManager, focusManager, customModelDataRegistry, visualEffectService);
        flaskSoulboundListener = new FlaskSoulboundListener(flaskService);

        graceManager = new GraceManager(this, playerDataRepository);
        runeManager = new RuneManager(this, playerDataRepository, customModelDataRegistry);
        runeManager.setFocusManager(focusManager);
        compassBarService = new CompassBarService(this);
        classManager = new ClassManager(this, playerDataRepository);
        classGuiService = new ClassGuiService(this, classManager);
        levelManager = new LevelManager(this, playerDataRepository, runeManager, classManager, staminaManager, focusManager);
        runeManager.setLevelManager(levelManager);
        graceGuiService = new GraceGuiService(this, graceManager, flaskService, staminaManager, focusManager);

        weaponRegistry = new WeaponRegistry();
        weaponItemFactory = new WeaponItemFactory(this, customModelDataRegistry);
        smithingStoneService = new SmithingStoneService(this);
        smithingAnvilService = new SmithingAnvilService(this);
        smithingService = new SmithingService(this, weaponRegistry, weaponItemFactory, runeManager, smithingStoneService);
        weaponDamageService = new WeaponDamageService(weaponRegistry, weaponItemFactory, smithingService);
        smithingMenu = new SmithingMenu(this, smithingService, smithingStoneService, weaponItemFactory, runeManager);
        armorRegistry = new ArmorRegistry();
        armorItemFactory = new ArmorItemFactory(this, customModelDataRegistry);
        equipmentWeightService = new EquipmentWeightService(levelManager, weaponRegistry, weaponItemFactory, armorRegistry, armorItemFactory);
        dodgeManager = new DodgeManager(this, equipmentWeightService, visualEffectService);
        levelGuiService = new LevelGuiService(this, levelManager, runeManager, equipmentWeightService, visualEffectService);
        spellRegistry = new SpellRegistry();
        spellItemFactory = new SpellItemFactory(this, customModelDataRegistry);
        ashOfWarRegistry = new AshOfWarRegistry();
        ashOfWarItemFactory = new AshOfWarItemFactory(this, customModelDataRegistry);
        ashOfWarBindingService = new AshOfWarBindingService(this, weaponRegistry, weaponItemFactory);
        spiritAshRegistry = new SpiritAshRegistry();
        spiritAshItemFactory = new SpiritAshItemFactory(this, customModelDataRegistry);
        spiritAshManager = new SpiritAshManager(this);
        spiritSpringManager = new SpiritSpringManager(this);
        talismanRegistry = new TalismanRegistry();
        talismanItemFactory = new TalismanItemFactory(this, customModelDataRegistry);
        talismanManager = new TalismanManager(this, playerDataRepository, talismanRegistry, levelManager, staminaManager, focusManager);
        talismanMenu = new TalismanMenu(this, talismanRegistry, talismanItemFactory, talismanManager);
        levelManager.setDerivedStatsPostProcessor(talismanManager::applyPassiveStats);
        staminaManager.setRegenMultiplierProvider(talismanManager::staminaRegenMultiplier);
        equipmentWeightService.setEquipLoadMultiplierProvider(talismanManager::equipLoadMultiplier);
        runeManager.setRuneGainMultiplierProvider(talismanManager::runeGainMultiplier);
        torrentManager = new TorrentManager(this, customModelDataRegistry);
        bossRegistry = new BossRegistry();
        bossManager = new BossManager(
                this,
                runeManager,
                bossRegistry,
                playerDataRepository,
                weaponRegistry,
                weaponItemFactory,
                ashOfWarRegistry,
                ashOfWarItemFactory,
                spellRegistry,
                spellItemFactory,
                spiritAshRegistry,
                spiritAshItemFactory,
                talismanRegistry,
                talismanItemFactory
        );
        bossManager.loadConfiguredBosses(bossRegistry);
        bossPortalManager = new BossPortalManager(this, bossRegistry, bossManager);
        remembranceStationService = new RemembranceStationService(this);
        remembranceMenu = new RemembranceMenu(bossManager);
        enemyRegistry = new EnemyRegistry();
        enemyManager = new EnemyManager(this, runeManager, smithingStoneService);
        enemySpawnerManager = new EnemySpawnerManager(this, enemyRegistry, enemyManager);
        enemyManager.setSpawnerManager(enemySpawnerManager);
        flaskService.setTorrentManager(torrentManager);
        torrentManager.setFlaskService(flaskService);
        torrentManager.setSpiritSpringManager(spiritSpringManager);
        classManager.setLoadoutService(new ClassLoadoutService(
                weaponRegistry,
                weaponItemFactory,
                armorRegistry,
                armorItemFactory,
                spellRegistry,
                spellItemFactory
        ));
        playerGuiService = new PlayerGuiService(this, levelManager, runeManager, classManager, equipmentWeightService, staminaManager, focusManager, spellRegistry);

        weaponGameplayService = new WeaponGameplayService(
                this,
                weaponRegistry,
                weaponItemFactory,
                weaponDamageService,
                ashOfWarBindingService,
                levelManager,
                focusManager,
                staminaManager,
                runeManager
        );
        ashApplyMenu = new AshApplyMenu(this, ashOfWarRegistry, ashOfWarItemFactory, ashOfWarBindingService, weaponRegistry, weaponItemFactory);

        new EldenCommandRegistrar(this).register(
                graceManager,
                runeManager,
                classManager,
                classGuiService,
                levelManager,
                levelGuiService,
                weaponRegistry,
                weaponItemFactory,
                smithingService,
                smithingAnvilService,
                smithingStoneService,
                armorRegistry,
                armorItemFactory,
                playerGuiService,
                ashOfWarRegistry,
                ashOfWarItemFactory,
                spellRegistry,
                spellItemFactory,
                spiritAshRegistry,
                spiritAshItemFactory,
                spiritSpringManager,
                talismanRegistry,
                talismanItemFactory,
                talismanManager,
                talismanMenu,
                torrentManager,
                bossRegistry,
                bossManager,
                bossPortalManager,
                remembranceStationService,
                enemyRegistry,
                enemyManager,
                enemySpawnerManager
        );

        spellListener = new EldenListenerRegistrar(this).register(
                graceManager,
                graceGuiService,
                flaskService,
                flaskSoulboundListener,
                enemySpawnerManager,
                visualEffectService,
                runeManager,
                staminaManager,
                dodgeManager,
                levelManager,
                weaponDamageService,
                weaponGameplayService,
                focusManager,
                talismanManager,
                smithingAnvilService,
                smithingMenu,
                equipmentWeightService,
                classManager,
                classGuiService,
                levelGuiService,
                playerGuiService,
                torrentManager,
                ashApplyMenu,
                ashOfWarRegistry,
                ashOfWarItemFactory,
                ashOfWarBindingService,
                weaponRegistry,
                weaponItemFactory,
                spellRegistry,
                spellItemFactory,
                spiritAshRegistry,
                spiritAshItemFactory,
                spiritAshManager,
                talismanMenu,
                bossManager,
                bossPortalManager,
                remembranceStationService,
                remembranceMenu,
                enemyManager
        );

        runeManager.startActionBarTask();
        staminaManager.startRegenTask();
        compassBarService.start();

        getLogger().info("Site of Grace, flasks, runes, classes, leveling, combat system, HP/FP/Stamina, compass bossbar, weapons, smithing, armor, player profile, ashes of war, spirit ashes, spells and bosses enabled.");
    }

    @Override
    public void onDisable() {
        if (graceManager != null) {
            graceManager.saveAll();
        }

        if (runeManager != null) {
            runeManager.stopActionBarTask();
            runeManager.saveAll();
        }

        if (staminaManager != null) {
            staminaManager.stopRegenTask();
        }

        if (compassBarService != null) {
            compassBarService.stop();
        }

        if (classManager != null) {
            classManager.saveAll();
        }

        if (levelManager != null) {
            levelManager.saveAll();
        }

        if (spellListener != null) {
            spellListener.shutdown();
        }

        if (torrentManager != null) {
            torrentManager.shutdown();
        }

        if (spiritSpringManager != null) {
            spiritSpringManager.shutdown();
        }

        if (spiritAshManager != null) {
            spiritAshManager.shutdown();
        }

        if (bossManager != null) {
            bossManager.shutdown();
        }

        if (bossPortalManager != null) {
            bossPortalManager.shutdown();
        }

        if (enemySpawnerManager != null) {
            enemySpawnerManager.shutdown();
        }

        if (enemyManager != null) {
            enemyManager.shutdown();
        }

        if (focusManager != null) {
            focusManager.saveAll();
        }

        if (playerDataRepository != null) {
            playerDataRepository.close();
        }
    }
}
