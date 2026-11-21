package org.alexdev.kepler.game.room.tasks;

import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.item.interactors.InteractionType;
import org.alexdev.kepler.game.item.interactors.types.PetWaterBowlInteractor;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.pets.*;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.entities.RoomPet;
import org.alexdev.kepler.game.room.managers.RoomEntityManager;
import org.alexdev.kepler.game.room.mapping.RoomTile;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.messages.outgoing.rooms.user.CHAT_MESSAGE;
import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.GameConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A pet's mind, run once a second by StatusTask (reference: Odin/docs/pets.md).
 *
 * Needs come first: a tired pet goes to its basket and sleeps (longer at night), a thirsty or hungry one goes to its
 * water or food, or begs its owner when there is none (a thirsty one first shows it the empty bowl), an unhappy one plays with a ball. Otherwise it idles the way its
 * nature says (energetic pets wander and play more, lazy ones sit and lie down, friendly ones seek people): sniffing,
 * scratching, stretching, wagging, looking around, short walks near where it is. It greets its owner when they come in,
 * sniffs or growls at visitors, looks at whoever says its name, and obeys commands (PetManager) depending on
 * friendship, mood and nature.
 *
 * Every public method is synchronized: the room's StatusTask (tick), EntityTask (the item interactors call
 * {@link #reachedItem}) and the chat thread ({@link #hear}) all call in.
 */
public class PetTask extends TickTask {
    public enum Activity { IDLE, WANDER, GO_FOOD, GO_WATER, GO_TOY, GO_NEST, GO_PERSON, GO_AWAY, FOLLOW, EATING, DRINKING, PLAYING, SLEEPING, POSE }

    private enum Purpose { COME, GREET, VISIT, SNIFF, BEG }

    /** Everything that is a body pose: cleared before a new one, and on every step (EntityTask). */
    public static final StatusType[] POSES = {
            StatusType.SIT, StatusType.LAY, StatusType.PET_SLEEP, StatusType.DEAD, StatusType.JUMP, StatusType.EAT,
            StatusType.PLAY, StatusType.SNIFF, StatusType.SCRATCH, StatusType.STRETCH, StatusType.BEG, StatusType.READY
    };

    /** A food bowl is gone after this many meals (its custom data counts them, 0..3); a treat after one. */
    private static final int BOWL_MEALS = 4;
    private static final int TOY_FRAMES = 6;

    private final Pet pet;
    private final Room room;

    private Activity activity = Activity.IDLE;
    private int timer;
    private Item targetItem;
    private Player targetPlayer;
    private Purpose purpose;
    private boolean ordered;
    private int retries;
    private boolean passive;

    private long nextDecision;
    private long nextSpeech;
    private long nextSocial;
    private long nextFriendshipGain;
    private long nextPraise;
    private long nextScold;
    private long lastObeyed;
    private long nextSave;
    private long nextEmptyBowl;
    private boolean dirty;

    private long minSleepUntil;
    private int recoverCounter;
    private String toyState;
    private int toyFrame;
    private int lastIdle = -1;

    private final Set<Integer> seen = new HashSet<>();

    public PetTask(Pet pet, Room room) {
        this.pet = pet;
        this.room = room;
        this.nextDecision = now() + seconds(2, 6);
    }

    /**
     * The pet came into its room (the room loaded, or its basket was placed).
     */
    public synchronized void onLoaded() {
        this.pet.restAfterAbsence();
        this.pet.saveDetails();
        this.nextSave = now() + 60_000;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The tick
    // ------------------------------------------------------------------------------------------------------------

    @Override
    public synchronized void tick() {
        if (this.user().getRoom() != this.room) {
            return;
        }

        long now = now();

        if (this.passive) {
            return;
        }

        this.watchPeople(now);

        switch (this.activity) {
            case EATING:
                if (--this.timer <= 0) this.finishEating(true);
                break;
            case DRINKING:
                if (--this.timer <= 0) this.finishDrinking(true);
                break;
            case PLAYING:
                this.spinToy();

                if (--this.timer <= 0) {
                    this.finishPlaying(true);
                } else if (this.timer % 7 == 0) {
                    this.say(PetTexts.Mood.PLAY, 0.4);
                }
                break;
            case POSE:
                if (--this.timer <= 0) this.endPose();
                break;
            case SLEEPING:
                this.sleepTick(now);
                break;
            case FOLLOW:
                this.followTick();
                break;
            case GO_PERSON:
                this.personTick();
                break;
            case GO_FOOD:
            case GO_WATER:
            case GO_TOY:
            case GO_NEST:
                this.goalTick();
                break;
            case WANDER:
            case GO_AWAY:
                if (!this.user().isWalking()) {
                    this.idle(now, 2, 6);
                }
                break;
            default:
                break;
        }

        if (this.activity == Activity.IDLE && now >= this.nextDecision) {
            this.decide(now);
        }

        if (this.dirty && now >= this.nextSave) {
            this.save();
        }
    }

    private void decide(long now) {
        int energy = this.pet.getEnergy();
        int hunger = this.pet.getHunger();
        int thirst = this.pet.getThirst();
        int happiness = this.pet.getHappiness();
        boolean night = isNight();
        double activity = this.pet.getActivity();

        // Active pets make up their mind more often.
        this.nextDecision = now + (long) (seconds(6, 15) * (1.35 - activity * 0.6));

        if (energy <= 1 || (night && energy < PetStat.ENERGY.getMax() && chance(0.5)) || (night && chance(0.2))) {
            this.goSleep(false);
            return;
        }

        if (thirst <= 1 || (thirst == 2 && chance(0.3))) {
            if (this.seek(Activity.GO_WATER, false)) return;
            if (thirst <= 1 && this.seekEmptyBowl(false)) return;
            if (thirst <= 1) { this.complain(); return; }
        }

        if (hunger <= 2 || (hunger == 3 && chance(0.3))) {
            if (this.seek(Activity.GO_FOOD, false)) return;
            if (hunger <= 2) { this.complain(); return; }
        }

        if (energy >= 3 && (happiness <= 3 || chance(0.05 + 0.15 * activity))) {
            if (this.seek(Activity.GO_TOY, false)) return;
        }

        Player owner = this.getOwner();

        if (owner != null && now >= this.nextSocial && distance(owner) > 2
                && chance(0.05 + 0.15 * this.pet.getSociability() + this.pet.getFriendship() / 1000)) {
            this.nextSocial = now + 30_000;

            if (this.goToPerson(owner, Purpose.VISIT)) return;
        }

        this.idleAction(activity, owner, happiness, energy);
    }

    private void idleAction(double activity, Player owner, int happiness, int energy) {
        double sociability = this.pet.getSociability();
        boolean someoneNear = this.room.getEntityManager().getPlayers().stream()
                .anyMatch(p -> !RoomEntityManager.isHidden(p) && distance(p) <= 3);

        double[] weights = {
                2 + 4 * activity,                                   // 0 wander
                2.5,                                                 // 1 sniff
                1,                                                   // 2 scratch
                0.6,                                                 // 3 stretch
                2 * (1.2 - activity) + 0.5,                          // 4 sit
                2 * (1.2 - activity) + (energy <= 3 ? 1.5 : 0),      // 5 lie down
                1,                                                   // 6 look around
                someoneNear ? 1.5 * sociability : 0,                 // 7 wag
                0.4 * activity,                                      // 8 jump
                this.pet.isCrazy() ? 1 : 0,                          // 9 crazy face
                happiness <= 2 ? 1.5 : 0,                            // 10 sad
                0.8                                                  // 11 make a noise
        };

        // Not the same thing twice in a row, mostly.
        if (this.lastIdle >= 0 && this.lastIdle < weights.length) {
            weights[this.lastIdle] *= 0.25;
        }

        int choice = pick(weights);
        this.lastIdle = choice;

        switch (choice) {
            case 0:
                Position tile = this.tileNear(2 + (int) Math.round(activity * 5));

                if (tile != null && this.walk(tile)) {
                    this.activity = Activity.WANDER;
                    return;
                }

                this.pose(StatusType.SNIFF, seconds(3, 5) / 1000);
                break;
            case 1:
                this.pose(StatusType.SNIFF, seconds(3, 6) / 1000);
                this.say(PetTexts.Mood.SNIFF, 0.35);
                break;
            case 2:
                this.pose(StatusType.SCRATCH, seconds(3, 5) / 1000);
                break;
            case 3:
                this.pose(StatusType.STRETCH, seconds(2, 4) / 1000);
                break;
            case 4:
                this.pose(StatusType.SIT, seconds(8, 25) / 1000);
                if (chance(0.3)) this.wag(4);
                break;
            case 5:
                this.pose(StatusType.LAY, seconds(10, 30) / 1000);
                break;
            case 6:
                Position position = this.user().getPosition();
                position.setHeadRotation(Math.floorMod(position.getBodyRotation() + (chance(0.5) ? 1 : -1), 8));
                this.user().setNeedsUpdate(true);
                this.idle(now(), 2, 5);
                break;
            case 7:
                this.wag(4);
                this.face("sml", 3);
                this.idle(now(), 3, 6);
                break;
            case 8:
                this.pose(StatusType.JUMP, 2);
                break;
            case 9:
                this.face("crz", 3);
                this.idle(now(), 3, 6);
                break;
            case 10:
                this.face("sad", 5);
                this.pose(StatusType.LAY, seconds(8, 15) / 1000);
                break;
            default:
                this.say(PetTexts.Mood.GENERIC, 1);
                this.idle(now(), 3, 8);
                break;
        }
    }

    /** Hungry or thirsty with nothing there: beg the owner, or look sad. */
    private void complain() {
        Player owner = this.getOwner();

        if (owner != null && distance(owner) > 1 && chance(0.6) && this.goToPerson(owner, Purpose.BEG)) {
            return;
        }

        this.face("sad", 5);
        this.pose(StatusType.BEG, seconds(5, 8) / 1000);
        this.say(PetTexts.Mood.BEG, 0.6);
    }

    // ------------------------------------------------------------------------------------------------------------
    // People
    // ------------------------------------------------------------------------------------------------------------

    private void watchPeople(long now) {
        List<Player> players = this.room.getEntityManager().getPlayers();
        Set<Integer> present = new HashSet<>();

        for (Player player : players) {
            if (RoomEntityManager.isHidden(player)) {
                continue;
            }

            int id = player.getDetails().getId();
            present.add(id);

            if (!this.seen.add(id)) {
                continue;
            }

            boolean owner = id == this.pet.getOwnerId();

            if (this.activity == Activity.SLEEPING) {
                if (owner && chance(0.5)) {
                    this.wakeUp(false);
                } else {
                    continue;
                }
            }

            if (now < this.nextSocial || this.isBusy()) {
                continue;
            }

            if (owner) {
                if (chance(0.4 + 0.6 * this.pet.getSociability()) && this.goToPerson(player, Purpose.GREET)) {
                    this.nextSocial = now + 20_000;
                }
            } else if (this.pet.isHostile()) {
                if (chance(0.5)) {
                    this.nextSocial = now + 20_000;
                    this.user().look(player.getRoomUser().getPosition(), true);
                    this.face("agr", 3);
                    this.say(PetTexts.Mood.ANGRY, 0.7);
                }
            } else if (chance(0.6 * this.pet.getSociability()) && this.goToPerson(player, Purpose.SNIFF)) {
                this.nextSocial = now + 20_000;
            }
        }

        this.seen.retainAll(present);
    }

    /** Eating, drinking, playing or following: not to be pulled away by people coming in. */
    private boolean isBusy() {
        switch (this.activity) {
            case EATING: case DRINKING: case PLAYING: case FOLLOW: case GO_FOOD: case GO_WATER: case GO_PERSON:
                return true;
            case POSE:
                return this.ordered;
            default:
                return false;
        }
    }

    private boolean goToPerson(Player player, Purpose purpose) {
        if (player.getRoomUser().getRoom() != this.room) {
            return false;
        }

        this.stopActivity();
        this.targetPlayer = player;
        this.purpose = purpose;
        this.retries = 0;

        if (distance(player) <= 1) {
            this.arrivePerson();
            return true;
        }

        Position tile = this.tileNextTo(player.getRoomUser().getPosition());

        if (tile == null || !this.walk(tile)) {
            this.targetPlayer = null;
            return false;
        }

        this.activity = Activity.GO_PERSON;
        return true;
    }

    private void personTick() {
        Player player = this.targetPlayer;

        if (player == null || player.getRoomUser().getRoom() != this.room) {
            this.idle(now(), 2, 5);
            return;
        }

        if (this.user().isWalking()) {
            return;
        }

        if (distance(player) <= 1) {
            this.arrivePerson();
            return;
        }

        Position tile = this.tileNextTo(player.getRoomUser().getPosition());

        if (++this.retries > 3 || tile == null || !this.walk(tile)) {
            this.idle(now(), 2, 5);
        }
    }

    private void arrivePerson() {
        Player player = this.targetPlayer;
        Purpose purpose = this.purpose;

        this.targetPlayer = null;
        this.activity = Activity.IDLE;
        this.user().look(player.getRoomUser().getPosition(), true);

        switch (purpose) {
            case GREET:
                this.face("joy", 4);
                this.wag(6);
                this.say(chance(0.5) ? PetTexts.Mood.PLAY : PetTexts.Mood.GENERIC, 0.8);
                this.gainFriendship(0.5f);

                if (this.pet.getActivity() >= 0.7 && chance(0.5)) {
                    this.pose(StatusType.JUMP, 2);
                    return;
                }

                this.idle(now(), 4, 7);
                break;
            case COME:
                this.wag(5);
                this.face("sml", 3);

                if (chance(0.5)) {
                    this.pose(StatusType.SIT, seconds(6, 12) / 1000);
                    return;
                }

                this.idle(now(), 4, 8);
                break;
            case VISIT:
                this.wag(4);

                if (chance(0.5)) {
                    this.pose(StatusType.SIT, seconds(6, 15) / 1000);
                    return;
                }

                this.idle(now(), 3, 7);
                break;
            case SNIFF:
                this.pose(StatusType.SNIFF, 3);
                this.say(PetTexts.Mood.SNIFF, 0.3);

                if (chance(this.pet.getSociability())) {
                    this.wag(4);
                }
                break;
            case BEG:
                this.pose(StatusType.BEG, seconds(6, 10) / 1000);
                this.say(PetTexts.Mood.BEG, 0.8);
                break;
        }
    }

    private void followTick() {
        Player player = this.targetPlayer;

        if (player == null || player.getRoomUser().getRoom() != this.room || --this.timer <= 0) {
            this.targetPlayer = null;
            this.pet.addLevel(PetStat.ENERGY, -1);
            this.dirty = true;

            if (chance(0.5)) {
                this.pose(StatusType.SIT, seconds(5, 10) / 1000);
            } else {
                this.idle(now(), 2, 5);
            }
            return;
        }

        if (this.timer % 10 == 0) {
            this.wag(3);
        }

        Position target = player.getRoomUser().getPosition();
        RoomPet user = this.user();

        if (user.isWalking()) {
            Position goal = user.getGoal();

            if (goal == null || goal.getDistanceSquared(target) <= 1) {
                return;
            }
        } else if (distance(player) <= 1) {
            if (!user.getPosition().equals(target)) {
                user.look(target, true);
            }
            return;
        }

        Position tile = this.tileNextTo(target);

        if (tile != null) {
            this.walk(tile);
        }
    }

    /**
     * Someone said the pet's name. With a command when they may give one (owner, rights, staff).
     */
    public synchronized void hear(Player speaker, PetCommand command, boolean mayCommand) {
        if (this.passive || this.user().getRoom() != this.room) {
            return;
        }

        boolean owner = speaker.getDetails().getId() == this.pet.getOwnerId();
        Position from = speaker.getRoomUser().getPosition();

        if (command == null) {
            if (this.activity == Activity.SLEEPING) {
                this.say(PetTexts.Mood.SLEEP, 0.2);
                return;
            }

            if (this.activity == Activity.EATING || this.activity == Activity.DRINKING || this.activity == Activity.PLAYING) {
                return;
            }

            this.user().look(from, true);

            if (this.pet.isHostile() && !owner) {
                this.face("agr", 2);
                this.say(PetTexts.Mood.ANGRY, 0.4);
            } else if (chance(this.pet.getSociability())) {
                this.wag(3);
            }
            return;
        }

        if (this.activity == Activity.SLEEPING) {
            if (command == PetCommand.NEST) {
                this.say(PetTexts.Mood.SLEEP, 0.5);
                return;
            }

            double wake = (owner ? 0.6 : 0.3) + (command == PetCommand.COME_HERE || command == PetCommand.FOLLOW_ME ? 0.2 : 0);

            if (!chance(wake)) {
                this.say(PetTexts.Mood.SLEEP, 0.6);
                return;
            }

            this.wakeUp(false);
        }

        if ((this.activity == Activity.EATING || this.activity == Activity.DRINKING) && command != PetCommand.BAD) {
            this.say(PetTexts.Mood.EAT, 0.5);
            return;
        }

        if (command == PetCommand.GOOD) {
            this.praise(owner, from);
            return;
        }

        if (command == PetCommand.BAD) {
            this.scold(from);
            return;
        }

        if ((command == PetCommand.EAT && this.pet.getHunger() >= PetStat.HUNGER.getMax())
                || (command == PetCommand.DRINK && this.pet.getThirst() >= PetStat.THIRST.getMax())) {
            this.user().look(from, true);
            this.face("puz", 3);
            return;
        }

        if (!chance(this.obeyChance(owner, command) / 100.0)) {
            this.refuse(command, from);
            return;
        }

        if (owner) {
            this.gainFriendship(1);
        }

        this.pet.markAttention();
        this.dirty = true;
        this.lastObeyed = now();
        this.execute(command, speaker);
    }

    private double obeyChance(boolean owner, PetCommand command) {
        double friendship = this.pet.getFriendship();
        double chance = owner ? 55 + friendship * 0.4 : 30 + this.pet.getSociability() * 30 + friendship * 0.1;
        int happiness = this.pet.getHappiness();

        if (happiness <= 1) chance -= 20;
        else if (happiness <= 2) chance -= 10;

        if (command.isActive() && this.pet.getEnergy() <= 1) chance -= 30;
        if (this.pet.getHunger() <= 1 && command != PetCommand.EAT) chance -= 15;
        if (this.pet.isGrumpy()) chance -= 15;
        if (this.pet.getFriendlinessNature() >= 8) chance += 8;

        if (command == PetCommand.EAT || command == PetCommand.DRINK) chance += 30;
        if (command == PetCommand.NEST && this.pet.getEnergy() < PetStat.ENERGY.getMax()) chance += 20;

        return Math.max(10, Math.min(97, chance));
    }

    private void refuse(PetCommand command, Position from) {
        this.user().look(from, true);

        if (command.isActive() && this.pet.getEnergy() <= 1) {
            this.face("eyb", 3);
            this.pose(StatusType.LAY, seconds(8, 15) / 1000);
            return;
        }

        if (this.pet.isHostile() || this.pet.isGrumpy()) {
            this.face("agr", 3);
            this.say(PetTexts.Mood.ANGRY, 0.7);
        } else {
            this.face("puz", 3);
            this.say(PetTexts.Mood.GENERIC, 0.3);
        }
    }

    private void execute(PetCommand command, Player speaker) {
        Position from = speaker.getRoomUser().getPosition();
        this.stopActivity();

        switch (command) {
            case SIT:
                this.user().look(from, true);
                this.poseOrdered(StatusType.SIT, seconds(10, 30) / 1000);
                break;
            case LIE:
                this.user().look(from, true);
                this.poseOrdered(StatusType.LAY, seconds(10, 30) / 1000);
                break;
            case PLAY_DEAD:
                this.poseOrdered(StatusType.DEAD, seconds(4, 10) / 1000);
                break;
            case JUMP:
                this.poseOrdered(StatusType.JUMP, 2 + ThreadLocalRandom.current().nextInt(2));
                break;
            case BEG:
                this.user().look(from, true);
                this.poseOrdered(StatusType.BEG, seconds(6, 10) / 1000);
                this.sayNow(PetTexts.Mood.BEG);
                break;
            case VOICE:
                this.user().look(from, true);
                this.sayNow(PetTexts.Mood.GENERIC);
                if (chance(this.pet.getSociability())) this.wag(3);
                this.idle(now(), 3, 6);
                break;
            case COME_HERE:
                if (!this.goToPerson(speaker, Purpose.COME)) {
                    this.user().look(from, true);
                    this.face("puz", 3);
                    this.idle(now(), 3, 6);
                }
                break;
            case FOLLOW_ME:
                this.targetPlayer = speaker;
                this.timer = seconds(60, 120) / 1000;
                this.activity = Activity.FOLLOW;
                this.wag(4);
                break;
            case GO_AWAY:
                this.goAway(from);
                break;
            case NEST:
                this.goSleep(true);
                break;
            case EAT:
                if (!this.seek(Activity.GO_FOOD, true)) {
                    this.user().look(from, true);
                    this.face("sad", 4);
                    this.sayNow(PetTexts.Mood.BEG);
                    this.idle(now(), 3, 6);
                }
                break;
            case DRINK:
                if (!this.seek(Activity.GO_WATER, true) && !this.seekEmptyBowl(true)) {
                    this.user().look(from, true);
                    this.face("sad", 4);
                    this.idle(now(), 3, 6);
                }
                break;
            default:
                break;
        }
    }

    private void praise(boolean owner, Position from) {
        long now = now();

        if (this.activity == Activity.IDLE || this.activity == Activity.POSE || this.activity == Activity.WANDER) {
            this.user().look(from, true);
        }

        this.face("joy", 3);
        this.wag(5);
        this.say(chance(0.5) ? PetTexts.Mood.PLAY : PetTexts.Mood.GENERIC, 0.5);

        // Praise right after an obeyed command counts most.
        if (now >= this.nextPraise) {
            float gain = now - this.lastObeyed < 30_000 ? 3 : 1;
            this.pet.addFriendship(owner ? gain : gain / 2);
            this.pet.addLevel(PetStat.HAPPINESS, 1);
            this.pet.markAttention();
            this.nextPraise = now + 15_000;
            this.dirty = true;
        }
    }

    private void scold(Position from) {
        long now = now();

        if (this.activity == Activity.PLAYING) {
            this.finishPlaying(false);
        } else if (this.activity == Activity.EATING) {
            this.finishEating(false);
        } else if (this.activity == Activity.POSE) {
            this.endPose();
        }

        this.user().look(from, true);

        if (this.pet.isHostile() || this.pet.isGrumpy()) {
            this.face("agr", 4);
            this.say(PetTexts.Mood.ANGRY, 0.6);
        } else {
            this.face("sad", 5);
            this.say(PetTexts.Mood.SNIFF, 0.3);
        }

        if (now >= this.nextScold) {
            this.pet.addFriendship(-2);
            this.pet.addLevel(PetStat.HAPPINESS, -1);
            this.nextScold = now + 10_000;
            this.dirty = true;
        }

        this.idle(now, 4, 8);
    }

    private void goAway(Position from) {
        Position best = null;
        int bestDistance = 0;

        for (int i = 0; i < 20; i++) {
            Position tile = this.room.getMapping().getRandomWalkableBound(this.pet, false);

            if (tile != null && tile.getDistanceSquared(from) > bestDistance) {
                best = tile;
                bestDistance = tile.getDistanceSquared(from);
            }
        }

        if (best != null && bestDistance >= 3 && this.walk(best)) {
            this.activity = Activity.GO_AWAY;
            return;
        }

        this.face("puz", 3);
        this.idle(now(), 3, 6);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Food, water, toys, basket
    // ------------------------------------------------------------------------------------------------------------

    private boolean seek(Activity goal, boolean ordered) {
        Position position = this.user().getPosition();
        List<Item> items = new ArrayList<>();

        for (Item item : this.room.getItemManager().getFloorItems()) {
            if (this.isFor(goal, item) && (item.getTile() == null || item.getTile().getOtherEntities(this.pet).isEmpty())) {
                items.add(item);
            }
        }

        items.sort(Comparator.comparingInt(item -> item.getPosition().getDistanceSquared(position)));

        for (int i = 0; i < Math.min(3, items.size()); i++) {
            Item item = items.get(i);

            this.stopActivity();
            this.targetItem = item;
            this.ordered = ordered;
            this.retries = 0;
            this.activity = goal;

            if (position.getX() == item.getPosition().getX() && position.getY() == item.getPosition().getY()) {
                this.reachedItem(item);
                return true;
            }

            if (this.walk(item.getPosition())) {
                return true;
            }
        }

        this.targetItem = null;
        this.activity = Activity.IDLE;
        return false;
    }

    /**
     * Thirsty with only empty water bowls: go to the nearest and show it (at most every few minutes unless told to
     * drink), so whoever is in the room sees what is missing.
     */
    private boolean seekEmptyBowl(boolean ordered) {
        long now = now();

        if (!ordered && now < this.nextEmptyBowl) {
            return false;
        }

        Position position = this.user().getPosition();
        Item bowl = null;

        for (Item item : this.room.getItemManager().getFloorItems()) {
            if (item.getDefinition().getInteractionType() != InteractionType.PET_WATER_BOWL
                    || PetWaterBowlInteractor.level(item) > 0
                    || (item.getTile() != null && !item.getTile().getOtherEntities(this.pet).isEmpty())) {
                continue;
            }

            if (bowl == null || item.getPosition().getDistanceSquared(position) < bowl.getPosition().getDistanceSquared(position)) {
                bowl = item;
            }
        }

        if (bowl == null) {
            return false;
        }

        this.nextEmptyBowl = now + seconds(150, 300);
        this.stopActivity();
        this.targetItem = bowl;
        this.ordered = ordered;
        this.retries = 0;
        this.activity = Activity.GO_WATER;

        if (position.getX() == bowl.getPosition().getX() && position.getY() == bowl.getPosition().getY()) {
            this.reachedItem(bowl);
            return true;
        }

        if (this.walk(bowl.getPosition())) {
            return true;
        }

        this.targetItem = null;
        this.activity = Activity.IDLE;
        return false;
    }

    /** At an empty water bowl: sniff it and whine; the next decisions beg the owner. */
    private void foundEmptyBowl() {
        this.targetItem = null;
        this.pose(StatusType.SNIFF, seconds(3, 5) / 1000);
        this.face("sad", 6);
        this.say(PetTexts.Mood.BEG, 0.7);
    }

    private boolean isFor(Activity goal, Item item) {
        InteractionType type = item.getDefinition().getInteractionType();

        switch (goal) {
            case GO_FOOD:
                return type == InteractionType.PET_FOOD && this.canEat(item) && bowlMeals(item) < BOWL_MEALS;
            case GO_WATER:
                return type == InteractionType.PET_WATER_BOWL && PetWaterBowlInteractor.level(item) > 0;
            case GO_TOY:
                return type == InteractionType.PET_TOY;
            default:
                return false;
        }
    }

    /** Food for every pet (pet_food), or for this kind only (pet_dog_food, pet_cat_food, pet_croc_food). */
    private boolean canEat(Item item) {
        if (item.hasBehaviour(ItemBehaviour.PET_FOOD)) {
            return true;
        }

        PetType type = PetManager.getInstance().getType(this.pet);

        return (type == PetType.DOG && item.hasBehaviour(ItemBehaviour.PET_DOG_FOOD))
                || (type == PetType.CAT && item.hasBehaviour(ItemBehaviour.PET_CAT_FOOD))
                || (type == PetType.CROC && item.hasBehaviour(ItemBehaviour.PET_CROC_FOOD));
    }

    private static int bowlMeals(Item item) {
        String data = item.getCustomData();
        return StringUtils.isNumeric(data) && !data.isEmpty() ? Integer.parseInt(data) : 0;
    }

    private static boolean isTreat(Item item) {
        return item.getDefinition().getSprite().startsWith("goodie");
    }

    /**
     * The pet stopped on a pet item (the item interactors call this).
     */
    public synchronized void reachedItem(Item item) {
        if (this.passive) {
            return;
        }

        InteractionType type = item.getDefinition().getInteractionType();

        if (item == this.targetItem) {
            switch (this.activity) {
                case GO_FOOD: this.startEating(item); return;
                case GO_WATER:
                    if (PetWaterBowlInteractor.level(item) > 0) {
                        this.startDrinking(item);
                    } else {
                        this.foundEmptyBowl();
                    }
                    return;
                case GO_TOY: this.startPlaying(item); return;
                case GO_NEST: this.startSleeping(this.ordered); return;
                default: break;
            }
        }

        // Walked onto it on the way somewhere else.
        if (this.activity != Activity.IDLE && this.activity != Activity.WANDER && this.activity != Activity.GO_AWAY) {
            return;
        }

        if (type == InteractionType.PET_FOOD && this.canEat(item) && bowlMeals(item) < BOWL_MEALS
                && this.pet.getHunger() <= PetStat.HUNGER.getMax() - 2) {
            this.startEating(item);
        } else if (type == InteractionType.PET_WATER_BOWL && PetWaterBowlInteractor.level(item) > 0
                && this.pet.getThirst() < PetStat.THIRST.getMax()) {
            this.startDrinking(item);
        } else if (type == InteractionType.PET_TOY && this.pet.getHappiness() < PetStat.HAPPINESS.getMax()
                && this.pet.getEnergy() >= 2 && chance(0.5 + this.pet.getActivity() / 2)) {
            this.startPlaying(item);
        } else if (type == InteractionType.PET_NEST && item.getId() == this.pet.getDetails().getItemId()
                && (this.pet.getEnergy() <= 2 || isNight())) {
            this.startSleeping(false);
        }
    }

    /**
     * A pet item was picked up or moved: stop using it.
     */
    public synchronized void itemGone(Item item) {
        if (item != this.targetItem) {
            return;
        }

        switch (this.activity) {
            case EATING: this.finishEating(false); break;
            case DRINKING: this.finishDrinking(false); break;
            case PLAYING: this.toyState = null; this.finishPlaying(false); break;
            case GO_FOOD: case GO_WATER: case GO_TOY:
                this.user().stopWalking();
                this.idle(now(), 2, 4);
                break;
            default: break;
        }
    }

    /**
     * Food or water was put in the room, or a water bowl was filled: hungry and thirsty pets come.
     */
    public synchronized void foodPlaced() {
        if (this.activity == Activity.IDLE || this.activity == Activity.WANDER || this.activity == Activity.POSE && !this.ordered) {
            if (this.pet.getHunger() <= 3 || this.pet.getThirst() <= 1) {
                this.nextDecision = now() + seconds(1, 3);

                if (this.activity != Activity.IDLE) {
                    this.idle(now(), 1, 3);
                }
            }
        }
    }

    private void goalTick() {
        if (this.user().isWalking()) {
            return;
        }

        Item item = this.targetItem;
        Position position = this.user().getPosition();

        if (this.activity == Activity.GO_NEST) {
            Item nest = this.getNest();

            if (nest != null && position.getX() == nest.getPosition().getX() && position.getY() == nest.getPosition().getY()) {
                this.startSleeping(this.ordered);
                return;
            }

            // The basket can't be reached: curl up here.
            if (++this.retries > 2 || nest == null || !this.walk(nest.getPosition())) {
                this.startSleeping(this.ordered);
            }
            return;
        }

        if (item == null || item.getRoom() != this.room) {
            this.idle(now(), 2, 4);
            return;
        }

        if (position.getX() == item.getPosition().getX() && position.getY() == item.getPosition().getY()) {
            this.reachedItem(item);
            return;
        }

        if (++this.retries > 2 || !this.walk(item.getPosition())) {
            this.face("puz", 3);
            this.idle(now(), 3, 6);
        }
    }

    private void startEating(Item item) {
        this.clearPoses();
        this.activity = Activity.EATING;
        this.targetItem = item;
        this.timer = seconds(9, 14) / 1000;
        this.faceForBowl();
        this.user().setStatus(StatusType.EAT, "1");
        this.user().setNeedsUpdate(true);
        this.say(PetTexts.Mood.EAT, 0.8);
    }

    private void finishEating(boolean meal) {
        Item item = this.targetItem;

        this.user().removeStatus(StatusType.EAT);
        this.user().setNeedsUpdate(true);
        this.targetItem = null;
        this.activity = Activity.IDLE;

        if (meal && item != null && item.getRoom() == this.room) {
            boolean wasHungry = this.pet.getHunger() <= 3;

            if (isTreat(item)) {
                // A treat: one bite, mostly for the joy of it.
                this.pet.addLevel(PetStat.HUNGER, 1);
                this.pet.addLevel(PetStat.HAPPINESS, 2);
                this.removeItem(item);
            } else {
                // "Lavt Energiindhold" (petfood3) fills less than "Medium" (the other bowls).
                this.pet.addLevel(PetStat.HUNGER, item.getDefinition().getSprite().equals("petfood3") ? 2 : 3);

                if (wasHungry) {
                    this.pet.addLevel(PetStat.HAPPINESS, 1);
                }

                int meals = bowlMeals(item) + 1;

                if (meals >= BOWL_MEALS) {
                    this.removeItem(item);
                } else {
                    item.setCustomData(String.valueOf(meals));
                    item.updateStatus();
                    item.save();
                }
            }

            this.save();
            this.happy();
            this.say(PetTexts.Mood.EAT, 0.3);
        }

        this.idle(now(), 3, 6);
    }

    private void startDrinking(Item item) {
        this.clearPoses();
        this.activity = Activity.DRINKING;
        this.targetItem = item;
        this.timer = seconds(6, 10) / 1000;
        this.faceForBowl();
        this.user().setStatus(StatusType.EAT, "1");
        this.user().setNeedsUpdate(true);
    }

    private void finishDrinking(boolean drank) {
        Item item = this.targetItem;

        this.user().removeStatus(StatusType.EAT);
        this.user().setNeedsUpdate(true);
        this.targetItem = null;
        this.activity = Activity.IDLE;

        if (drank && item != null && item.getRoom() == this.room) {
            this.pet.setLevel(PetStat.THIRST, PetStat.THIRST.getMax());

            // Each drink takes one of the bowl's five levels; someone with rights fills it again (double-click).
            int level = PetWaterBowlInteractor.level(item);

            if (level > 0) {
                item.setCustomData(String.valueOf(level - 1));
                item.updateStatus();
                item.save();
            }

            this.save();
            this.happy();
        }

        this.idle(now(), 3, 6);
    }

    private void startPlaying(Item item) {
        this.clearPoses();
        this.activity = Activity.PLAYING;
        this.targetItem = item;
        this.timer = seconds(12, 20) / 1000;
        this.toyState = item.getCustomData();
        this.toyFrame = 0;
        this.faceForBowl();
        this.user().setStatus(StatusType.PLAY, "1");
        this.user().setNeedsUpdate(true);
        this.say(PetTexts.Mood.PLAY, 0.8);
    }

    /** The ball (toy1) has 6 frames of a turn: show them while the pet plays. */
    private void spinToy() {
        Item item = this.targetItem;

        if (item == null || item.getRoom() != this.room) {
            return;
        }

        this.toyFrame = (this.toyFrame + 1) % TOY_FRAMES;
        item.setCustomData(String.valueOf(this.toyFrame));
        item.updateStatus();
    }

    private void finishPlaying(boolean played) {
        Item item = this.targetItem;

        this.user().removeStatus(StatusType.PLAY);
        this.user().setNeedsUpdate(true);
        this.targetItem = null;
        this.activity = Activity.IDLE;

        if (item != null && item.getRoom() == this.room && this.toyState != null) {
            item.setCustomData(this.toyState);
            item.updateStatus();
        }

        this.toyState = null;

        if (played) {
            this.pet.setLevel(PetStat.HAPPINESS, PetStat.HAPPINESS.getMax());
            this.pet.addLevel(PetStat.ENERGY, -1);
            this.save();
            this.happy();

            if (this.pet.getActivity() >= 0.6 && chance(0.4)) {
                this.pose(StatusType.JUMP, 2);
                return;
            }
        }

        this.idle(now(), 3, 6);
    }

    private void removeItem(Item item) {
        this.room.getMapping().removeItem(item);
        item.delete();
    }

    private Item getNest() {
        Item nest = this.room.getItemManager().getById(this.pet.getDetails().getItemId());
        return nest != null && nest.getRoom() == this.room ? nest : null;
    }

    private void goSleep(boolean ordered) {
        Item nest = this.getNest();
        Position position = this.user().getPosition();

        this.stopActivity();
        this.ordered = ordered;
        this.retries = 0;

        if (nest == null || (position.getX() == nest.getPosition().getX() && position.getY() == nest.getPosition().getY())) {
            this.startSleeping(ordered);
            return;
        }

        if (this.walk(nest.getPosition())) {
            this.activity = Activity.GO_NEST;
            return;
        }

        this.startSleeping(ordered);
    }

    private void startSleeping(boolean ordered) {
        this.clearPoses();
        this.evenRotation();
        this.activity = Activity.SLEEPING;
        this.recoverCounter = 0;
        // Told to sleep while rested: a short nap.
        this.minSleepUntil = now() + (ordered ? seconds(40, 70) : seconds(60, 120));
        this.user().setStatus(StatusType.PET_SLEEP, StringUtil.format(this.user().getPosition().getZ()));
        this.user().setNeedsUpdate(true);
        this.nextSpeech = Math.max(this.nextSpeech, now() + seconds(5, 15));
        this.say(PetTexts.Mood.SLEEP, 0.5);
    }

    private void sleepTick(long now) {
        if (++this.recoverCounter >= Math.max(10, GameConfiguration.getInstance().getInteger("pet.energy.recover.seconds"))) {
            this.recoverCounter = 0;

            if (this.pet.getEnergy() < PetStat.ENERGY.getMax()) {
                this.pet.addLevel(PetStat.ENERGY, 1);
                this.dirty = true;
            }
        }

        if (chance(1.0 / 60)) {
            this.say(PetTexts.Mood.SLEEP, 1);
        }

        if (now >= this.minSleepUntil && this.pet.getEnergy() >= PetStat.ENERGY.getMax() && !isNight()) {
            this.wakeUp(true);
        }
    }

    private void wakeUp(boolean stretch) {
        this.user().removeStatus(StatusType.PET_SLEEP);
        this.user().setNeedsUpdate(true);
        this.activity = Activity.IDLE;
        this.save();

        if (stretch) {
            this.pose(chance(0.6) ? StatusType.STRETCH : StatusType.SCRATCH, 3);
        } else {
            this.idle(now(), 2, 4);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Poses, faces, speech
    // ------------------------------------------------------------------------------------------------------------

    private void pose(StatusType type, int seconds) {
        this.ordered = false;
        this.setPose(type, seconds);
    }

    private void poseOrdered(StatusType type, int seconds) {
        this.ordered = true;
        this.setPose(type, seconds);
    }

    private void setPose(StatusType type, int seconds) {
        RoomPet user = this.user();

        if (user.isWalking()) {
            user.stopWalking();
        }

        this.clearPoses();
        seconds = Math.max(1, seconds);

        String value = "";

        switch (type) {
            case SIT:
            case LAY:
            case PET_SLEEP:
                this.evenRotation();
                value = String.valueOf(StringUtil.format(user.getPosition().getZ()));
                break;
            case BEG:
            case DEAD:
                this.evenRotation();
                value = type.getStatusCode();
                break;
            case JUMP:
                value = type.getStatusCode();
                break;
            default:
                break;
        }

        user.setStatus(type, value, seconds + 1, null, -1, -1);
        user.setNeedsUpdate(true);

        this.activity = Activity.POSE;
        this.timer = seconds;
    }

    private void endPose() {
        this.clearPoses();
        this.ordered = false;
        this.idle(now(), 1, 4);
    }

    private void face(String face, int seconds) {
        this.user().setStatus(StatusType.PET_GESTURE, face, seconds, null, -1, -1);
        this.user().setNeedsUpdate(true);
    }

    private void wag(int seconds) {
        this.user().setStatus(StatusType.WAG, "", seconds, null, -1, -1);
        this.user().setNeedsUpdate(true);
    }

    private void happy() {
        this.face("sml", 4);
        this.wag(4);
    }

    /**
     * The client has the eating head (p_eat_hd_*) and the playing body (p_pla_bd_*) only for direction 2, which it
     * mirrors for 4: in any other direction the pet's head or body isn't drawn. Turn to whichever is nearer.
     */
    private void faceForBowl() {
        Position position = this.user().getPosition();
        int rotation = position.getBodyRotation();
        int toTwo = Math.min(Math.floorMod(rotation - 2, 8), Math.floorMod(2 - rotation, 8));
        int toFour = Math.min(Math.floorMod(rotation - 4, 8), Math.floorMod(4 - rotation, 8));
        int facing = toTwo < toFour ? 2 : toFour < toTwo ? 4 : (chance(0.5) ? 2 : 4);

        position.setRotation(facing);
        this.user().setNeedsUpdate(true);
    }

    /** Sit and lie poses exist for the straight directions. */
    private void evenRotation() {
        Position position = this.user().getPosition();
        int rotation = position.getBodyRotation() / 2 * 2;
        position.setRotation(rotation);
    }

    public void clearPoses() {
        for (StatusType type : POSES) {
            this.user().removeStatus(type);
        }

        this.user().setNeedsUpdate(true);
    }

    private void say(PetTexts.Mood mood, double probability) {
        long now = now();

        if (now < this.nextSpeech || !chance(probability)) {
            return;
        }

        this.talk(mood, now);
    }

    /** Answer a command: only a short pause since the last line. */
    private void sayNow(PetTexts.Mood mood) {
        long now = now();

        if (now < this.nextSpeech - 6_000) {
            return;
        }

        this.talk(mood, now);
    }

    private void talk(PetTexts.Mood mood, long now) {
        String text = PetTexts.getInstance().getSaying(mood, this.pet.getDetails().getType());

        if (text == null) {
            return;
        }

        this.nextSpeech = now + seconds(7, 14);
        this.user().talk(text, CHAT_MESSAGE.ChatMessageType.CHAT);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------

    /** Stop whatever the pet was doing (before a command, or before heading somewhere). */
    private void stopActivity() {
        switch (this.activity) {
            case EATING: this.finishEating(false); break;
            case DRINKING: this.finishDrinking(false); break;
            case PLAYING: this.finishPlaying(false); break;
            case SLEEPING: this.user().removeStatus(StatusType.PET_SLEEP); break;
            default: break;
        }

        if (this.user().isWalking()) {
            this.user().stopWalking();
        }

        this.clearPoses();
        this.targetItem = null;
        this.targetPlayer = null;
        this.ordered = false;
        this.activity = Activity.IDLE;
    }

    private void idle(long now, int minSeconds, int maxSeconds) {
        this.activity = Activity.IDLE;
        this.targetItem = null;
        this.targetPlayer = null;
        this.nextDecision = now + seconds(minSeconds, maxSeconds);
    }

    private boolean walk(Position position) {
        this.clearPoses();
        return this.user().walkTo(position.getX(), position.getY());
    }

    /** A free tile next to someone, the one nearest the pet first. */
    private Position tileNextTo(Position target) {
        Position door = this.room.getModel().getDoorLocation();
        Position from = this.user().getPosition();
        Position best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }

                Position tile = new Position(target.getX() + dx, target.getY() + dy);

                if (tile.equals(door)) {
                    continue;
                }

                if (tile.getX() == from.getX() && tile.getY() == from.getY()) {
                    return tile;
                }

                if (!RoomTile.isValidTile(this.room, this.pet, tile)) {
                    continue;
                }

                int distance = tile.getDistanceSquared(from);

                if (distance < bestDistance) {
                    best = tile;
                    bestDistance = distance;
                }
            }
        }

        return best;
    }

    /** A free tile within a few steps. */
    private Position tileNear(int radius) {
        Position from = this.user().getPosition();
        Position door = this.room.getModel().getDoorLocation();

        for (int i = 0; i < 12; i++) {
            int dx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int dy = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            if (dx == 0 && dy == 0) {
                continue;
            }

            Position tile = new Position(from.getX() + dx, from.getY() + dy);

            if (!tile.equals(door) && RoomTile.isValidTile(this.room, this.pet, tile)) {
                return tile;
            }
        }

        return this.room.getMapping().getRandomWalkableBound(this.pet, false);
    }

    private Player getOwner() {
        for (Player player : this.room.getEntityManager().getPlayers()) {
            if (player.getDetails().getId() == this.pet.getOwnerId() && !RoomEntityManager.isHidden(player)) {
                return player;
            }
        }

        return null;
    }

    private void gainFriendship(float amount) {
        long now = now();

        if (now < this.nextFriendshipGain) {
            return;
        }

        this.pet.addFriendship(amount);
        this.pet.markAttention();
        this.nextFriendshipGain = now + 20_000;
        this.dirty = true;
    }

    private void save() {
        this.pet.saveDetails();
        this.dirty = false;
        this.nextSave = now() + 60_000;
    }

    private int distance(Player player) {
        return player.getRoomUser().getPosition().getDistanceSquared(this.user().getPosition());
    }

    private RoomPet user() {
        return this.pet.getRoomUser();
    }

    /** Hotel time between "pet.night.start.hour" and "pet.night.end.hour" (23 and 7). */
    public static boolean isNight() {
        int start = GameConfiguration.getInstance().getInteger("pet.night.start.hour");
        int end = GameConfiguration.getInstance().getInteger("pet.night.end.hour");
        int hour = LocalTime.now().getHour();

        if (start == end) {
            return false;
        }

        return start < end ? hour >= start && hour < end : hour >= start || hour < end;
    }

    private static int pick(double[] weights) {
        double total = 0;

        for (double weight : weights) {
            total += Math.max(0, weight);
        }

        double roll = ThreadLocalRandom.current().nextDouble() * total;

        for (int i = 0; i < weights.length; i++) {
            roll -= Math.max(0, weights[i]);

            if (roll < 0) {
                return i;
            }
        }

        return weights.length - 1;
    }

    private static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /** Milliseconds, random between two numbers of seconds. */
    private static int seconds(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min * 1000, max * 1000 + 1);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Staff: the pet's "Control" window (fuse_debug_window, PetControlCommand)
    // ------------------------------------------------------------------------------------------------------------

    public synchronized void setPassive(boolean passive) {
        if (passive) {
            this.stopActivity();
        } else {
            this.idle(now(), 1, 3);
        }

        this.passive = passive;
    }

    public boolean isPassive() {
        return passive;
    }

    public Activity getActivity() {
        return activity;
    }
}
