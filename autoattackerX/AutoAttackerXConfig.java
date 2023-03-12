package net.runelite.client.plugins.autoattackerx;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autoattackerX")
public interface AutoAttackerXConfig extends Config
{
        @ConfigItem(
                keyName = "attackNPCs",
                name = "Attack NPCs",
                description = "Configures whether or not to attack NPCs",
                position = 1
        )
        default boolean attackNPCs()
        {
                return false;
        }

        //setting for attack distance
        @ConfigItem(
                keyName = "attackDistance",
                name = "Attack Distance",
                description = "Configures the distance to attack NPCs",
                position = 2
        )
        default int attackDistance()
        {
                return 1;
        }

        // setting for attack npcs
        @ConfigItem(
                keyName = "npcList",
                name = "NPC List",
                description = "Configures a list of NPCs to attack separated by commas (e.g. Cow,Chicken) or (e.g. All)",
                position = 3
        )
        default String npcList()
        {
                return "";
        }

        @ConfigItem(
                keyName = "autoLoot",
                name = "Auto Loot",
                description = "Configures whether or not to automatically loot",
                position = 4
        )
        default boolean autoLoot()
        {
                return false;
        }

        @ConfigItem(
                keyName = "buryBones",
                name = "Bury Bones",
                description = "configures whether or not to automatically bury bones requires auto loot to be enabled and name of bones in loot list",
                position = 5
        )
        default boolean buryBones() { return false; }

        //setting for auto loot distance
        @ConfigItem(
                keyName = "lootDistance",
                name = "Loot Distance",
                description = "Configures the distance to loot items",
                position = 6
        )
        default int lootDistance()
        {
                return 1;
        }

        @ConfigItem(
                keyName = "lootList",
                name = "Loot List",
                description = "Configures a list of items to automatically loot",
                position = 7
        )
        default String lootList() { return ""; }

        //create auto eat setting
        @ConfigItem(
                keyName = "autoEat",
                name = "Auto Eat",
                description = "Configures whether or not to automatically eat food",
                position = 8
        )
        default boolean autoEat() { return false; }

        //setting for health threshold to eat at
        @ConfigItem(
                keyName = "healthThreshold",
                name = "Health Threshold",
                description = "Configures the health threshold to eat atm (e.g. 1 - 100) auto eat must be enabled",
                position = 9
        )
        default int healthThreshold() { return 50; }

        //setting for auto eat food name
        @ConfigItem(
                keyName = "foodList",
                name = "Food List",
                description = "Configures the names of the food to eat separated by commas (e.g. Lobster,Shark) auto eat must be enabled",
                position = 10
        )
        default String foodList() { return ""; }

        @ConfigItem(
                keyName = "potionList",
                name = "Potion List",
                description = "Configures the names of the potions to consume separated by commas (e.g. Lobster,Shark) auto eat must be enabled",
                position = 11
        )
        default String potionList() { return ""; }

        //toggle that will make the player go to the nearest bank when they run out of food
        @ConfigItem(
                keyName = "bankWhenEmpty",
                name = "No Food Bank",
                description = "Configures whether or not to automatically bank when out of food",
                position = 12
        )
        default boolean bankWhenEmpty() { return false; }

        // setting for bank withdraw amount of food
        @ConfigItem(
                keyName = "foodWithdrawAmount",
                name = "Food Withdraw Amount",
                description = "Configures the amount of food to withdraw from the bank",
                position = 13
        )
        default int foodWithdrawAmount() { return 10; }

        // setting for bank withdraw amount of potions
        @ConfigItem(
                keyName = "potionWithdrawAmount",
                name = "Potion Withdraw Amount",
                description = "Configures the amount of potions to withdraw from the bank",
                position = 14
        )
        default int potionWithdrawAmount() { return 0; }

        // setting for deposit blacklist
        @ConfigItem(
                keyName = "depositBlacklist",
                name = "Deposit Blacklist",
                description = "Configures a list of items to not deposit within loot list separated by commas (e.g. Lobster,Shark)",
                position = 15
        )
        default String depositBlacklist() { return ""; }

        // toggle to disable popup notifications
        @ConfigItem(
                keyName = "disableNotifications",
                name = "Disable Notifications",
                description = "Configures whether or not to disable popup notifications (eg, level up or quest completion)",
                position = 16
        )
        default boolean disableNotifications() { return false; }

        // toggle to attack only reachable npcs
        @ConfigItem(
                keyName = "attackReachable",
                name = "Attack Reachable",
                description = "Configures whether or not to attack only reachable npcs",
                position = 17
        )
        default boolean attackReachable() { return false; }
}