package agents;

public class cell {
    float grass;
    int sheeps;
    boolean visited;
    int turn;

    public cell() {
        this.grass = this.sheeps = 0;
        this.visited = false;
        this.turn = -1;
    }


    public float getGrass() {
        return grass;
    }

    public void setGrass(float grass) {
        this.grass = grass;
    }

    public int getSheeps() {
        return sheeps;
    }

    public int getTurn() {
        return turn;
    }

    public void setSheeps(int sheeps) {
        this.sheeps = sheeps;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean hasSheep() {
        return this.sheeps > 0;
    }

    public boolean hasGrass(){
        return this.grass > 0;
    }

}