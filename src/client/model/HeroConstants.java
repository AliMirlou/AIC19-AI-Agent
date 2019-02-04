package client.model;

public class HeroConstants
{
    private HeroType type;
    private AbilityName[] abilityNames;
    private int maxHP;
    private int moveAPCost;
    private int respawnTime;

    public HeroType getType()
    {
        return type;
    }


    void setType(HeroType type)
    {
        this.type = type;
    }

    public AbilityName[] getAbilityNames()
    {
        return abilityNames;
    }

    void setAbilityNames(AbilityName[] abilityNames)
    {
        this.abilityNames = abilityNames;
    }

    public int getMaxHP()
    {
        return maxHP;
    }

    void setMaxHP(int maxHP)
    {
        this.maxHP = maxHP;
    }

    public int getMoveAPCost()
    {
        return moveAPCost;
    }

    void setMoveAPCost(int moveAPCost)
    {
        this.moveAPCost = moveAPCost;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    void setRespawnTime(int respawnTime) {
        this.respawnTime = respawnTime;
    }
}
