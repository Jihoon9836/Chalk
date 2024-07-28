package io.github.mortuusars.chalk.item;

import io.github.mortuusars.chalk.Chalk;
import io.github.mortuusars.chalk.config.Config;
import io.github.mortuusars.chalk.core.IChalkDrawingTool;
import io.github.mortuusars.chalk.core.Mark;
import io.github.mortuusars.chalk.core.MarkSymbol;
import io.github.mortuusars.chalk.data.ChalkColors;
import io.github.mortuusars.chalk.utils.MarkDrawingContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChalkItem extends Item implements IChalkDrawingTool {
    private final DyeColor color;

    public ChalkItem(DyeColor dyeColor, Properties properties) {
        super(properties);
        color = dyeColor;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        final InteractionHand hand = context.getHand();
        final ItemStack itemStack = context.getItemInHand();
        final Player player = context.getPlayer();

        if (player == null || !(itemStack.getItem() instanceof ChalkItem))
            return InteractionResult.FAIL;

        // When holding chalks in both hands - skip drawing from offhand
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() instanceof ChalkItem)
            return InteractionResult.FAIL;

        MarkDrawingContext drawingContext = createDrawingContext(player, context.getClickedPos(), context.getClickLocation(), context.getClickedFace(), hand);

        if (!drawingContext.canDraw())
            return InteractionResult.FAIL;

        if (player.isSecondaryUseActive()) {
            drawingContext.openSymbolSelectionScreen();
            return InteractionResult.CONSUME;
        }

        if (drawMark(drawingContext, drawingContext.createRegularMark(ChalkColors.fromDyeColor(color), false)))
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        else if (drawingContext.hasExistingMark())
            return InteractionResult.PASS;

        return InteractionResult.FAIL;
    }

    @Override
    public void onMarkDrawn(Player player, InteractionHand hand, BlockPos markBlockPos, BlockState markBlockState) {
        if (player.isCreative())
            return;

        ItemStack result = damageAndDestroy(player.getItemInHand(hand), player);
        if (result.isEmpty())
            player.setItemInHand(hand, ItemStack.EMPTY);
    }

    @Override
    public Mark getMark(ItemStack itemInHand, MarkDrawingContext drawingContext, MarkSymbol symbol) {
        return drawingContext.createMark(ChalkColors.fromDyeColor(getColor()), symbol, isGlowing(itemInHand));
    }

    @Override
    public int getMarkColorValue(ItemStack stack) {
        return ChalkColors.fromDyeColor(getColor());
    }

    @Override
    public Optional<DyeColor> getMarkColor(ItemStack stack) {
        return Optional.of(getColor());
    }

    @Override
    public boolean isGlowing(ItemStack stack) {
        return false;
    }

    public static ItemStack damageAndDestroy(ItemStack chalkStack, Player player) {
        if (!chalkStack.isDamageableItem())
            return chalkStack;

        chalkStack.setDamageValue(chalkStack.getDamageValue() + 1);
        if (chalkStack.getDamageValue() >= chalkStack.getMaxDamage()) {
            Vec3 playerPos = player.position();
            player.level().playSound(player, playerPos.x, playerPos.y, playerPos.z, Chalk.SoundEvents.CHALK_BROKEN.get(),
                    SoundSource.PLAYERS, 0.9f, 0.9f + player.level().random.nextFloat() * 0.2f);
            return ItemStack.EMPTY;
        }

        return chalkStack;
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        try {
            return Config.Common.CHALK_DURABILITY.get();
        }
        catch (IllegalStateException e) {
            return 64; // Fallback for the case where config is not loaded yet. In datagen for example.
        }
    }
    public DyeColor getColor() {
        return this.color;
    }
    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }
    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return false;
    }
}
