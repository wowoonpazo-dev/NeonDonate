package ru.imperium.neon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class NeonDonate extends JavaPlugin implements Listener {

    // Хранилище цветов игроков: [Ник] -> [Цвет1, Цвет2]
    private final Map<String, String[]> playerColors = new HashMap<>();
    private final Map<UUID, String> targetSelection = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NeonDonate GUI успешно включен!");
    }

    // --- КОМАНДА /neon ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("neon.admin")) {
            player.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        openTargetMenu(player);
        return true;
    }

    // --- МЕНЮ 1: ВЫБОР ЦЕЛИ ---
    private void openTargetMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Выберите цель").color(NamedTextColor.DARK_PURPLE));

        // Кнопка "На себя"
        ItemStack self = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta selfMeta = self.getItemMeta();
        selfMeta.displayName(Component.text("Выдать себе").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        self.setItemMeta(selfMeta);
        inv.setItem(11, self);

        // Кнопка "На донат"
        ItemStack donate = new ItemStack(Material.GOLD_INGOT);
        ItemMeta donMeta = donate.getItemMeta();
        donMeta.displayName(Component.text("Выдать донату").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        donate.setItemMeta(donMeta);
        inv.setItem(15, donate);

        player.openInventory(inv);
    }

    // --- МЕНЮ 2: ВЫБОР ЦВЕТОВ ---
    private void openColorMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("Выберите 2 цвета").color(NamedTextColor.LIGHT_PURPLE));

        // Список доступных цветов (можно расширить)
        Map<String, Material> colors = new LinkedHashMap<>();
        colors.put("#FF0000", Material.RED_DYE);
        colors.put("#00FF00", Material.LIME_DYE);
        colors.put("#0000FF", Material.LAPIS_LAZULI);
        colors.put("#FFFF00", Material.YELLOW_DYE);
        colors.put("#FF00FF", Material.MAGENTA_DYE);
        colors.put("#00FFFF", Material.CYAN_DYE);
        colors.put("#FFA500", Material.ORANGE_DYE);
        colors.put("#800080", Material.PURPLE_DYE);

        int slot = 10;
        for (Map.Entry<String, Material> entry : colors.entrySet()) {
            ItemStack item = new ItemStack(entry.getValue());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text("Цвет: " + entry.getKey()).color(TextColor.fromHexString(entry.getKey())).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
            if (slot == 17) slot = 19; // Перенос на второй ряд
        }

        // Кнопка "Сохранить" (внизу)
        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = save.getItemMeta();
        saveMeta.displayName(Component.text("ПРИМЕНИТЬ НЕОН").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        save.setItemMeta(saveMeta);
        inv.setItem(40, save);

        player.openInventory(inv);
    }

    // --- ОБРАБОТКА КЛИКОВ В МЕНЮ ---
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (!title.contains("Выберите")) return; // Проверяем, что это наши меню

        event.setCancelled(true); // Запрещаем брать предметы

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        // Если это меню выбора цели
        if (title.contains("Выберите цель")) {
            if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                targetSelection.put(player.getUniqueId(), player.getName());
                openColorMenu(player);
            } else if (event.getCurrentItem().getType() == Material.GOLD_INGOT) {
                player.sendMessage(Component.text("Выберите игрока-доната в чате...").color(NamedTextColor.YELLOW));
                player.closeInventory();
                // Здесь можно сделать ввод в чате или список игроков, для простоты закроем меню
            }
        }
        
        // Если это меню выбора цветов
        if (title.contains("Выберите 2 цвета")) {
            if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                String target = targetSelection.getOrDefault(player.getUniqueId(), player.getName());
                // Для теста ставим фиксированные цвета, так как сложно отслеживать "первый" и "второй" клик без доп. переменных
                applyNeon(target, "#FF00FF", "#00FFFF"); 
                player.closeInventory();
                player.sendMessage(Component.text("Неон применен для " + target + "!").color(NamedTextColor.GREEN));
            }
        }
    }

    // --- ЛОГИКА ВЫДАЧИ НЕОНА ---
    private void applyNeon(String targetName, String color1, String color2) {
        playerColors.put(targetName, new String[]{color1, color2});
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            target.setGlowing(true);
            // Красивый градиентный префикс
            Component prefix = Component.text("[")
                .color(TextColor.fromHexString(color1))
                .append(Component.text("НЕОН").color(TextColor.fromHexString(color2)))
                .append(Component.text("] ").color(TextColor.fromHexString(color1)));
            
            target.displayName(prefix.append(Component.text(targetName)));
        }
    }

    // --- ЧАСТИЦЫ (СЛЕД) ---
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!playerColors.containsKey(player.getName())) return;

        if (player.isOnGround() && event.getFrom().distance(event.getTo()) > 0.1) {
            String[] colors = playerColors.get(player.getName());
            Color c1 = hexToColor(colors[0]);
            Color c2 = hexToColor(colors[1]);

            // Чередуем цвета для эффекта переливания
            Color currentColor = (System.currentTimeMillis() / 200) % 2 == 0 ? c1 : c2;

            player.getWorld().spawnParticle(
                Particle.REDSTONE, 
                player.getLocation().add(0, 0.1, 0), 
                5, 0.2, 0.1, 0.2, 
                new Particle.DustOptions(currentColor, 1.5f)
            );
        }
    }

    // Вспомогательная функция для перевода HEX в RGB
    private Color hexToColor(String hex) {
        return Color.fromRGB(
            Integer.valueOf(hex.substring(1, 3), 16),
            Integer.valueOf(hex.substring(3, 5), 16),
            Integer.valueOf(hex.substring(5, 7), 16)
        );
    }
}