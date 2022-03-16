package agents;

import java.util.ArrayList;

public class map {
    ArrayList<ArrayList<cell>> cells;

    public map() {
        cells = new ArrayList<>();
        cells.add(new ArrayList<>());
    }


    public Integer[] getNextNotVisited(Integer[] posActual) {
        int distance = -1;
        Integer[] position = new Integer[]{0, 0};
        for (int i = 0; i < cells.size(); ++i) {
            for (int j = 0; j < cells.get(i).size(); ++j) {
                int actDist = getDistance(j, i, posActual);
                if (!cells.get(i).get(j).isVisited() && actDist > distance) {
                    distance = actDist;
                    position[0] = j;
                    position[1] = i;
                }
            }
        }
        if (distance == -1) {
            int turn = -1;
            for (int i = 0; i < cells.size(); ++i) {
                for (int j = 0; j < cells.get(i).size(); ++j) {
                    int actDist = getDistance(j, i, posActual);
                    if (turn == -1) {
                        distance = actDist;
                        position[0] = j;
                        position[1] = i;
                        turn = cells.get(i).get(j).getTurn();
                    }
                    else if (cells.get(i).get(j).getTurn() < turn && actDist > distance) {
                        distance = actDist;
                        position[0] = j;
                        position[1] = i;
                        turn = cells.get(i).get(j).getTurn();
                    }
                }
            }
        }
        return position;
    }

    public Integer[] bestGrassCell(Integer[] pos) {
        Integer[] actual = new Integer[2];
        actual[0] = -1;
        actual[1] = -1;
        float grass = -1;
        float distance = -1;
        for (int i = 0 ; i < cells.size(); i++){
            for (int j = 0; j < cells.get(i).size(); ++j) {
                cell c = cells.get(i).get(j);
                if (grass == -1) {
                    grass = c.getGrass();
                    distance = getDistance(j, i, pos);
                    actual[0] = j;
                    actual[1] = i;
                }
                if (!c.hasSheep() && c.hasGrass() && (c.getGrass() > grass || (c.getGrass() == grass && getDistance(j, i, pos) < distance))) {
                    grass = c.getGrass();
                    distance = getDistance(j, i, pos);
                    actual[0] = j;
                    actual[1] = i;
                }
            }
        }
        return actual;
    }

    public int getFertileCells() {
        int numCells = 0;
        for (int i = 0; i < cells.size(); ++i) {
            for (int j = 0; j < cells.get(i).size(); ++j) {
                if (cells.get(i).get(j).hasGrass()) ++numCells;
            }
        }
        return numCells;
    }

    public int getDistance(int x, int y, Integer[] position) {
        return Math.abs(x-position[0]) + Math.abs(y-position[1]);
    }


    public boolean[] cellInMap(int x, int y) {
        if (y < 0 || x < 0) return new boolean[]{false,false};
        else if (y >= cells.size() || x >= cells.get(y).size()) return new boolean[]{true,false};
        else return new boolean[]{true,true};
    }

    public cell getCell(int x, int y, boolean update) {
        boolean[] valid = cellInMap(x,y);
        if (valid[0] && valid[1]) return cells.get(y).get(x);
        else if (update && valid[0]) {
            setCell(x, y, new cell());
            return cells.get(y).get(x);
        }
        else return new cell();
    }

    public void setCell(int x, int y, cell c) {
        if (y >= cells.size()) {
            int size = cells.size();
            int n = y + 1 - size;
            for (int i = 0; i < n; ++i)  cells.add(new ArrayList<>());
        }

        if (x >= cells.get(y).size()) {
            int max = 0;
            for (ArrayList<cell> cell : cells) {
                if (max < cell.size()) max = cell.size();
            }
            if (max < x+1) max = x+1;
            for (ArrayList<cell> cell : cells) {
                int n = max - cell.size();
                for (int j = 0; j < n; ++j) cell.add(new cell());
            }
        }
        cells.get(y).set(x, c);
    }

}

