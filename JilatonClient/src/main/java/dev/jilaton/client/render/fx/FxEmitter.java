package dev.jilaton.client.render.fx;

/**
 * Implemented by modules that contribute world-space geometry. The overlay renderer calls every enabled
 * emitter once per frame during extraction; emitters must only append to the frame, never draw directly.
 */
public interface FxEmitter {
    void emit(FxFrame frame);
}
