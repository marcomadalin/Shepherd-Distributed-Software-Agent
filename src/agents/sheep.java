package agents;

public class sheep {
    String id;
    String owner;
    int x;
    int y;
    int turn;
    static float woolGrowth = -1;
    static float maxWool = -1;
    float wool;
    boolean alive;
    boolean transported;

    public sheep(Integer[] position,float wools, String ids, String owners, boolean alives, boolean transporteds, int turn) {
        x = position[0];
        y = position[1];
        id = ids;
        owner = owners;
        alive = alives;
        transported = transporteds;
        wool = wools;
        this.turn = turn;
    }

    public Integer[] getPosition(){
        Integer[] pos = new Integer[2];
        pos[0] = x;
        pos[1] = y;
        return pos;
    }

    public float getWool() {
        return wool;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public boolean isAlive() {
        return alive;
    }

    public boolean isTransported() {
        return this.transported;
    }

    public String getId() {
        return id;
    }

    public int getTrun() {
        return this.turn;
    }

    public String getOwner() {
        return owner;
    }


    public void setWoolGrowth(float speed){
        woolGrowth = speed;
    }

    public void setMaxWool(float max) {
        maxWool = max;
    }

    public void update() {

        if (woolGrowth != -1 && maxWool != -1) wool = Math.min(maxWool,wool*woolGrowth);
    }

}