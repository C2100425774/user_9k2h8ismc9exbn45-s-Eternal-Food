package cn.autoforged.eternal_food_mod_1789477527.client;

import cn.autoforged.eternal_food_mod_1789477527.block.EternalClockBlock;
import cn.autoforged.eternal_food_mod_1789477527.block.entity.EternalClockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 永恒数字钟的方块实体渲染器：用 <b>7 段数码管</b> 在薄板正面画 HH:MM。
 *
 * <p>为什么不用字体：{@code Font} 画出来的是 MC 的等宽字体，不像数码管，而且放大后会发虚。
 * 7 段数码管是纯几何矩形，任何分辨率都是硬边。
 *
 * <p>只画<b>点亮</b>的段：熄灭的段不画，屏幕上就是干净的底色，不会出现一排黑色短条。
 *
 * <p>薄板几何（见 {@link EternalClockBlock}）：厚度 3/16，屏幕画在“离墙那一面”的外侧，
 * 所以四个朝向的偏移量都不一样（north/west 是 13/16 向外，south/east 是 3/16 向外）。
 */
public class EternalClockRenderer implements BlockEntityRenderer<EternalClockBlockEntity> {

    private static final ResourceLocation WHITE =
            new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final RenderType TYPE = RenderType.entityCutoutNoCull(WHITE);
    private static final int GLOW = 0xF000F0;

    /**
     * 白天用原版黄色（{@code §e}，0xFFFF55），夜晚用原版蓝色（{@code §9}，0x5555FF）。
     * 取的就是聊天颜色码对应的 RGB，这样表盘和物品提示里 {@code §e} / {@code §9} 上色的文字颜色一致。
     */
    private static final float[] DAY_COLOR = {1.00F, 1.00F, 0.3333F, 1.0F};
    private static final float[] NIGHT_COLOR = {0.3333F, 0.3333F, 1.00F, 1.0F};

    /** 当前这一帧用的颜色（渲染线程单线程，逐帧在 {@link #render} 里设置）。 */
    private float[] activeColor = DAY_COLOR;

    // 单位空间：4 位数字 + 中间的冒号
    private static final float DIGIT_W = 14.0F;
    private static final float DIGIT_H = 24.0F;
    private static final float GAP = 4.0F;
    private static final float COLON_W = 6.0F;
    private static final float COLON_X = DIGIT_W * 2 + GAP * 2;                  // 36
    private static final float[] DIGIT_X = {
            0.0F,
            DIGIT_W + GAP,                                                        // 18
            COLON_X + COLON_W + GAP,                                              // 46
            COLON_X + COLON_W + GAP + DIGIT_W + GAP,                              // 64
    };
    private static final float CONTENT_W = DIGIT_X[3] + DIGIT_W;                  // 78
    /** 文字占方块面的宽度比例：正面贴图只留 1px 金框，屏幕约 14/16，所以 0.80 能安全落在屏内。 */
    private static final float SCALE = 0.80F / CONTENT_W;

    /** 薄板厚度 3/16，屏幕贴在“离墙那一面”。 */
    private static final float THIN = 3.0F / 16.0F;
    private static final float THICK_SIDE = 1.0F - THIN;
    private static final float EPS = 0.01F;

    /** 每个数字点亮哪几段（a 上, b 右上, c 右下, d 下, e 左下, f 左上, g 中）。 */
    private static final String[] SEGMENTS = {
            "abcdef", "bc", "abged", "abgcd", "fgbc",
            "afgcd", "afgedc", "abc", "abcdefg", "abcdfg",
    };

    public EternalClockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EternalClockBlockEntity clock, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (clock.getLevel() == null) {
            return;
        }
        Direction facing = clock.getBlockState().getValue(EternalClockBlock.FACING);
        int hour = clock.hour();
        String text = String.format("%02d%02d", hour, clock.minute());
        // 06:00 起算白天，19:00 起算夜晚
        this.activeColor = (hour >= 6 && hour < 19) ? DAY_COLOR : NIGHT_COLOR;

        VertexConsumer consumer = buffer.getBuffer(TYPE);
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        float nx = facing.getStepX();
        float ny = facing.getStepY();
        float nz = facing.getStepZ();

        for (int i = 0; i < 4; i++) {
            drawDigit(consumer, pose, normal, facing, nx, ny, nz, DIGIT_X[i], text.charAt(i) - '0');
        }
        // 冒号夹在第 2、3 位之间
        drawRect(consumer, pose, normal, facing, nx, ny, nz, COLON_X, 14.0F, COLON_X + COLON_W, 18.0F);
        drawRect(consumer, pose, normal, facing, nx, ny, nz, COLON_X, 6.0F, COLON_X + COLON_W, 10.0F);
    }

    private void drawDigit(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Direction facing,
            float nx, float ny, float nz, float x0, int digit) {
        String lit = SEGMENTS[digit];
        float x1 = x0 + DIGIT_W;
        float half = 1.5F;
        float barTop = DIGIT_H - half;
        float barMid0 = DIGIT_H / 2.0F - half;
        float barMid1 = DIGIT_H / 2.0F + half;
        float barBot = half;

        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'a', x0, barTop, x1, DIGIT_H);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'g', x0, barMid0, x1, barMid1);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'd', x0, 0.0F, x1, barBot);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'f', x0, barMid1, x0 + 3.0F, barTop);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'b', x1 - 3.0F, barMid1, x1, barTop);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'e', x0, barBot, x0 + 3.0F, barMid0);
        segment(consumer, pose, normal, facing, nx, ny, nz, lit, 'c', x1 - 3.0F, barBot, x1, barMid0);
    }

    /** 只画点亮的段；没点亮的段直接跳过（否则会在屏幕上看到一排暗色短条）。 */
    private void segment(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Direction facing,
            float nx, float ny, float nz, String lit, char segment, float x0, float y0, float x1, float y1) {
        if (lit.indexOf(segment) < 0) {
            return;
        }
        drawRect(consumer, pose, normal, facing, nx, ny, nz, x0, y0, x1, y1);
    }

    private void drawRect(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Direction facing,
            float nx, float ny, float nz, float x0, float y0, float x1, float y1) {
        corner(consumer, pose, normal, facing, nx, ny, nz, x0, y0);
        corner(consumer, pose, normal, facing, nx, ny, nz, x1, y0);
        corner(consumer, pose, normal, facing, nx, ny, nz, x1, y1);
        corner(consumer, pose, normal, facing, nx, ny, nz, x0, y1);
    }

    private void corner(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, Direction facing,
            float nx, float ny, float nz, float ux, float uy) {
        float u = 0.5F + (ux - CONTENT_W / 2.0F) * SCALE;
        float v = 0.5F + (uy - DIGIT_H / 2.0F) * SCALE;
        float x;
        float y = v;
        float z;
        switch (facing) {
            case SOUTH:
                // 薄板在南侧（z 0~3/16），屏幕朝 +Z
                x = u;
                z = THIN + EPS;
                break;
            case EAST:
                // 薄板在西侧（x 0~3/16），屏幕朝 +X
                x = THIN + EPS;
                z = 1.0F - u;
                break;
            case WEST:
                // 薄板在东侧（x 13/16~1），屏幕朝 -X
                x = THICK_SIDE - EPS;
                z = u;
                break;
            case NORTH:
            default:
                // 薄板在南侧（z 13/16~1），屏幕朝 -Z
                x = 1.0F - u;
                z = THICK_SIDE - EPS;
                break;
        }
        consumer.vertex(pose, x, y, z)
                .color(this.activeColor[0], this.activeColor[1], this.activeColor[2], this.activeColor[3])
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(GLOW)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}
