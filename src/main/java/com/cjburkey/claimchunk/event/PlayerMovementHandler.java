package com.cjburkey.claimchunk.event;

import com.cjburkey.claimchunk.ClaimChunk;
import com.cjburkey.claimchunk.Utils;
import com.cjburkey.claimchunk.chunk.AutoClaimHandler;
import com.cjburkey.claimchunk.chunk.ChunkHandler;
import com.cjburkey.claimchunk.player.PlayerHandler;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMovementHandler implements Listener {

    private final ClaimChunk claimChunk;

    public PlayerMovementHandler(ClaimChunk claimChunk) {
        this.claimChunk = claimChunk;
    }

    // TODO: MAKE THIS MORE EFFICIENT

    @SuppressWarnings("unused")
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e != null && !e.isCancelled() && e.getTo() != null) {
            // Get the previous and current chunks
            Chunk prev = e.getFrom().getChunk();
            Chunk to = e.getTo().getChunk();

            // Make sure the player moved into a new chunk
            if (prev.getX() != to.getX() || prev.getZ() != to.getZ()) {
                // If the claim is currently autoclaiming, try to claim this chunk
                if (AutoClaimHandler.inList(e.getPlayer())) {
                    claimChunk.getCommandHandler().mainHandler.claimChunk(e.getPlayer(), to);
                    return;
                }

                ChunkHandler ch = claimChunk.getChunkHandler();

                // Check if the previous chunk was already claimed
                boolean lastClaimed = ch.isClaimed(prev.getWorld(), prev.getX(), prev.getZ());

                // Check if the new chunk is already claimed
                if (ch.isClaimed(to.getWorld(), to.getX(), to.getZ())) {
                    // If the new chunk and the previous chunk were claimed, check if the owners differ
                    if (lastClaimed) {
                        UUID prevOwner = ch.getOwner(prev.getWorld(), prev.getX(), prev.getZ());
                        UUID newOwner = ch.getOwner(to.getWorld(), to.getX(), to.getZ());

                        // Only display the new chunk's owner if they differ from the previous chunk's owner
                        if ((prevOwner == null && newOwner == null) || (prevOwner != null && !prevOwner.equals(newOwner))) {
                            showTitle(e.getPlayer(), to);
                        }
                    } else {
                        // Show the player the chunk's owner
                        showTitle(e.getPlayer(), to);
                    }
                } else {
                    // The player entered an unclaimed chunk from a claimed chunk
                    if (lastClaimed) {
                        UUID lastOwner = ch.getOwner(prev.getWorld(), prev.getX(), prev.getZ());
                        String name = claimChunk.getPlayerHandler().getChunkName(lastOwner);
                        String msg = (e.getPlayer().getUniqueId().equals(lastOwner) ? claimChunk.getMessages().chunkLeaveSelf : claimChunk.getMessages().chunkLeave)
                                .replace("%%PLAYER%%",
                                        ((name == null) ? claimChunk.getMessages().chunkLeaveUnknown : name));
                        Utils.toPlayer(e.getPlayer(), msg);
                    }
                }
            }
        }
    }

    private void showTitle(Player player, Chunk newChunk) {
        // Get the UUID of the new chunk owner
        UUID newOwner = claimChunk.getChunkHandler().getOwner(newChunk.getWorld(), newChunk.getX(),
                newChunk.getZ());

        // Check if this player doesn't own the new chunk
        if (newOwner != null && !player.getUniqueId().equals(newOwner)) {
            // Get the name of the chunks for the owner of this chunk and display it
            PlayerHandler ph = claimChunk.getPlayerHandler();
            String newName = ph.getChunkName(newOwner);
            String text = ((newName == null)
                    ? claimChunk.getMessages().unknownChunkOwner  // Something probably went wrong with the PlayerHandler
                    : claimChunk.getMessages().chunkOwner.replace("%%PLAYER%%", newName));
            showTitleRaw(true, player, text);

            // Send a message to the chunk owner if possible
            if (ph.hasAlerts(newOwner)) {
                Player owner = Bukkit.getPlayer(newOwner);
                if (owner != null) {
                    if (owner.canSee(player)
                            || !claimChunk.chConfig().getBool("chunks", "hideAlertsForVanishedPlayers")) {
                        showTitleRaw(false, owner, claimChunk.getMessages().playerEnterChunk.replace("%%PLAYER%%", player.getDisplayName()));
                    }
                }
            }
        } else {
            // This chunk is owned by this player
            showTitleRaw(true, player, claimChunk.getMessages().chunkSelf);
        }
    }

    private void showTitleRaw(boolean isOwnerDisplay, Player player, String msg) {
        if (claimChunk.chConfig().getBool("chunks", "displayNameOfOwner")
                || !isOwnerDisplay) {
            Utils.toPlayer(player, msg);
        }
    }

}
