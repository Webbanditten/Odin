package org.alexdev.kepler.game.pets;

import org.alexdev.kepler.dao.mysql.PetDao;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.fuserights.Fuse;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.room.entities.RoomPet;
import org.alexdev.kepler.util.DateUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * A pet in a room. Its needs (PetStat) are levels stored as timestamps in items_pets; its nature (the status window's
 * "Natur:" words) and friendship with its owner decide how it behaves (PetTask) and how well it obeys (PetManager).
 */
public class Pet extends Entity {
    /** pet_enrg_0..11 and pet_frnd_0..11 in the client's texts. */
    public static final int NATURE_COUNT = 12;
    public static final float FRIENDSHIP_START = 30;
    public static final float FRIENDSHIP_MAX = 100;
    /** Friendship lost per day nobody paid the pet any attention (applied when its room loads). */
    private static final float FRIENDSHIP_LOSS_PER_DAY = 2;
    private static final float FRIENDSHIP_MIN_AFTER_LOSS = 10;

    // How active a pet is by its energy nature (pet_enrg_N): Træt .. Utrættelig, then Sur (grumpy) and Gal (crazy).
    private static final double[] ACTIVITY = { 0.10, 0.20, 0.25, 0.30, 0.45, 0.60, 0.70, 0.80, 0.90, 1.00, 0.50, 0.95 };
    // How much a pet seeks people by its friendliness nature (pet_frnd_N): Fjendtlig .. Ivrig.
    private static final double[] SOCIABILITY = { 0.05, 0.10, 0.20, 0.35, 0.45, 0.60, 0.70, 0.80, 0.85, 0.90, 0.75, 0.95 };

    private final PetDetails petDetails;
    private final RoomPet roomUser;
    private int ownerId;

    public Pet(PetDetails petDetails) {
        this.petDetails = petDetails;
        this.roomUser = new RoomPet(this);
    }

    public String getName() {
        return this.petDetails.getName();
    }

    public int getAge() {
        return (int) TimeUnit.SECONDS.toDays(DateUtil.getCurrentTimeSeconds() - this.petDetails.getBorn());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Needs
    // ------------------------------------------------------------------------------------------------------------

    private static long stepSeconds(PetStat stat) {
        return Math.max(60, GameConfiguration.getInstance().getInteger(stat.getStepSetting()) * 60L);
    }

    private long getTimestamp(PetStat stat) {
        switch (stat) {
            case HUNGER: return this.petDetails.getLastEat();
            case THIRST: return this.petDetails.getLastDrink();
            case HAPPINESS: return this.petDetails.getLastPlayToy();
            default: return this.petDetails.getLastKip();
        }
    }

    private void setTimestamp(PetStat stat, long timestamp) {
        switch (stat) {
            case HUNGER: this.petDetails.setLastEat(timestamp); break;
            case THIRST: this.petDetails.setLastDrink(timestamp); break;
            case HAPPINESS: this.petDetails.setLastPlayToy(timestamp); break;
            default: this.petDetails.setLastKip(timestamp); break;
        }
    }

    /**
     * The level of a need: max right after it was met, one step less every "pet.<stat>.step.minutes", never below 0.
     */
    public int getLevel(PetStat stat) {
        long elapsed = Math.max(0, DateUtil.getCurrentTimeSeconds() - this.getTimestamp(stat));
        long steps = elapsed / stepSeconds(stat);
        return (int) Math.max(0, stat.getMax() - steps);
    }

    /**
     * Set a need to a level (not saved: call {@link #saveDetails()}).
     */
    public void setLevel(PetStat stat, int level) {
        level = Math.max(0, Math.min(stat.getMax(), level));
        this.setTimestamp(stat, DateUtil.getCurrentTimeSeconds() - (stat.getMax() - level) * stepSeconds(stat));
    }

    public void addLevel(PetStat stat, int amount) {
        this.setLevel(stat, this.getLevel(stat) + amount);
    }

    public int getHunger() {
        return this.getLevel(PetStat.HUNGER);
    }

    public int getThirst() {
        return this.getLevel(PetStat.THIRST);
    }

    public int getHappiness() {
        return this.getLevel(PetStat.HAPPINESS);
    }

    public int getEnergy() {
        return this.getLevel(PetStat.ENERGY);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Nature and friendship
    // ------------------------------------------------------------------------------------------------------------

    /** Index of the pet_enrg_ word (items_pets.nature_negative). */
    public int getEnergyNature() {
        return Math.max(0, Math.min(NATURE_COUNT - 1, this.petDetails.getNatureNegative()));
    }

    /** Index of the pet_frnd_ word (items_pets.nature_positive). */
    public int getFriendlinessNature() {
        return Math.max(0, Math.min(NATURE_COUNT - 1, this.petDetails.getNaturePositive()));
    }

    /** 0..1: how much the pet moves around and plays. */
    public double getActivity() {
        return ACTIVITY[this.getEnergyNature()];
    }

    /** 0..1: how much the pet seeks people. */
    public double getSociability() {
        return SOCIABILITY[this.getFriendlinessNature()];
    }

    /** Fjendtlig, Vred, Mistænksom: growls at strangers. */
    public boolean isHostile() {
        return this.getFriendlinessNature() <= 2;
    }

    /** Sur: grumbles and refuses more often. */
    public boolean isGrumpy() {
        return this.getEnergyNature() == 10;
    }

    /** Gal: pulls crazy faces now and then. */
    public boolean isCrazy() {
        return this.getEnergyNature() == 11;
    }

    /** 0..100, with its owner. */
    public float getFriendship() {
        return Math.max(0, Math.min(FRIENDSHIP_MAX, this.petDetails.getFriendship()));
    }

    public void addFriendship(float amount) {
        this.petDetails.setFriendship(Math.max(0, Math.min(FRIENDSHIP_MAX, this.getFriendship() + amount)));
    }

    /** Someone paid the pet attention (a command it obeyed, praise): friendship stops fading for a day. */
    public void markAttention() {
        this.petDetails.setLastPlayUser(DateUtil.getCurrentTimeSeconds());
    }

    /**
     * The pet's room loaded: it rested while nobody was there, and its friendship faded for every whole day without
     * attention.
     */
    public void restAfterAbsence() {
        this.setLevel(PetStat.ENERGY, PetStat.ENERGY.getMax());

        long now = DateUtil.getCurrentTimeSeconds();
        long away = now - this.petDetails.getLastPlayUser();
        long days = away / TimeUnit.DAYS.toSeconds(1);

        if (days > 0) {
            float floor = Math.min(this.getFriendship(), FRIENDSHIP_MIN_AFTER_LOSS);
            this.petDetails.setFriendship(Math.max(floor, this.getFriendship() - days * FRIENDSHIP_LOSS_PER_DAY));
            // Keep the part of a day that hasn't counted yet.
            this.petDetails.setLastPlayUser(now - (away % TimeUnit.DAYS.toSeconds(1)));
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** The Habbo who owns the pet (the owner of its basket). */
    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public boolean hasFuse(Fuse permission) {
        return false;
    }

    @Override
    public PetDetails getDetails() {
        return this.petDetails;
    }

    @Override
    public RoomPet getRoomUser() {
        return this.roomUser;
    }

    @Override
    public EntityType getType() {
        return EntityType.PET;
    }

    @Override
    public void dispose() {

    }

    /**
     * Save needs, friendship and position.
     */
    public void saveDetails() {
        Position position = this.roomUser.getPosition();

        if (position == null || this.roomUser.getRoom() == null) {
            PetDao.saveDetails(this.petDetails.getId(), this.petDetails, this.petDetails.getX(), this.petDetails.getY(), this.petDetails.getRotation());
            return;
        }

        PetDao.saveDetails(this.petDetails.getId(), this.petDetails, position.getX(), position.getY(), position.getRotation());
    }
}
