package com.mattworzala.canvas.demo;

import com.mattworzala.canvas.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.data.DataImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Position;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MainDemo {

    public static void main(String[] args) {
        // Initialization
        MinecraftServer minecraftServer = MinecraftServer.init();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        // Create the instance
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        // Set the ChunkGenerator
        instanceContainer.setChunkGenerator(new GeneratorDemo());
        // Enable the auto chunk loading (when players come close)
        instanceContainer.enableAutoChunkLoad(true);

        // Add an event callback to specify the spawning instance (and the spawn position)
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addEventCallback(PlayerLoginEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.getInventory().addItemStack(ItemStack.of(Material.STONE, 100));
            player.setRespawnPoint(new Position(0, 42, 0));
        });

        globalEventHandler.addEventCallback(PlayerChatEvent.class, event -> {
            Canvas canvas = CanvasProvider.canvas(event.getPlayer());

            switch (event.getMessage().toLowerCase()) {
                case "wiki:first_fragment":
                    canvas.render(WikiFragments::FirstFragment);
                    break;
                case "wiki:first_listener":
                    canvas.render(WikiFragments::FirstListener);
                    break;
                case "wiki:composition":
                    canvas.render(WikiFragments::Composition);
                    break;
                case "wiki:titled_fragment":
                    canvas.render(WikiFragments::TitledFragment);
                    break;
                case "wiki:composition_with_data":
                    canvas.render(WikiFragments::CompositionWithData);
                    break;
                case "wiki:composition_with_state":
                    canvas.render(WikiFragments::CompositionWithState);
                    break;
                case "basic":
                    canvas.render(BasicTest::BasicItems);
                    break;
                case "flash":
                    canvas.render(FlashingTest::FlashingInv);
                    break;
                case "recipe_mask":
                    canvas.render(RecipeMaskTest::RecipeMaskTest);
                    break;
                case "batch":
                    canvas.render(BasicTest::BatchTest);
                    break;
                case "v2:basic":
                    canvas.render(DataReworkTest::MySmartFragment);
                    break;
                case "v2:composition":
                    canvas.render(DataReworkTest::SmartComposition);
                    break;
                default:
                    event.getPlayer().sendMessage("No inventory named '" + event.getMessage() + "'!");
            }
        });

        // Start the server on port 25565
        minecraftServer.start("localhost", 25565);
    }

    private static class GeneratorDemo implements ChunkGenerator {

        @Override
        public void generateChunkData(@NotNull ChunkBatch batch, int chunkX, int chunkZ) {
            // Set chunk blocks
            for (byte x = 0; x < Chunk.CHUNK_SIZE_X; x++)
                for (byte z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                    for (byte y = 0; y < 40; y++) {
                        batch.setBlock(x, y, z, Block.STONE);
                    }
                }
        }

        @Override
        public void fillBiomes(@NotNull Biome[] biomes, int chunkX, int chunkZ) {
            Arrays.fill(biomes, Biome.PLAINS);
        }

        @Override
        public List<ChunkPopulator> getPopulators() {
            return null;
        }
    }

}
