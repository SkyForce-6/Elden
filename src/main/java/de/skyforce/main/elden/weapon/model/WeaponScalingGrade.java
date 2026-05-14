package de.skyforce.main.elden.weapon.model;

/**
 * Scaling grades as in Elden Ring (S > A > B > C > D > E).
 * The {@code factor} determines how much of the max attribute bonus
 * is applied to the weapon's damage.
 */
public enum WeaponScalingGrade {

    S(1.00),
    A(0.85),
    B(0.70),
    C(0.55),
    D(0.40),
    E(0.25),
    NONE(0.00);

    private final double factor;

    WeaponScalingGrade(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}

