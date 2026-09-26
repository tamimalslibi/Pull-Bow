package com.pullbow.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PullBowPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, UUID> arrowShooters = new HashMap<>();
    private static final long COOLDOWN_TIME = 15000; // 15 seconds in milliseconds
    private static final String PULL_BOW_NAME = ChatColor.RED + "" + ChatColor.BOLD + "PULL BOW";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Pull Bow Plugin has been enabled!");

        // Register command to get the Pull Bow - ONLY FOR OPS
        getCommand("getpullbow").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return false;
            }

            Player player = (Player) sender;

            // Check if player is OP
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You must be a server operator to use this command!");
                return false;
            }

            // Give Pull Bow to OP
            player.getInventory().addItem(createPullBow());
            player.sendMessage(ChatColor.GREEN + "You received the Pull Bow!");
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Pull Bow Plugin has been disabled!");
        cooldowns.clear();
        arrowShooters.clear();
    }

    /**
     * Creates the Pull Bow item with all enchantments and lore
     */
    private ItemStack createPullBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            // Set the red bold name
            meta.setDisplayName(PULL_BOW_NAME);

            // Set lore - matching the Dash Bow style EXACTLY
            meta.setLore(Arrays.asList(
                ChatColor.GOLD + "A BOW THAT PULLS HIT PLAYERS.",
                "",
                ChatColor.RED + "⚔ " + ChatColor.BOLD + "Ability: " + ChatColor.WHITE + "Shoot",
                ChatColor.GRAY + "Pull hit players directly towards you.",
                "",
                ChatColor.RED + "⏱ " + ChatColor.BOLD + "Cooldown: " + ChatColor.WHITE + "15 Seconds",
                ChatColor.RED + "♦ " + ChatColor.BOLD + "UNBREAKABLE"
            ));

            bow.setItemMeta(meta);
        }

        return bow;
    }

    /**
     * Checks if an item is the Pull Bow
     */
    private boolean isPullBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(PULL_BOW_NAME);
    }

    /**
     * Handles arrow launch - tracks shooter and applies cooldown
     */
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Arrow arrow = (Arrow) event.getEntity();
        Player shooter = (Player) arrow.getShooter();
        ItemStack itemInHand = shooter.getInventory().getItemInMainHand();

        // Check if shooting with Pull Bow
        if (!isPullBow(itemInHand)) {
            return;
        }

        UUID shooterId = shooter.getUniqueId();

        // Check cooldown
        if (cooldowns.containsKey(shooterId)) {
            long timeLeft = (cooldowns.get(shooterId) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                shooter.sendMessage(ChatColor.RED + "Pull Bow is on cooldown! " + timeLeft + " seconds remaining.");
                event.setCancelled(true);
                return;
            } else {
                cooldowns.remove(shooterId);
            }
        }

        // Apply cooldown immediately when shot is fired
        cooldowns.put(shooterId, System.currentTimeMillis());
        shooter.sendMessage(ChatColor.YELLOW + "Pull Bow used! 15 second cooldown started.");

        // Track which arrow belongs to which shooter
        arrowShooters.put(arrow.getUniqueId(), shooterId);
    }

    /**
     * Handles arrow hit - pulls the target to the shooter
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if damage is from an arrow
        if (!(event.getDamager() instanceof Arrow)) {
            return;
        }

        // Check if the damaged entity is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Arrow arrow = (Arrow) event.getDamager();
        Player target = (Player) event.getEntity();

        // Check if this arrow was shot by a Pull Bow
        UUID arrowId = arrow.getUniqueId();
        if (!arrowShooters.containsKey(arrowId)) {
            return;
        }

        UUID shooterId = arrowShooters.get(arrowId);
        Player shooter = Bukkit.getPlayer(shooterId);

        // Clean up arrow tracking
        arrowShooters.remove(arrowId);

        // Make sure shooter is still online
        if (shooter == null || !shooter.isOnline()) {
            return;
        }

        // Don't pull yourself
        if (target.getUniqueId().equals(shooterId)) {
            return;
        }

        // Start the pull effect
        pullPlayerToShooter(target, shooter);
    }

    /**
     * Smoothly pulls the target player directly in front of the shooter
     */
    private void pullPlayerToShooter(Player target, Player shooter) {
        new BukkitRunnable() {
            int ticksRun = 0;
            final int maxTicks = 40; // Run for up to 2 seconds (40 ticks)

            @Override
            public void run() {
                // Stop if either player is offline or dead
                if (!shooter.isOnline() || !target.isOnline() || !shooter.isValid() || !target.isValid()) {
                    cancel();
                    return;
                }

                // Calculate the position directly in front of the shooter
                Vector shooterDirection = shooter.getLocation().getDirection().normalize();
                Vector targetPosition = shooter.getLocation().toVector()
                    .add(shooterDirection.multiply(2.0)); // 2 blocks in front

                // Get current target location
                Vector currentPosition = target.getLocation().toVector();

                // Calculate distance to final position
                double distance = currentPosition.distance(targetPosition);

                // If target is close enough to the final position, stop pulling
                if (distance < 1.5) {
                    // Teleport to exact position for final placement
                    target.teleport(shooter.getLocation().add(shooterDirection.multiply(2.0)));
                    target.sendMessage(ChatColor.RED + "You were pulled by " + shooter.getName() + "!");
                    shooter.sendMessage(ChatColor.GREEN + "You pulled " + target.getName() + " to you!");
                    cancel();
                    return;
                }

                // Calculate pull direction
                Vector pullDirection = targetPosition.clone().subtract(currentPosition).normalize();

                // Apply strong velocity towards the final position
                double pullStrength = Math.min(distance * 0.4, 2.0); // Scale with distance, cap at 2.0
                target.setVelocity(pullDirection.multiply(pullStrength));

                // Safety: stop after max ticks to prevent infinite loop
                ticksRun++;
                if (ticksRun >= maxTicks) {
                    // Force final position
                    target.teleport(shooter.getLocation().add(shooterDirection.multiply(2.0)));
                    target.sendMessage(ChatColor.RED + "You were pulled by " + shooter.getName() + "!");
                    shooter.sendMessage(ChatColor.GREEN + "You pulled " + target.getName() + " to you!");
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 1L); // Run every tick
    }
}
