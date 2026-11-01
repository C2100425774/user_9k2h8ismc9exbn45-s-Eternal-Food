package cn.autoforged.eternal_food_mod_1789477527.event;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalChorusFruitPlusItem;
import cn.autoforged.eternal_food_mod_1789477527.network.ModNetwork;
import cn.autoforged.eternal_food_mod_1789477527.util.SafeTeleport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒紫颂果 plus 的被动：<b>坠落虚空受伤</b>时自动救命。
 *
 * <p>触发条件（三个都要满足）：
 * <ol>
 *   <li>伤害来源是 {@code minecraft:out_of_world}（原版"掉出世界"伤害）；</li>
 *   <li>玩家确实在<b>世界底部之下</b>（{@code y < minBuildHeight - 32}）——
 *       这样带虚空伤害的 Boss / 结界、法阵打人时不会触发，避免逃课；</li>
 *   <li>背包（含主手、副手）里至少有一个永恒紫颂果 plus。</li>
 * </ol>
 *
 * <p>处理顺序：
 * <ol>
 *   <li>先在附近找一个安全位置传过去（聊天栏提示"传送到安全位置"）；</li>
 *   <li>附近找不到（例如整片都是虚空 / 岩浆海）→ 退化成<b>触发主动效果</b>：
 *       传送到该变体对应的复活点 / 世界出生点 / 磁石顶上，并播放食用时的不死图腾动画，
 *       聊天栏提示"没有找到安全位置，只能触发主动效果"；</li>
 *   <li>主动效果也失败（例如绑定版没绑定过磁石）→ 最后兜底传送到世界出生点。</li>
 * </ol>
 *
 * <p>任何情况下物品都不消耗。
 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoidRescueHandler {

    /** 世界底部再往下这么多格才算"掉进虚空"，留足余量排除地面上的虚空伤害。 */
    private static final int VOID_MARGIN = 32;

    private VoidRescueHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!isFallingIntoVoid(player, event.getSource())) {
            return;
        }
        ItemStack fruit = findFruit(player);
        if (fruit.isEmpty()) {
            return;
        }

        // 先取消伤害，再传送：即使传送过程中再判定一次也不会掉血
        event.setCanceled(true);

        if (SafeTeleport.rescueNearby(player)) {
            player.displayClientMessage(Component
                    .translatable("message.eternal_food_mod_1789477527.void_rescue", fruit.getHoverName())
                    .withStyle(ChatFormatting.AQUA), false);
            return;
        }

        // 附近找不到安全位置 → 触发主动效果（传送到复活点 / 世界出生点 / 磁石顶上）
        boolean activeWorked = fruit.getItem() instanceof EternalChorusFruitPlusItem item
                && item.teleportByMode(player, fruit);
        if (activeWorked) {
            // 和食用时一样播一次不死图腾动画，避免玩家以为没生效
            ModNetwork.sendTotemAnimation(player, fruit.copy());
            player.displayClientMessage(Component
                    .translatable("message.eternal_food_mod_1789477527.void_rescue_active",
                            fruit.getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return;
        }

        // 主动效果也失败（绑定版没有绑定过磁石）→ 兜底送出生点，至少不让人继续掉
        SafeTeleport.fallbackToSpawn(player);
        player.displayClientMessage(Component
                .translatable("message.eternal_food_mod_1789477527.void_rescue", fruit.getHoverName())
                .withStyle(ChatFormatting.AQUA), false);
    }

    private static boolean isFallingIntoVoid(ServerPlayer player, DamageSource source) {
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        double threshold = player.level().getMinBuildHeight() - VOID_MARGIN;
        return player.getY() < threshold;
    }

    /** 主手 → 副手 → 背包 → 快捷栏，找一个永恒紫颂果 plus。 */
    private static ItemStack findFruit(ServerPlayer player) {
        if (isFruit(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (isFruit(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isFruit(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isFruit(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EternalChorusFruitPlusItem;
    }
}
