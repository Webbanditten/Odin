package org.alexdev.kepler.game.room.entities;

import org.alexdev.kepler.dao.mysql.PetDao;
import org.alexdev.kepler.game.GameScheduler;
import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.item.Item;
import org.alexdev.kepler.game.item.base.ItemBehaviour;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.pets.Pet;
import org.alexdev.kepler.game.pets.PetAction;
import org.alexdev.kepler.game.pets.PetManager;
import org.alexdev.kepler.game.pets.PetType;
import org.alexdev.kepler.game.room.enums.StatusType;
import org.alexdev.kepler.messages.outgoing.rooms.items.STUFFDATAUPDATE;
import org.alexdev.kepler.util.DateUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.alexdev.kepler.util.StringUtil.tryParseInt;

public class RoomPet extends RoomEntity {
    private Pet pet;
    private Item item;

    public RoomPet(Entity entity) {
        super(entity);
        this.pet = (Pet) entity;
        this.item = null;
    }

    /**
     * Triggers the current item that the player has walked on top of.
     */
    @Override
    public void invokeItem(Position oldPosition) {
        super.invokeItem(oldPosition);
        this.pet.setWalkBeforeSitLay(false);
    }



    public void tryDrinking() {
        if (this.getRoom() == null) {
            return;
        }

        var room = this.getRoom();
        var bowlsInRoom = room.getItemManager().getFloorItems().stream().filter(item -> item.hasBehaviour(ItemBehaviour.PET_WATER_BOWL) &&
                tryParseInt(item.getCustomData(), 0) > 0).collect(Collectors.toList());

        if (bowlsInRoom.size() < 1) {
            return;
        }

        Collections.shuffle(bowlsInRoom);
        Item item = bowlsInRoom.get(0);

        if (item == null) {
            return;
        }

        this.pet.getRoomUser().walkTo(item.getPosition().getX(), item.getPosition().getY());

        if (!this.pet.getRoomUser().isWalking()) {
            return;
        }

        this.pet.setAction(PetAction.DRINK);
        this.pet.setActionDuration(ThreadLocalRandom.current().nextInt(30));
        this.item = item;
    }

    public void tryPlay() {
        if (this.getRoom() == null) {
            return;
        }

        var room = this.getRoom();
        var toysInRoom = room.getItemManager().getFloorItems().stream().filter(item -> item.hasBehaviour(ItemBehaviour.PET_TOY)).collect(Collectors.toList());


        Collections.shuffle(toysInRoom);
        Item item = toysInRoom.get(0);

        if (item == null) {
            return;
        }

        this.pet.getRoomUser().walkTo(item.getPosition().getX(), item.getPosition().getY());

        if (!this.pet.getRoomUser().isWalking()) {
            return;
        }

        this.pet.setAction(PetAction.PLAY);
        this.pet.setActionDuration(ThreadLocalRandom.current().nextInt(30));
        this.item = item;
    }

    public void tryEating() {
        if (this.getRoom() == null) {
            return;
        }

        var room = this.getRoom();
        var petType = PetManager.getInstance().getType(this.pet);

        List<Item> foodInRoom = null;

        if (petType == PetType.DOG) {
            foodInRoom = room.getItemManager().getFloorItems().stream().filter(item -> item.hasBehaviour(ItemBehaviour.PET_FOOD) || item.hasBehaviour(ItemBehaviour.PET_DOG_FOOD)).collect(Collectors.toList());
        }

        if (petType == PetType.CAT) {
            foodInRoom = room.getItemManager().getFloorItems().stream().filter(item -> item.hasBehaviour(ItemBehaviour.PET_FOOD) || item.hasBehaviour(ItemBehaviour.PET_CAT_FOOD)).collect(Collectors.toList());
        }

        if (petType == PetType.CROC) {
            foodInRoom = room.getItemManager().getFloorItems().stream().filter(item -> item.hasBehaviour(ItemBehaviour.PET_FOOD) || item.hasBehaviour(ItemBehaviour.PET_CROC_FOOD)).collect(Collectors.toList());
        }

        if (foodInRoom.size() < 1) {
            return;
        }

        Collections.shuffle(foodInRoom);
        Item item = foodInRoom.get(0);

        if (item == null) {
            return;
        }

        this.pet.getRoomUser().walkTo(item.getPosition().getX(), item.getPosition().getY());

        if (!this.pet.getRoomUser().isWalking()) {
            return;
        }

        this.pet.setAction(PetAction.EAT);
        this.pet.setActionDuration(ThreadLocalRandom.current().nextInt(30));
        this.item = item;
    }

    @Override
    public void stopWalking() {
        super.stopWalking();

        if (this.item.getRoom() != null) {
            this.getPosition().setRotation(this.item.getPosition().getRotation());

            switch(this.pet.getAction()) {
                case PLAY:
                    this.setStatus(StatusType.PLAY, "");
                    this.setNeedsUpdate(true);
                    playWithToy(this.pet);

                    //this.pet.getDetails().setLastPlayToy(DateUtil.getCurrentTimeSeconds());
                    //PetDao.saveDetails(this.pet.getDetails().getId(), this.pet.getDetails());
                    break;
                case DRINK:
                    this.setStatus(StatusType.EAT, "");
                    this.setNeedsUpdate(true);
                    this.decreaseWaterInBowl(this.pet);

                    this.pet.getDetails().setLastDrink(DateUtil.getCurrentTimeSeconds());
                    PetDao.saveDetails(this.pet.getDetails().getId(), this.pet.getDetails());
                    break;
                case EAT:
                    this.setStatus(StatusType.EAT, "");
                    this.setNeedsUpdate(true);
                    removeFoodItem(this.pet);

                    this.pet.getDetails().setLastEat(DateUtil.getCurrentTimeSeconds());
                    PetDao.saveDetails(this.pet.getDetails().getId(), this.pet.getDetails());
                    break;
            }
        } else {
            this.pet.setAction(PetAction.NONE);
            this.pet.setActionDuration(0);
        }
    }

    private void decreaseWaterInBowl(final Pet pet) {
        GameScheduler.getInstance().getService().schedule(()-> {
            if (item.getRoom() != null) {
                int customDataInt = tryParseInt(item.getCustomData(), 0);
                if(customDataInt > 0) {
                    customDataInt--;
                    item.setCustomData(Integer.toString(customDataInt));
                }
                item.updateStatus();
                item.save();
            }

            pet.setAction(PetAction.NONE);
            pet.setActionDuration(0);

            removeStatus(StatusType.EAT);
            setNeedsUpdate(true);
        }, 5, TimeUnit.SECONDS);
    }

    private void playWithToy(final Pet pet) {
        if (item.getRoom() != null) {
            item.setCustomData("1");
            item.updateStatus();
            item.save();
        }
        GameScheduler.getInstance().getService().schedule(()-> {
            pet.setAction(PetAction.NONE);
            pet.setActionDuration(0);

            removeStatus(StatusType.PLAY);
            setNeedsUpdate(true);
        }, 5, TimeUnit.SECONDS);
    }


    private void emptyPetBowl(final Pet pet) {
        GameScheduler.getInstance().getService().schedule(()-> {
            if (item.getRoom() != null) {
                item.setCustomData("0");
                item.updateStatus();
                item.save();
            }

            pet.setAction(PetAction.NONE);
            pet.setActionDuration(0);

            removeStatus(StatusType.EAT);
            setNeedsUpdate(true);
        }, 5, TimeUnit.SECONDS);
    }

    private void removeFoodItem(final Pet pet) {
        GameScheduler.getInstance().getService().schedule(()-> {
            if (item.getRoom() != null) {
                getRoom().getMapping().removeItem(null, item);
                item.delete();
            }

            pet.setAction(PetAction.NONE);
            pet.setActionDuration(0);

            removeStatus(StatusType.EAT);
            setNeedsUpdate(true);
        }, 5, TimeUnit.SECONDS);
    }
}
