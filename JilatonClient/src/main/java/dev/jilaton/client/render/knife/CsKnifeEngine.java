/*
 * Ported from Anubis 0.9.9.3 (net.anubis.client.module.impl.render.WeaponInspectModule, "CS Knife"), GPL-3.0, with the
 * owners' permission recorded in development/ORCHESTRATION.md (R4). The module/event/key plumbing is replaced by
 * {@link Config} and {@link #inspect()} so Jilaton's Knife module can drive it; animation, meshes and trails are as
 * ported.
 */
package dev.jilaton.client.render.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class CsKnifeEngine {
   /** Everything the engine reads from its owning module, sampled once per call. */
   public record Config(String skin, String items, float speed, float knifeSize, boolean showArm, boolean deploy,
                        boolean slashTrail, float trailLength, boolean sparkles) { }

   public static volatile CsKnifeEngine active;
   private volatile Config cfg = new Config("Butterfly", "Swords", 1.0F, 1.0F, true, true, true, 0.14F, true);
   private BooleanSupplier inspectHeld = () -> false;
   private static final String SWORDS = "Swords";
   private static final String WEAPONS = "Weapons";
   private static final String ANY_ITEM = "Any Item";
   private static final Identifier KNIFE_TEXTURE = Identifier.fromNamespaceAndPath("jilaton", "textures/misc/knife_base.png");
   private static final float EASE_OUT_SECONDS = 0.15F;
   private static final int IDLE = 0;
   private static final int INSPECT = 1;
   private static final int DEPLOY = 2;
   private static final float[] REST = new float[16];
   private static final float SPARKLE_SIZE = 0.07F;
   private int mode = 0;
   private InspectAnimation.Clip clip;
   private KnifeSkin clipKnife;
   private long startedAt;
   private float seconds;
   private boolean easingOut;
   private long easeStartedAt;
   private float easeFrom;
   private int slot;
   private Item item;
   private int heldSlot = -1;
   private Item heldItem;
   private final float[] state = new float[16];
   private final float[] trailSample = new float[12];
   private final SlashTrail[] trails = new SlashTrail[2];
   private final Matrix4f base = new Matrix4f();
   private final Matrix4f baseInverse = new Matrix4f();
   private final Matrix3f baseNormal = new Matrix3f();
   private boolean baseKnown;

   public CsKnifeEngine() {
      for (int i = 0; i < this.trails.length; i++) {
         this.trails[i] = new SlashTrail();
      }
   }

   public void configure(Config config, BooleanSupplier held) {
      this.cfg = config;
      this.inspectHeld = held;
   }

   /** Becomes the drawing engine for the hand pass. */
   public void activate() {
      this.mode = 0;
      this.heldItem = null;
      this.clearTrails();
      active = this;
   }

   public void deactivate() {
      if (active == this) active = null;
      this.mode = 0;
      this.clearTrails();
   }

   /** Whether the ported engine knows this skin name (Butterfly, Karambit, Katana). */
   public static boolean hasSkin(String name) {
      return KnifeSkin.of(name) != null;
   }

   public static void handsBegin(PoseStack pose) {
      CsKnifeEngine module = active;
      if (module != null) {
         module.base.set(pose.last().pose());
         module.base.invert(module.baseInverse);
         module.baseNormal.set(pose.last().normal());
         module.baseKnown = true;
      }
   }

   public static boolean renderItem(PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand) {
      CsKnifeEngine module = active;
      return module != null && module.render(pose, holder, stack, context, collector, light, mainHand);
   }

   /** Starts an inspect when the held item allows it; the owning module calls this on its inspect key. */
   public boolean inspect() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (mc.screen != null || player == null || player.isSpectator() || player.isShiftKeyDown()) {
         return false;
      }

      ItemStack held = player.getMainHandItem();
      if (!this.inspectable(held) || player.isUsingItem()) {
         return false;
      }

      this.start(1, player.getInventory().getSelectedSlot(), held);
      return true;
   }

   private boolean held() {
      return Minecraft.getInstance().screen == null && this.inspectHeld.getAsBoolean();
   }

   private boolean inspectable(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return !"Any Item".equals(this.cfg.items()) && !isSword(stack)
            ? "Weapons".equals(this.cfg.items())
               && (
                  stack.is(ItemTags.AXES)
                     || stack.is(ItemTags.SPEARS)
                     || stack.is(Items.MACE)
                     || stack.is(Items.TRIDENT)
               )
            : true;
      }
   }

   private static boolean isSword(ItemStack stack) {
      return !stack.isEmpty()
         && (stack.is(ItemTags.SWORDS) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sword"));
   }

   private static boolean handheld(ItemStack stack) {
      return isSword(stack)
         || stack.is(ItemTags.AXES)
         || stack.is(ItemTags.PICKAXES)
         || stack.is(ItemTags.SHOVELS)
         || stack.is(ItemTags.HOES);
   }

   private void start(int next, int selected, ItemStack held) {
      KnifeSkin knife = isSword(held) ? KnifeSkin.of(this.cfg.skin()) : null;
      this.clip = next == 2 ? knife.deploy : (knife != null ? knife.inspect : InspectAnimation.clip("Classic"));
      this.clipKnife = knife;
      this.mode = next;
      this.seconds = this.clip.seconds;
      this.easingOut = false;
      this.startedAt = System.nanoTime();
      this.slot = selected;
      this.item = held.getItem();
   }

   private void easeOut() {
      long now = System.nanoTime();
      this.easeFrom = Math.min(this.progress(now), 1.0F);
      this.easeStartedAt = now;
      this.easingOut = true;
   }

   public void tick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         this.mode = 0;
         this.heldItem = null;
      } else {
         ItemStack held = player.getMainHandItem();
         int selected = player.getInventory().getSelectedSlot();
         if (selected != this.heldSlot || !held.is(this.heldItem)) {
            this.heldSlot = selected;
            this.heldItem = held.getItem();
            if (this.cfg.deploy() && isSword(held) && KnifeSkin.of(this.cfg.skin()) != null) {
               this.start(2, selected, held);
            } else if (this.mode != 0 && !this.easingOut) {
               this.easeOut();
            }
         } else if (this.mode != 0 && !this.easingOut) {
            if (player.swinging) {
               this.mode = 0;
            } else if (player.isUsingItem()) {
               this.easeOut();
            } else if (this.loopedProgress(System.nanoTime()) >= 1.0F) {
               this.mode = 0;
            }
         }
      }
   }

   private float progress(long now) {
      return (float)(now - this.startedAt) / 1.0E9F / (this.seconds / this.cfg.speed());
   }

   private float loopedProgress(long now) {
      float progress = this.progress(now);
      if (this.mode == 1 && this.clip.loops() && !(progress < this.clip.loopExit) && this.held()) {
         float duration = this.seconds / this.cfg.speed();
         long cycle = (long)((this.clip.loopExit - this.clip.loopEnter) * duration * 1.0E9F);
         if (cycle <= 0L) {
            return progress;
         }

         long over = (long)((progress - this.clip.loopExit) * duration * 1.0E9F);
         this.startedAt += (over / cycle + 1L) * cycle;
         return this.progress(now);
      } else {
         return progress;
      }
   }

   private float animate(ItemStack stack, KnifeSkin knife, long now) {
      if (this.mode == 0 || !stack.is(this.item)) {
         return 0.0F;
      }

      if (knife != this.clipKnife) {
         this.mode = 0;
         return 0.0F;
      }

      float weight = 1.0F;
      float progress;
      if (this.easingOut) {
         float t = (float)(now - this.easeStartedAt) / 1.0E9F / 0.15F;
         if (t >= 1.0F) {
            this.mode = 0;
            return 0.0F;
         }

         progress = this.easeFrom;
         weight = 1.0F - t * t * (3.0F - 2.0F * t);
      } else {
         progress = this.loopedProgress(now);
         if (progress >= 1.0F) {
            this.mode = 0;
            return 0.0F;
         }
      }

      this.clip.sample(progress, this.state);
      return weight;
   }

   private boolean render(PoseStack pose, LivingEntity holder, ItemStack stack, ItemDisplayContext context, SubmitNodeCollector collector, int light, boolean mainHand) {
      int side = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
      boolean sword = isSword(stack);
      KnifeSkin knife = sword ? KnifeSkin.of(this.cfg.skin()) : null;
      long now = System.nanoTime();
      float weight = mainHand ? this.animate(stack, knife, now) : 0.0F;
      float[] at = weight > 0.0F ? this.state : REST;
      boolean trailing = mainHand && sword && this.cfg.slashTrail() && this.baseKnown;
      if (mainHand && !trailing) {
         this.clearTrails();
      }

      if (knife == null) {
         if (this.cfg.showArm() && handheld(stack) || false) {
            FirstPersonArm.Arm arm = FirstPersonArm.arm();
            if (arm != null) {
               FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, at[0] * weight, at[1] * weight, at[2] * weight);
            }
         }

         if (weight > 0.0F) {
            InspectAnimation.place(pose, at, weight, side, 0.07F, 0.2F, 0.07F);
         }

         if (trailing) {
            this.swordTrail(pose.last().pose(), side);
            this.record(now, 1);
            this.submitGlow(collector, now, KnifeMaterial.of(stack).glow, 0.0F, null);
         }

         return false;
      } else {
         KnifeMaterial material = KnifeMaterial.of(stack);
         FirstPersonArm.Arm arm = !this.cfg.showArm() && true ? null : FirstPersonArm.arm();
         boolean slim = arm != null && arm.slim();
         pose.pushPose();
         if (side < 0) {
            pose.scale(-1.0F, 1.0F, 1.0F);
         }

         knife.draw(
            pose,
            at,
            weight,
            slim,
            this.cfg.knifeSize(),
            sink(collector, light, side, arm, mainHand && offHandFree(), material, stack.hasFoil()),
            trailing ? this.trailSample : null
         );
         pose.popPose();
         if (trailing) {
            this.record(now, knife.streakCount());
            float flash = this.cfg.sparkles() && weight > 0.0F ? knife.sparkle(at, weight) : 0.0F;
            this.submitGlow(collector, now, material.glow, flash, knife);
         }

         return true;
      }
   }

   private static KnifeSkin.Sink sink(
      SubmitNodeCollector collector, int light, int side, FirstPersonArm.Arm arm, boolean otherFree, KnifeMaterial material, boolean foil
   ) {
      final RenderType solid = RenderTypes.entityCutoutNoCull(KNIFE_TEXTURE);
      final int[] palette = material.palette;
      return new KnifeSkin.Sink() {
         @Override
         public void part(PoseStack part, KnifeMesh mesh, KnifeMesh.Shine shine) {
            collector.submitCustomGeometry(part, solid, (p, out) -> mesh.emit(p, out, palette, light, shine));
            if (foil) {
               collector.order(1).submitCustomGeometry(part, RenderTypes.entityGlint(), (p, out) -> mesh.emit(p, out, palette, light, KnifeMesh.Shine.NONE));
            }
         }

         @Override
         public void glow(PoseStack part, KnifeMesh mesh, KnifeMesh.Shine shine) {
            collector.order(1)
               .submitCustomGeometry(part, RenderTypes.eyes(CsKnifeEngine.KNIFE_TEXTURE), (p, out) -> mesh.emitGlow(p, out, material.glow, shine));
         }

         @Override
         public void arm(PoseStack hand, boolean other) {
            if (arm != null && (!other || otherFree)) {
               arm.draw(hand, side < 0 != other, collector, light);
            }
         }
      };
   }

   private static boolean offHandFree() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && player.getOffhandItem().isEmpty();
   }

   private void swordTrail(Matrix4f pose, int side) {
      Matrix4f m = new Matrix4f(pose)
         .translate(side * 1.13F / 16.0F, 0.2F, 0.070625F)
         .rotate(new Quaternionf().rotationXYZ(0.0F, (float) (-Math.PI / 2), 0.43633232F))
         .scale(0.68F)
         .translate(-0.5F, -0.5F, -0.5F);
      Vector3f p = m.transformPosition(0.3125F, 0.3125F, 0.5F, new Vector3f());
      this.trailSample[0] = p.x;
      this.trailSample[1] = p.y;
      this.trailSample[2] = p.z;
      m.transformPosition(0.96875F, 0.96875F, 0.5F, p);
      this.trailSample[3] = p.x;
      this.trailSample[4] = p.y;
      this.trailSample[5] = p.z;
   }

   private void record(long now, int streaks) {
      Vector3f p = new Vector3f();

      for (int s = 0; s < this.trails.length; s++) {
         if (s >= streaks) {
            this.trails[s].clear();
         } else {
            for (int i = s * 6; i < s * 6 + 6; i += 3) {
               this.baseInverse.transformPosition(this.trailSample[i], this.trailSample[i + 1], this.trailSample[i + 2], p);
               this.trailSample[i] = p.x;
               this.trailSample[i + 1] = p.y;
               this.trailSample[i + 2] = p.z;
            }

            this.trails[s].add(now, this.trailSample, s * 6);
         }
      }
   }

   private void clearTrails() {
      for (SlashTrail trail : this.trails) {
         trail.clear();
      }
   }

   private void submitGlow(SubmitNodeCollector collector, long now, int glow, float flash, KnifeSkin knife) {
      long lifetime = (long)(this.cfg.trailLength() * 1.0E9F);
      int streaks = knife != null ? knife.streakCount() : 1;
      boolean showing = flash > 0.0F;

      for (int s = 0; s < streaks && !showing; s++) {
         showing = !this.trails[s].isEmpty(now, lifetime);
      }

      if (showing) {
         float tipX = this.trailSample[3];
         float tipY = this.trailSample[4];
         float tipZ = this.trailSample[5];
         float turn = (float)(now % 6283185307L) / 1.0E9F * 1.5F;
         PoseStack hand = new PoseStack();
         hand.last().pose().set(this.base);
         hand.last().normal().set(this.baseNormal);
         collector.order(1).submitCustomGeometry(hand, RenderTypes.eyes(KNIFE_TEXTURE), (p, out) -> {
            for (int s = 0; s < streaks; s++) {
               this.trails[s].emit(p, out, now, lifetime, glow, knife != null ? knife.streakStrength(s) : 1.0F);
            }

            if (flash > 0.0F) {
               SlashTrail.sparkle(p, out, tipX, tipY, tipZ, 0.07F * flash, turn, glow, flash);
            }
         });
      }
   }
}
