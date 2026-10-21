package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.EternalFoodMod;
import cn.autoforged.eternal_food_mod_1789477527.entity.ModEntities;
import cn.autoforged.eternal_food_mod_1789477527.item.EternalSpawnEggItem;
import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** 客户端专属注册（渲染器 / 模型层等），必须与物理服务端隔离。 */
@Mod.EventBusSubscriber(modid = EternalFoodMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_ETERNAL_MILK.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_ETERNAL_POTION.get(), ThrownItemRenderer::new);
    }

    /**
     * 永恒药水的物品染色与原版药水一致：模型 layer0 = 液体覆盖层（potion_overlay），
     * 用对应药水颜色相乘；layer1 = 瓶身纹理（eternal_potion 等），不参与染色（返回 -1）。
     *
     * <p>永恒药箭同理：layer0 = 箭头覆盖层（tipped_arrow_head），用对应药水颜色相乘；
     * layer1 = 箭身纹理（eternal_tipped_arrow_base），不参与染色。
     */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex > 0 ? -1 : PotionUtils.getColor(stack),
                ModItems.ETERNAL_POTION.get(),
                ModItems.ETERNAL_SPLASH_POTION.get(),
                ModItems.ETERNAL_LINGERING_POTION.get(),
                ModItems.ETERNAL_TIPPED_ARROW.get());
        // 永恒刷怪蛋：layer0（tintindex 0）金色描边不染色，layer1/2 用对应原版刷怪蛋的背景 / 高光色。
        event.register(EternalSpawnEggItem::getLayerColor, ModItems.ETERNAL_SPAWN_EGG.get());
    }

    /**
     * 让弩装填「永恒烟花火箭」时使用原版装填普通烟花火箭的模型（crossbow_firework）。
     *
     * <p>原版弩的物品模型用 {@code firework} 谓词（{@code ItemProperties}）在「装填了烟花」与
     * 「装填了箭」之间切换模型；该谓词内部对 {@code Items.FIREWORK_ROCKET} 做身份比较，永恒烟花
     * 火箭是自定义物品，因而会被当成箭显示。这里在客户端重注册同一个 {@code firework} 谓词
     * （{@code ItemProperties.register} 是覆盖语义），把永恒烟花也视为烟花即可，无需改动弩本身的
     * 代码或资源。
     */
    @SubscribeEvent
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(Items.CROSSBOW,
                new ResourceLocation("firework"),
                (stack, level, entity, seed) -> CrossbowItem.isCharged(stack)
                        && (CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET)
                                || CrossbowItem.containsChargedProjectile(stack,
                                        ModItems.ETERNAL_FIREWORK_ROCKET.get()))
                        ? 1.0F : 0.0F));
    }
}
