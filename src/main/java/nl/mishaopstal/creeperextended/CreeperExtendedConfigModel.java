package nl.mishaopstal.creeperextended;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;
import io.wispforest.owo.config.annotation.SectionHeader;

@Modmenu(modId = CreeperExtended.MOD_ID)
@Config(name = "creeper-extended-config", wrapperName = "CreeperExtendedConfig")
public class CreeperExtendedConfigModel {
    @SectionHeader("Animation")
    public boolean spinAnimation = true;
    public boolean showParticles = true;

    @SectionHeader("Flashbang")
    public boolean flashBang = true;
    @RangeConstraint(min = 1, max = 20)
    public int flashBangRadius = 10;
    @RangeConstraint(min = 1, max = 1000)
    public int flashBangDurationTicks = 100;
    @RangeConstraint(min = 0, max = 1)
    public float flashBangBlindnessStrength = 1.0f; // 0.0 .. 1.0
    @RangeConstraint(min = 0, max = 1)
    public float flashBangNauseaStrength = 0.5f; // 0.0 .. 1.0

    @SectionHeader("Beeping")
    public boolean beeping = true;
    @RangeConstraint(min = 1, max = 30)
    public int beepAmountPerFullSpinCycle = 3; // how many beeps per full rotation cycle
    @RangeConstraint(min = 0, max = 5)
    public float beepVolume = 1.0f; // 0.0 .. 1.0
}
