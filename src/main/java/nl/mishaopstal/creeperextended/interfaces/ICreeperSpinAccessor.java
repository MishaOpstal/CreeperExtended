package nl.mishaopstal.creeperextended.interfaces;

/**
 * Accessor interface exposed by our CreeperEntity mixin for querying spin state.
 * Placed outside the mixin package to avoid Mixin transformer attempting to load/transform it.
 */
public interface ICreeperSpinAccessor {
    float creeperextended$getSpinAngle();
    float creeperextended$getSpinSpeedRps();
    boolean creeperextended$isSpinActive();
}
