package dev.jilaton.client.module.impl;

import dev.jilaton.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomFovModuleTest {
    @Test void overlayCompensationUsesPerspectiveTangentRatio() {
        assertEquals(1, CustomFovModule.overlayScale(70, 70));
        assertEquals(Math.tan(Math.toRadians(55)) / Math.tan(Math.toRadians(35)),
                CustomFovModule.overlayScale(70, 110), 1e-6);
        assertEquals(1, CustomFovModule.overlayScale(0, 110));
        assertEquals(1, CustomFovModule.overlayScale(70, Float.NaN));
        assertEquals(1, CustomFovModule.overlayScale(180, 110));
    }

    @Test void fixedFovYieldsToPanoramaAndDisableRestoresUnmodifiedValues() {
        CustomFovModule module = new CustomFovModule();
        NumberSetting setting = (NumberSetting) module.settings().getFirst();
        setting.setValue(120);
        assertEquals(80, module.apply(80, true, false));
        module.setEnabled(true);
        try {
            assertEquals(120, module.apply(80, true, false));
            assertEquals(120, module.apply(70, false, false));
            assertEquals(CustomFovModule.overlayScale(70, 120), module.effectScale());
            assertEquals(90, module.apply(90, false, true));
            assertEquals(1, module.effectScale());
        } finally { module.setEnabled(false); }
        assertEquals(80, module.apply(80, true, false));
        assertEquals(1, module.effectScale());
    }
}
