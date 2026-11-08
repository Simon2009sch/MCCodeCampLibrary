package me.simoncrafter.mCCodeCampLibraryTestPlugin.testing;

import me.simoncrafter.mCCodeCampLibrary.projectile.effect.ProjectileEffects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import java.util.Objects;

/** No player, command block or remote-console sender can initiate live effects or restoration. */
public final class ProjectileEffectsCommand implements CommandExecutor {
    private final ProjectileEffects effects;
    public ProjectileEffectsCommand(ProjectileEffects effects) { this.effects = Objects.requireNonNull(effects); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("Projectile effects commands are console-only.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("Usage: projectileeffects status|live-meteor|live-ray|restore|disarm");
            return true;
        }
        switch (args[0]) {
            case "status" -> sender.sendMessage(effects.status());
            case "live-meteor" -> sender.sendMessage("Meteor started: " + effects.liveMeteor());
            case "live-ray" -> sender.sendMessage("Ray started: " + effects.liveRay());
            case "restore" -> sender.sendMessage("Restore started: " + effects.restore(null));
            case "disarm" -> { effects.disarmLiveTest(); sender.sendMessage(effects.status()); }
            default -> sender.sendMessage("Unknown projectileeffects action.");
        }
        return true;
    }
}
