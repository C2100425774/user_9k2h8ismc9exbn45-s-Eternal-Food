package cn.autoforged.eternal_food_mod_1789477527.entity;

import cn.autoforged.eternal_food_mod_1789477527.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 永恒投掷型药水的投掷物：复用原版 {@link ThrownPotion} 的落点效果（喷溅范围 / 瞬间效果 /
 * 滞留云），仅额外支持"永恒滞留型药水"这一自定义物品。
 *
 * <p>原版 {@code ThrownPotion#isLingering()} 是 private，且直接判断物品是否为
 * {@code Items.LINGERING_POTION}。因此在命中瞬间把手中的永恒滞留型药水临时替换成
 * 携带同样 NBT 的原版滞留型药水，让原版逻辑生成滞留云；投掷物随即 discard，替换不会残留。
 */
public class ThrownEternalPotion extends ThrownPotion {

    public ThrownEternalPotion(EntityType<? extends ThrownPotion> type, Level level) {
        super(type, level);
    }

    public ThrownEternalPotion(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_ETERNAL_POTION.get(), level);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.setOwner(shooter);
    }

    /** 供发射器使用：在指定坐标生成，随后由发射器调用 {@code shoot} 赋予飞行速度。 */
    public ThrownEternalPotion(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_ETERNAL_POTION.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void onHit(HitResult result) {
        ItemStack item = this.getItem();
        if (item.is(ModItems.ETERNAL_LINGERING_POTION.get())) {
            ItemStack vanilla = new ItemStack(Items.LINGERING_POTION);
            if (item.getTag() != null) {
                vanilla.setTag(item.getTag().copy());
            }
            this.setItem(vanilla);
        }
        super.onHit(result);
    }
}
