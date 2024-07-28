package io.github.mortuusars.chalk.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.chalk.Chalk;
import io.github.mortuusars.chalk.Config;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Basically a copy of AddTableLootModifier just to allow disabling it through config.
 */
public class ChalkAddTableLootModifier extends AddTableLootModifier {
    public static final MapCodec<ChalkAddTableLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(glm -> glm.conditions),
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(ChalkAddTableLootModifier::table))
            .apply(instance, ChalkAddTableLootModifier::new));

    public ChalkAddTableLootModifier(LootItemCondition[] conditions, ResourceKey<LootTable> table) {
        super(conditions, table);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        if (!Config.Common.GENERATE_IN_CHESTS.get()) {
            return generatedLoot;
        }

        return super.doApply(generatedLoot, context);
    }

    @Override
    public @NotNull MapCodec<? extends IGlobalLootModifier> codec() {
        return Chalk.LootModifiers.ADD_TABLE.get();
    }
}
