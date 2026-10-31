package com.fromthehorror.client;

import com.fromthehorror.FromTheHorror;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Identifier;

public class StalkerRenderer extends MobEntityRenderer<HostileEntity, PlayerEntityModel<HostileEntity>> {
    private final Identifier texture;
    private final boolean tall;

    public StalkerRenderer(EntityRendererFactory.Context ctx, String name, boolean tall) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
        this.texture = new Identifier(FromTheHorror.ID, "textures/entity/" + name + ".png");
        this.tall = tall;
    }

    @Override
    public Identifier getTexture(HostileEntity entity) { return texture; }

    @Override
    protected void scale(HostileEntity entity, MatrixStack matrices, float amount) {
        if (tall) matrices.scale(1.05f, 1.2f, 1.05f);
    }
}
