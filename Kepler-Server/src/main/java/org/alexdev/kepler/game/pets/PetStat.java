package org.alexdev.kepler.game.pets;

/**
 * A pet's needs, as levels from 0 (empty) to max. The status window shows hunger, thirst and happiness with the
 * client's texts pet_hung_0..6, pet_thir_0..3 and pet_mood_0..6; energy isn't shown (it decides when the pet sleeps).
 *
 * Each level is stored as a timestamp in items_pets (Pet#getLevel): the level drops one step every
 * "pet.<stat>.step.minutes" after it, so a need keeps dropping while nobody is in the room.
 */
public enum PetStat {
    HUNGER(6, "pet.hunger.step.minutes"),
    THIRST(3, "pet.thirst.step.minutes"),
    HAPPINESS(6, "pet.happiness.step.minutes"),
    ENERGY(6, "pet.energy.step.minutes");

    private final int max;
    private final String stepSetting;

    PetStat(int max, String stepSetting) {
        this.max = max;
        this.stepSetting = stepSetting;
    }

    public int getMax() {
        return max;
    }

    public String getStepSetting() {
        return stepSetting;
    }
}
