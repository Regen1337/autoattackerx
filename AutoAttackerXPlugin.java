package net.runelite.client.plugins.autoattackerx;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.LocatableQueryResults;
import net.runelite.api.NPC;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.TileItems;
import net.unethicalite.api.entities.TileObjects;
import net.unethicalite.api.items.Bank;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.Reachable;
import net.unethicalite.api.movement.pathfinder.model.BankLocation;
import net.unethicalite.client.Static;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.commons.Time;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.queries.GameObjectQuery;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;

@PluginDescriptor(
        name = "Auto Attacker X",
        description = "Automatically attacks NPCs and banks, eats food, and uses potions",
        tags = {"@nukeafrica", "@autoattackerv2"},
        enabledByDefault = false
)

@Slf4j
public class AutoAttackerXPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private AutoAttackerXConfig config;

    static final String CONFIG_GROUP = "autoattackerX";
    private State currentState;
    private State previousState;
    private State nextState;

    private WorldPoint itemDropLocation;
    private WorldPoint savedLocation;

    public enum State
    {
        ATTACKING(false),
        LOOTING(false),
        BANKING(false),
        EATING(false),
        BURYING(false),
        USING_POTIONS(false),
        OPENING_BANK(false),
        NAVIGATING_TO_BANK(false),
        NAVIGATING_BACK_FROM_BANK(false),
        NAVIGATING_TO_ITEM(false),
        NAVIGATING_TO_NPC(false),
        IDLING(false),
        LOGGING_IN(false),
        LOGGED_OUT(false);

        private boolean hasEntered;
        private boolean isComplete;

        State(boolean hasEntered) {
          this.hasEntered = hasEntered;
          this.isComplete = false;
        }
  
        public boolean hasEntered() {
            return hasEntered;
        }

        public boolean isComplete() {
            return isComplete;
        }

        public void setIsComplete(boolean isComplete) {
            this.isComplete = isComplete;
        }

        public void setHasEntered(boolean hasEntered) {
            this.hasEntered = hasEntered;
        }
    }

    @Provides
    AutoAttackerXConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoAttackerXConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        log.info("atv2: Plugin started");
        previousState = State.IDLING;
        currentState = State.IDLING;
        nextState = State.IDLING;
        itemDropLocation = null;
        savedLocation = null;
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("atv2: Plugin stopped");
        changeState(State.IDLING);
        itemDropLocation = null;
        savedLocation = null;
    }

    public void changeState(State newState)
    {
        previousState = currentState;
        currentState = newState;
        nextState = newState;
        currentState.setHasEntered(false);
        currentState.setIsComplete(false);
    }

    public ArrayList<String> getLootList()
    {
        ArrayList<String> lootArray = new ArrayList<String>(100);
        String[] lootList = config.lootList().split(",");
        for (String loot : lootList)
        {
            lootArray.add(loot.toLowerCase());
        }
        return lootArray;
    }

    public ArrayList<String> getDepositBlacklist()
    {
        ArrayList<String> depositArray = new ArrayList<String>(100);
        String[] depositList = config.depositBlacklist().split(",");
        for (String deposit : depositList)
        {
            depositArray.add(deposit.toLowerCase());
        }
        return depositArray;
    }

    public ArrayList<String> getFoodList()
    {
        ArrayList<String> foodArray = new ArrayList<String>(100);
        String[] foodList = config.foodList().split(",");
        for (String food : foodList)
        {
            foodArray.add(food.toLowerCase());
        }
        return foodArray;
    }

    public ArrayList<String> getPotionList()
    {
        ArrayList<String> potionArray = new ArrayList<String>(100);
        String[] potionList = config.potionList().split(",");
        for (String potion : potionList)
        {
            potionArray.add(potion.toLowerCase());
        }
        return potionArray;
    }

    public ArrayList<String> getNpcList()
    {
        ArrayList<String> npcArray = new ArrayList<String>(100);
        String[] npcList = config.npcList().split(",");
        for (String npc : npcList)
        {
            npcArray.add(npc.toLowerCase());
        }
        return npcArray;
    }

    public boolean hasFood()
    {
        if (Inventory.contains(x -> getFoodList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasPotions()
    {
        if (Inventory.contains(x -> getPotionList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasBones()
    {
        if (Inventory.contains(x -> x.getName().toLowerCase().contains("bones")))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasLoot()
    {
        if (Inventory.contains(x -> getLootList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasLootInBank()
    {
        if (Bank.contains(x -> getLootList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasFoodInBank()
    {
        if (Bank.contains(x -> getFoodList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasPotionsInBank()
    {
        if (Bank.contains(x -> getPotionList().contains(x.getName().toLowerCase())))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public NPC getInteractingWithNPC()
    {
        NPC npc = null;
        if (client.getLocalPlayer().getInteracting() != null)
        {
            if (client.getLocalPlayer().getInteracting() instanceof NPC)
            {
                npc = (NPC) client.getLocalPlayer().getInteracting();
            }
        }
        return npc;
    }

    public Item getInteractingWithItem()
    {
        Item item = null;
        if (client.getLocalPlayer().getInteracting() != null)
        {
            if (client.getLocalPlayer().getInteracting() instanceof Item)
            {
                item = (Item) client.getLocalPlayer().getInteracting();
            }
        }
        return item;
    }

    public GameObject isInteractingWithGameObject()
    {
        GameObject gameObject = null;
        if (client.getLocalPlayer().getInteracting() != null)
        {
            if (client.getLocalPlayer().getInteracting() instanceof GameObject)
            {
                gameObject = (GameObject) client.getLocalPlayer().getInteracting();
            }
        }
        return gameObject;
    }

    public boolean eatFood()
    {
        if (hasFood())
        {
            Item food = Inventory.getFirst(x -> getFoodList().contains(x.getName().toLowerCase()));
            if (food != null)
            {
                log.info("atv2: Eating food");
                Static.getClient().invokeMenuAction("Eat", "<col=ff9040>" + food.getName(), 2, 57, food.getSlot(), 9764864);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean drinkPotion()
    {
        if (hasPotions())
        {
            Item potion = Inventory.getFirst(x -> getPotionList().contains(x.getName().toLowerCase()));
            if (potion != null)
            {
                Static.getClient().invokeMenuAction("Drink", "<col=ff9040>" + potion.getName(), 2, 57, potion.getSlot(), 9764864);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean buryBones()
    {
        if (hasLoot())
        {
            Item bones = Inventory.getFirst(x -> x.getName().toLowerCase().contains("bones"));
            if (bones != null)
            {
                Static.getClient().invokeMenuAction("Bury", "<col=ff9040>" + bones.getName(), 2, 57, bones.getSlot(), 9764864);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean checkHealth()
    {
        int currentPercentage = (int) Math.floor((double) client.getLocalPlayer().getHealthRatio()  / (double) client.getLocalPlayer().getHealthScale() * 100);
        if (currentPercentage < config.healthThreshold())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean withdrawFood()
    {
        if (hasFoodInBank() && Bank.isOpen())
        {
            int withdrawAmount = Math.min(config.foodWithdrawAmount(), Inventory.getFreeSlots());
            Item food = Bank.getFirst(x -> getFoodList().contains(x.getName().toLowerCase()) && x.getQuantity() >= withdrawAmount && Inventory.getFreeSlots() >= 1);
            if (food != null)
            {
                log.info("atv2: INTERACT: withdrawing food: " + food.getName() + " = " + withdrawAmount);
                Bank.withdraw(food.getName(), withdrawAmount, Bank.WithdrawMode.ITEM);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean withdrawPotions()
    {
        if (hasPotionsInBank() && Bank.isOpen())
        {
            int withdrawAmount = Math.min(config.potionWithdrawAmount(), Inventory.getFreeSlots());
            Item potion = Bank.getFirst(x -> getPotionList().contains(x.getName().toLowerCase()) && x.getQuantity() >= withdrawAmount && Inventory.getFreeSlots() >= 1);
            if (potion != null)
            {
                log.info("atv2: INTERACT: withdrawing potion: " + potion.getName() + " = " + withdrawAmount);
                Bank.withdraw(potion.getName(), withdrawAmount, Bank.WithdrawMode.ITEM);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean depositLoot()
    {
        if (hasLoot() && Bank.isOpen())
        {
            List<Item> loot = Inventory.getAll(x -> getLootList().contains(x.getName().toLowerCase()) && !getDepositBlacklist().contains(x.getName().toLowerCase()));
            if (loot != null)
            {
                for (Item item : loot)
                {
                    log.info("atv2: INTERACT: depositing loot: " + item.getName() + " = " + item.getQuantity());
                    Bank.depositAll(item.getName());
                }
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }


    public TileObject findNearestBankBooth()
    {
        TileObject bankBooth = TileObjects.getNearest(x -> x.getName().toLowerCase().contains("booth") && x.hasAction("Bank"));

        return bankBooth;
    }

    public NPC findNearestBank()
    {
        NPC bank = NPCs.getNearest(x -> x.hasAction("Bank"));
        return bank;
    }

    public BankLocation findNearestBankLocation()
    {
        BankLocation bankLocation = BankLocation.getNearest();
        return bankLocation;
    }

    public boolean isInBank()
    {
        return BankLocation.getNearest().getArea().contains(client.getLocalPlayer());
    }

    public boolean interactWithBank()
    {
        NPC bank = findNearestBank();
        TileObject bankBooth = findNearestBankBooth();
        if (bankBooth != null)
        {
            log.info("atv2: INTERACT: trying to interact with bank booth");
            bankBooth.interact("Bank");
            return true;
        }
        else if (bank != null)
        {
            log.info("atv2: INTERACT: trying to interact with banker");
            bank.interact("Bank");
            return true;
        }
        else
        {
            log.info("atv2: INTERACT: failed to interact with banker");
            return false;
        }
    }

    public TileItem findItemDrop()
    {
        TileItem item = TileItems.getNearest(x ->
            getLootList().contains(x.getName().toLowerCase())
                && config.autoLoot() && Reachable.isInteractable(x.getTile())
                && (!Inventory.isFull() || Inventory.contains(y -> y.getName() == x.getName() && x.isStackable()))
                && x.getTile().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) < config.lootDistance()
                && x.getQuantity() > 0);
        return item;
    }

    public boolean lootItemDrop()
    {
        TileItem item = findItemDrop();
        if (item != null)
        {
            log.info("atv2: INTERACT: trying to loot item: " + item.getName() + " = " + item.getQuantity());
            item.pickup();
            log.info("atv2: INTERACT: looted item: " + item.getName() + " = " + item.getQuantity());
            return true;
        }
        log.info("atv2: INTERACT: no item to loot");
        return false;
    }

    public boolean attackNPC()
    {
        NPC attacker = getUnderAttack();
        NPC npc = NPCs.getNearest(x ->
                x.hasAction("Attack") && config.attackNPCs() && isReachable(x)
                        && x.getCombatLevel() > 0 && x.getCombatLevel() < 100
                        && x.distanceTo(client.getLocalPlayer().getWorldLocation()) < config.attackDistance()
                        && ( client.getLocalPlayer().getInteracting() == x
                        || x.getInteracting() == client.getLocalPlayer()
                        || x.getInteracting() == null)
                        && (getNpcList().contains(x.getName().toLowerCase())
                        || config.npcList().equalsIgnoreCase("All")));
        
        if (attacker != null)
        {
            log.info("atv2: INTERACT: trying to attack prior attacker " + attacker.getName());
            attacker.interact("Attack");
            log.info("atv2: INTERACT: attacking prior attacker" + attacker.getName());
            return true;
        }
        else if (npc != null)
        {
            log.info("atv2: INTERACT: trying to attack " + npc.getName());
            npc.interact("Attack");
            log.info("atv2: INTERACT: attacking " + npc.getName());
            return true;
        }
        else
        {
            log.info("atv2: INTERACT: no npc found");
            return false;
        }
    }

    public boolean walkToBank()
    {
        if (config.bankWhenEmpty() && (getUnderAttack() != null || client.getLocalPlayer().isIdle()) && !hasFood())
        {
            log.info("atv2: INTERACT: walking to bank");
            Movement.walkTo(BankLocation.getNearest().getArea().getCenter());
            return true;
        }
        else
        {
            return false;
        }
    }

    public void saveLocation()
    {
        log.info("atv2: INTERACT: saving location");
        savedLocation = client.getLocalPlayer().getWorldLocation();
    }

    public boolean isAtSavedLocation()
    {
        if (savedLocation != null)
        {
            return savedLocation.distanceTo(client.getLocalPlayer().getWorldLocation()) < 5;
        }
        else
        {
            return false;
        }
    }

    public boolean walkToSavedLocation()
    {
        if (config.bankWhenEmpty() && ( getUnderAttack() != null || client.getLocalPlayer().isIdle()))
        {
            log.info("atv2: INTERACT: walking to saved location");
            Movement.walkTo(savedLocation);
            return true;
        }
        else
        {
            return false;
        }
    }

    // check if any nearby NPC is attacking the player
    public NPC getUnderAttack()
    {
        NPC npc = NPCs.getNearest(x -> x.getInteracting() == client.getLocalPlayer());
        return npc;
    }

    // function gets if there is a widget open and visible with uid 153 or 233
    public boolean isWidgetOpen()
    {
        Widget widget = Static.getClient().getWidget(233, 0);
        if (widget != null && widget.isVisible())
        {
            Static.getClient().invokeMenuAction("Continue", "", 0, 30, -1, 15269891);
            return true;
        }
        widget = Static.getClient().getWidget(193, 0);
        if (widget != null && widget.isVisible())
        {
            Static.getClient().invokeMenuAction("Continue", "", 0, 57, 2, 12648448);
            return true;
        }
        return false;
    }

    // function that returns if a npc is reachable if config.attackReachable is enabled or not reachable if disabled
    public boolean isReachable(NPC npc)
    {
        if (config.attackReachable())
        {
            return Reachable.isInteractable(npc);
        }
        else
        {
            return !Reachable.isInteractable(npc);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!currentState.hasEntered())
        {
            currentState.setHasEntered(true);
            switch (currentState)
            {
                case IDLING:
                    log.info("atv2: IDLING");
                    break;
                case NAVIGATING_TO_ITEM:
                    log.info("atv2: NAVIGATING_TO_ITEM");
                    lootItemDrop();
                    break;
                case NAVIGATING_TO_NPC:
                    log.info("atv2: NAVIGATING_TO_NPC");
                    attackNPC();
                    break;
                case ATTACKING:
                    log.info("atv2: ATTACKING");
                    // this is monitored on tick to see when the player is no longer attacking
                    break;
                case LOOTING:
                    log.info("atv2: LOOTING");
                    // this is monitored on tick to see when the player is no longer looting
                    break;
                case EATING:
                    log.info("atv2: EATING");
                    eatFood();
                    break;
                case USING_POTIONS:
                    log.info("atv2: USING_POTIONS");
                    drinkPotion();
                    break;
                case BANKING:
                    log.info("atv2: BANKING");
                    depositLoot();
                    withdrawFood();
                    withdrawPotions();
                    if (Bank.isOpen())
                    {
                        Bank.close();
                    }
                    break;
                case OPENING_BANK:
                    log.info("atv2: OPENING_BANK");
                    interactWithBank();
                    break;
                case NAVIGATING_TO_BANK:
                    log.info("atv2: NAVIGATING_TO_BANK");
                    saveLocation();
                    break;
                case NAVIGATING_BACK_FROM_BANK:
                    log.info("atv2: NAVIGATING_BACK_FROM_BANK");
                    break;
                case BURYING:
                    log.info("atv2: BURYING");
                    buryBones();
                    break;
                case LOGGED_OUT:
                    break;
                case LOGGING_IN:
                    break;
            }
        }
        else
        {
            if (currentState.isComplete())
            {
                currentState.setHasEntered(false);
                currentState.setIsComplete(false);
                previousState = currentState;
                switch (currentState)
                {
                    case IDLING:
                        if (config.bankWhenEmpty() && !hasFood())
                        {
                            currentState = State.NAVIGATING_TO_BANK;
                        }
                        else if (config.autoEat() && hasFood() && checkHealth())
                        {
                            currentState = State.EATING;
                        }
                        else if  (config.buryBones() && hasBones())
                        {
                            currentState = State.BURYING;
                        }
                        else if (config.attackNPCs() && getUnderAttack() != null)
                        {
                            currentState = State.NAVIGATING_TO_NPC;
                        }
                        else if (config.autoLoot() && Inventory.getFreeSlots() > 0 && findItemDrop() != null)
                        {
                            currentState = State.NAVIGATING_TO_ITEM;
                        }
                        else if (config.attackNPCs() && NPCs.getNearest(x -> x.hasAction("Attack") && config.attackNPCs() && isReachable(x) && x.getCombatLevel() > 0 && x.getCombatLevel() < 100 && x.distanceTo(client.getLocalPlayer().getWorldLocation()) < config.attackDistance() && ( client.getLocalPlayer().getInteracting() == x || getNpcList().contains(x.getName().toLowerCase()) || config.npcList().equalsIgnoreCase("All"))) != null)
                        {
                            currentState = State.NAVIGATING_TO_NPC;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case ATTACKING:
                        if (config.autoEat() && hasFood() && checkHealth())
                        {
                            currentState = State.EATING;
                        }
                        if (config.bankWhenEmpty() && !hasFood())
                        {
                            currentState = State.NAVIGATING_TO_BANK;
                        }
                        else if (config.autoLoot() && Inventory.getFreeSlots() > 0  && findItemDrop() != null)
                        {
                            currentState = State.NAVIGATING_TO_ITEM;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case LOOTING:
                        if (config.bankWhenEmpty() && !hasFood())
                        {
                            currentState = State.NAVIGATING_TO_BANK;
                        }
                        else if (config.autoLoot() && Inventory.getFreeSlots() > 0 && findItemDrop() != null)
                        {
                            currentState = State.NAVIGATING_TO_ITEM;
                        }
                        else if (config.buryBones() && hasBones())
                        {
                            currentState = State.BURYING;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case NAVIGATING_TO_ITEM:
                        if (config.autoLoot())
                        {  
                            log.info("atv2: NAVIGATING_TO_ITEM: transitioning to LOOTING");
                            currentState = State.LOOTING;
                        }
                        else
                        {
                            log.info("atv2: NAVIGATING_TO_ITEM: transitioning to IDLING");
                            currentState = State.IDLING;
                        }
                        break;
                    case EATING:
                        if (config.bankWhenEmpty() && !hasFood())
                        {
                            currentState = State.NAVIGATING_TO_BANK;
                        }
                        else if (getUnderAttack() != null)
                        {
                            currentState = State.ATTACKING;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case USING_POTIONS:
                        currentState = State.IDLING;
                        break;
                    case BANKING:
                        if (config.attackNPCs() && (hasFood() || hasPotions()))
                        {
                            currentState = State.NAVIGATING_BACK_FROM_BANK;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case OPENING_BANK:
                        if (config.bankWhenEmpty())
                        {
                            currentState = State.BANKING;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case NAVIGATING_TO_BANK:
                        if (config.bankWhenEmpty())
                        {
                            currentState = State.OPENING_BANK;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case NAVIGATING_TO_NPC:
                        if (config.attackNPCs() && client.getLocalPlayer().getInteracting() instanceof NPC)
                        {
                            currentState = State.ATTACKING;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case NAVIGATING_BACK_FROM_BANK:
                        if (config.attackNPCs())
                        {
                            currentState = State.NAVIGATING_TO_NPC;
                        }
                        else
                        {
                            currentState = State.IDLING;
                        }
                        break;
                    case BURYING:
                        currentState = State.IDLING;
                        break;
                    case LOGGED_OUT:
                        break;
                    case LOGGING_IN:
                        break;
                }
            }
            else
            {
                if (config.disableNotifications())
                {
                    isWidgetOpen();
                }
                switch (currentState)
                {
                    case IDLING:
                        currentState.setIsComplete(true);
                        break;
                    case ATTACKING:
                        if (!(client.getLocalPlayer().getInteracting() instanceof NPC))
                        {
                            currentState.setIsComplete(true);
                        }
                        else if (config.autoEat() && hasFood() && checkHealth())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case LOOTING:
                        if (!(client.getLocalPlayer().getInteracting() instanceof TileItem) || client.getLocalPlayer().isIdle())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case EATING:
                        if (client.getLocalPlayer().getAnimation() != 829)
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case USING_POTIONS:
                        break;
                    case NAVIGATING_TO_NPC:
                        if ((client.getLocalPlayer().getInteracting() instanceof NPC) || client.getLocalPlayer().isIdle())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case BANKING:
                        if (!Bank.isOpen())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case OPENING_BANK:
                        if (Bank.isOpen())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case NAVIGATING_TO_BANK:
                        if (isInBank())
                        {
                            currentState.setIsComplete(true);
                        }
                        else
                        {
                            walkToBank();
                        }
                        break;
                    case NAVIGATING_BACK_FROM_BANK:
                        if (isAtSavedLocation())
                        {
                            currentState.setIsComplete(true);
                        }
                        else
                        {
                            walkToSavedLocation();
                        }
                        break;
                    case BURYING:
                        if (client.getLocalPlayer().getAnimation() != 827)
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                    case LOGGED_OUT:
                        break;
                    case LOGGING_IN:
                        break;
                    case NAVIGATING_TO_ITEM:
                        if (client.getLocalPlayer().getInteracting() instanceof TileItem || client.getLocalPlayer().isIdle())
                        {
                            currentState.setIsComplete(true);
                        }
                        break;
                }
            }
        }
    }
}
