package cn.autoforged.eternal_food_mod_1789477527.brew;

/**
 * 鸭子接口：由 {@code BrewingStandBlockEntityMixin} 注入到原版酿造台 BlockEntity，
 * 让外部代码可以读取 / 写入原版包级私有的 {@code fuel} 字段。
 *
 * <p>故意放在 mixin 包之外——Mixin 禁止直接引用 mixin 包内的非 mixin 类。
 */
public interface EternalFuelBrewingAccess {

    int eternal_food$getFuel();

    void eternal_food$setFuel(int value);
}
