package agents;

public class merchant {
    int x;
    int y;
    float price;
    boolean buyer;

    public merchant(Integer[] position, float prices, boolean isBuyer){
        x = position[0];
        y = position[1];
        price = prices;
        buyer = isBuyer;
    }
    public float getPrice() {
        return price;
    }

    public Integer[] getPosition(){
        Integer[] pos = new Integer[2];
        pos[0] = x;
        pos[1] = y;
        return pos;
    }

}
