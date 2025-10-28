package ua.xlany.gradientpillars.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Arena {

    private final String name;
    private String worldName;
    private Location lobby;
    private Location spectator;
    private List<Location> pillars;
    private int minPlayers;
    private int maxPlayers;

    public Arena(String name) {
        this.name = name;
        this.pillars = new ArrayList<>();
        this.minPlayers = 2; // За замовчуванням
        this.maxPlayers = 16; // За замовчуванням
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location getLobby() {
        return lobby;
    }

    public void setLobby(Location lobby) {
        this.lobby = lobby;
        if (worldName == null && lobby != null) {
            worldName = lobby.getWorld().getName();
        }
    }

    public Location getSpectator() {
        return spectator;
    }

    public void setSpectator(Location spectator) {
        this.spectator = spectator;
    }

    public List<Location> getPillars() {
        return new ArrayList<>(pillars);
    }

    public void addPillar(Location pillar) {
        pillars.add(pillar);
    }

    public void setPillar(int index, Location pillar) {
        // Розширити список якщо потрібно
        while (pillars.size() <= index) {
            pillars.add(null);
        }
        pillars.set(index, pillar);
    }

    public void removePillar(int index) {
        if (index >= 0 && index < pillars.size()) {
            pillars.remove(index);
        }
    }

    public void clearPillars() {
        pillars.clear();
    }

    public boolean isSetup() {
        return worldName != null && lobby != null && spectator != null && !pillars.isEmpty()
                && pillars.stream().anyMatch(p -> p != null) && minPlayers > 0 && maxPlayers >= minPlayers;
    }

    public int getPillarCount() {
        return (int) pillars.stream().filter(p -> p != null).count();
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void rebindWorld(World world) {
        if (world == null) {
            return;
        }

        this.worldName = world.getName();

        if (lobby != null) {
            lobby.setWorld(world);
        }

        if (spectator != null) {
            spectator.setWorld(world);
        }

        for (int i = 0; i < pillars.size(); i++) {
            Location pillar = pillars.get(i);
            if (pillar != null) {
                pillar.setWorld(world);
                pillars.set(i, pillar);
            }
        }
    }
}
