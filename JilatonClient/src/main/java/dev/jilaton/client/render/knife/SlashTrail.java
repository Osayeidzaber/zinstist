/*
 * Ported from Anubis 0.9.9.3 (net.anubis.client.module.impl.render.SlashTrail), GPL-3.0, with the owners' permission
 * recorded in development/ORCHESTRATION.md (R4). Package and API adaptation only.
 */
package dev.jilaton.client.render.knife;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.vertex.PoseStack.Pose;

@Environment(EnvType.CLIENT)
final class SlashTrail {
   private static final int CAPACITY = 96;
   private static final long GAP_NANOS = 100000000L;
   private static final float SLOW = 0.6F;
   private static final float FAST = 2.8F;
   private static final float ALPHA = 0.6F;
   private static final float BASE_ALPHA = 0.12F;
   private final float[] points = new float[576];
   private final long[] times = new long[96];
   private int newest = -1;
   private int size;

   void add(long now, float[] sample, int from) {
      this.newest = (this.newest + 1) % 96;
      System.arraycopy(sample, from, this.points, this.newest * 6, 6);
      this.times[this.newest] = now;
      this.size = Math.min(this.size + 1, 96);
   }

   void clear() {
      this.size = 0;
      this.newest = -1;
   }

   boolean isEmpty(long now, long lifetime) {
      return this.size == 0 || now - this.times[this.newest] > lifetime;
   }

   void emit(Pose pose, VertexConsumer out, long now, long lifetime, int rgb, float strength) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;

      for (int k = 0; k + 1 < this.size; k++) {
         int newer = Math.floorMod(this.newest - k, 96);
         int older = Math.floorMod(this.newest - k - 1, 96);
         long ageNewer = now - this.times[newer];
         long ageOlder = now - this.times[older];
         if (ageOlder > lifetime) {
            break;
         }

         long dt = this.times[newer] - this.times[older];
         if (dt > 0L && dt <= 100000000L) {
            int n = newer * 6;
            int o = older * 6;
            float dx = this.points[n + 3] - this.points[o + 3];
            float dy = this.points[n + 4] - this.points[o + 4];
            float dz = this.points[n + 5] - this.points[o + 5];
            float speed = Mth.sqrt(dx * dx + dy * dy + dz * dz) / ((float)dt / 1.0E9F);
            float fast = Mth.clamp((speed - 0.6F) / 2.1999998F, 0.0F, 1.0F);
            if (!(fast <= 0.0F)) {
               float aNewer = fade(ageNewer, lifetime) * fast * strength;
               float aOlder = fade(ageOlder, lifetime) * fast * strength;
               float[] quad = new float[]{
                  this.points[o],
                  this.points[o + 1],
                  this.points[o + 2],
                  aOlder * 0.12F,
                  this.points[o + 3],
                  this.points[o + 4],
                  this.points[o + 5],
                  aOlder,
                  this.points[n + 3],
                  this.points[n + 4],
                  this.points[n + 5],
                  aNewer,
                  this.points[n],
                  this.points[n + 1],
                  this.points[n + 2],
                  aNewer * 0.12F
               };

               for (int v = 0; v < 4; v++) {
                  vertex(pose, out, quad, v, r, g, b);
               }

               for (int v = 3; v >= 0; v--) {
                  vertex(pose, out, quad, v, r, g, b);
               }
            }
         }
      }
   }

   private static float fade(long age, long lifetime) {
      float left = 1.0F - Mth.clamp((float)age / (float)lifetime, 0.0F, 1.0F);
      return left * left * 0.6F;
   }

   private static void vertex(Pose pose, VertexConsumer out, float[] quad, int v, int r, int g, int b) {
      int i = v * 4;
      out.addVertex(pose, quad[i], quad[i + 1], quad[i + 2])
         .setColor(r, g, b, Math.round(Mth.clamp(quad[i + 3], 0.0F, 1.0F) * 255.0F))
         .setUv(0.5F, 0.5F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 0.0F, 1.0F);
   }

   static void sparkle(Pose pose, VertexConsumer out, float x, float y, float z, float size, float turn, int rgb, float alpha) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      int core = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);

      for (int arm = 0; arm < 4; arm++) {
         float angle = turn + arm * (float) (Math.PI / 2);
         float cos = Mth.cos(angle);
         float sin = Mth.sin(angle);
         float length = size * (arm % 2 == 0 ? 1.0F : 0.7F);
         float width = size * 0.16F;
         float tx = x + cos * length;
         float ty = y + sin * length;
         float lx = x - sin * width;
         float ly = y + cos * width;
         float rx = x + sin * width;
         float ry = y - cos * width;
         star(pose, out, x, y, z, r, g, b, core);
         star(pose, out, lx, ly, z, r, g, b, core / 3);
         star(pose, out, tx, ty, z, r, g, b, 0);
         star(pose, out, rx, ry, z, r, g, b, core / 3);
         star(pose, out, rx, ry, z, r, g, b, core / 3);
         star(pose, out, tx, ty, z, r, g, b, 0);
         star(pose, out, lx, ly, z, r, g, b, core / 3);
         star(pose, out, x, y, z, r, g, b, core);
      }
   }

   private static void star(Pose pose, VertexConsumer out, float x, float y, float z, int r, int g, int b, int a) {
      out.addVertex(pose, x, y, z)
         .setColor(r, g, b, a)
         .setUv(0.5F, 0.5F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 0.0F, 1.0F);
   }
}
