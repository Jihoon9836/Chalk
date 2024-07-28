package io.github.mortuusars.chalk.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ConsecutiveSleepingTrigger extends SimpleCriterionTrigger<ConsecutiveSleepingTrigger.TriggerInstance> {
    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, PlayerSleepInfo sleepInfo) {
        this.trigger(player, triggerInstance -> triggerInstance.matches(player, sleepInfo));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<LocationPredicate> location,
                                  MinMaxBounds.Ints count, DistancePredicate distance) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                                LocationPredicate.CODEC.optionalFieldOf("location").forGetter(TriggerInstance::location),
                                MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(TriggerInstance::count),
                                DistancePredicate.CODEC.fieldOf("distance").forGetter(TriggerInstance::distance))
                        .apply(instance, TriggerInstance::new));

        public boolean matches(ServerPlayer player, PlayerSleepInfo sleepInfo) {
            boolean locationMatches = this.location.map(p ->
                    p.matches(player.serverLevel(), player.getX(), player.getY(), player.getZ())).orElse(true);
            if (!locationMatches) {
                return false;
            }

            List<BlockPos> sleepPositions = sleepInfo.sleepPositions();

            if (sleepPositions.isEmpty())
                return false;
            else if (sleepPositions.size() == 1)
                return count.matches(1);

            BlockPos lastSleepPos = sleepPositions.getLast();

            int matchedDistanceCount = 1;

            for (int i = sleepPositions.size() - 2; i >= 0; i--) {
                BlockPos pos = sleepPositions.get(i);

                if (distance.matches(lastSleepPos.getX(), lastSleepPos.getY(), lastSleepPos.getZ(),
                        pos.getX(), pos.getY(), pos.getZ()))
                    matchedDistanceCount++;
                else
                    break;
            }

            return count.matches(matchedDistanceCount);
        }
    }
}
