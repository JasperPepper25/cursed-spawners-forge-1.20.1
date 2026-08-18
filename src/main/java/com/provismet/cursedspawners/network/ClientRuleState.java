package com.provismet.cursedspawners.network;

public final class ClientRuleState {
    private ClientRuleState() {}
    private static volatile double miningSpeedModifier = 1.0D;

    public static double miningSpeedModifier() { return miningSpeedModifier; }
    public static void setMiningSpeedModifier(double value) { miningSpeedModifier = value; }
}
