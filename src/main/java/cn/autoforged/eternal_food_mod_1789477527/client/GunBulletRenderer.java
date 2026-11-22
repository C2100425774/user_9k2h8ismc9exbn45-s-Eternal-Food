package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.entity.GunBullet;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 3 级子弹的渲染：直接复用原版箭的模型/贴图。 */
public class GunBulletRenderer extends ArrowRenderer<GunBullet> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft:textures/entity/projectiles/arrow.png");

    public GunBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GunBullet entity) {
        return TEXTURE;
    }
}
