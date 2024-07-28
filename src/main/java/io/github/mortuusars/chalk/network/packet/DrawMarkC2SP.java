package io.github.mortuusars.chalk.network.packet;

import io.github.mortuusars.chalk.Chalk;
import io.github.mortuusars.chalk.block.ChalkMarkBlock;
import io.github.mortuusars.chalk.core.IChalkDrawingTool;
import io.github.mortuusars.chalk.utils.MarkDrawHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record DrawMarkC2SP(int color, CompoundTag blockStateNBT, BlockPos markBlockPos, InteractionHand drawingHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DrawMarkC2SP> TYPE = new CustomPacketPayload.Type<>(Chalk.resource("draw_mark"));
    public static final StreamCodec<FriendlyByteBuf, DrawMarkC2SP> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            DrawMarkC2SP::color,
            ByteBufCodecs.COMPOUND_TAG,
            DrawMarkC2SP::blockStateNBT,
            BlockPos.STREAM_CODEC,
            DrawMarkC2SP::markBlockPos,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Enum::ordinal, InteractionHand.values(), ByIdMap.OutOfBoundsStrategy.WRAP), Enum::ordinal),
            DrawMarkC2SP::drawingHand,
            DrawMarkC2SP::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DrawMarkC2SP packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();

            ItemStack itemInHand = player.getItemInHand(packet.drawingHand());
            if (!(itemInHand.getItem() instanceof IChalkDrawingTool)) {
                Chalk.LOGGER.error("{} is not a drawing tool.", itemInHand);
                return true;
            }

            Level level = player.level();
            BlockState existingState = level.getBlockState(packet.markBlockPos());

            if (!(existingState.isAir() || existingState.getBlock() instanceof ChalkMarkBlock)) {
                Chalk.LOGGER.error("Cannot draw at '{}': block is '{}'.", packet.markBlockPos(), existingState);
                return true;
            }

            BlockState blockState = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), packet.blockStateNBT());
            return MarkDrawHelper.draw(player, level, packet.markBlockPos(), blockState, packet.color(), packet.drawingHand());
        });
    }
}