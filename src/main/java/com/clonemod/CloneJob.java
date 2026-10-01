package com.clonemod;

public enum CloneJob {
    FOLLOW("Following you"),
    STAY("Standing still"),
    WOODCUTTER("Cutting wood"),
    MINER("Mining ore"),
    FARMER("Farming crops"),
    GUARD("Guarding");

    public final String label;

    CloneJob(String label) {
        this.label = label;
    }

    public CloneJob next() {
        CloneJob[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static CloneJob byId(int id) {
        CloneJob[] all = values();
        return all[Math.floorMod(id, all.length)];
    }
}
