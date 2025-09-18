package nl.mishaopstal.creeperextended;

import io.wispforest.owo.config.Option;
import io.wispforest.owo.config.annotation.*;

@Modmenu(modId = CreeperExtended.MOD_ID)
@Config(name = "creeper-extended-config", wrapperName = "CreeperExtendedConfig")
public class CreeperExtendedConfigModel {
    @SectionHeader("Explosion")
    @RangeConstraint(min = 1, max = 10)
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public float explosionDelaySeconds = 1.0f; // seconds until explosion after ignited

    @SectionHeader("Animation")
    public boolean spinAnimation = true;
    public boolean showParticles = true;

    @SectionHeader("Flashbang")
    public boolean flashBang = true;
    @RangeConstraint(min = 1, max = 20)
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public int flashBangRadius = 10;
    @RangeConstraint(min = 1, max = 150)
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public int flashBangDurationTicks = 100;
    @RangeConstraint(min = 0, max = 200)
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public int flashBangFadeInTicks = 10;
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public int flashBangColor = 0xFFFFFF; // RGB color of the flash overlay
    @RangeConstraint(min = 0.01f, max = 1.f)
    public float flashBangVolume = 1.0f; // 0.0 .. 1.0

    @SectionHeader("Beeping")
    public boolean beeping = true;
    @RangeConstraint(min = 1, max = 30)
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    public int beepAmountPerFullSpinCycle = 3; // how many beeps per full rotation cycle
    @Sync(Option.SyncMode.OVERRIDE_CLIENT)
    @RangeConstraint(min = 1, max = 4)
    public int beepAmountIncreasePerFullSpinCycle = 2; // how much to increase beeps per full rotation cycle when speedup is active
    @RangeConstraint(min = 0.01f, max = 1.f)
    public float beepVolume = 1.0f; // 0.0 .. 1.0
}
