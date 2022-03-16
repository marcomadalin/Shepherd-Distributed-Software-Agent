package agents;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.Math.abs;

public class agent extends Agent {
    int turn = 0;
    float wool = 0;
    float money = 0;

    boolean firstBuy = true;
    static int maxBuy = 20;
    static int actBuy = 0;

    AID[] shepherds = new AID[]{};
    AID[] environment= new AID[]{};

    ArrayList<String> Lie = new ArrayList<>();
    ArrayList<String> dontLie = new ArrayList<>();

    ArrayList<merchant> sellers = new ArrayList<>();
    ArrayList<merchant> buyers = new ArrayList<>();
    ArrayList<sheep> sheeps = new ArrayList<>();
    ArrayList<sheep> allSheeps = new ArrayList<>();

    Integer[] posActual;
    Integer[] destination;

    agents.map map = new map();

    int nextAction = 0;

    public void setup() {
        registerDF("my-agent");
        this.addBehaviour(new findEnvironment());
        shepherds = searchAgents("shepherd");
    }

    protected void takeDown() {
        deregisterDF();
    }

    class findEnvironment extends CyclicBehaviour {
        public void action() {
            environment = searchAgents("environment");
            if (environment != null) {
                agent.this.addBehaviour(new parser());
                agent.this.removeBehaviour(this);
            }
        }
    }

    public void nextMove() {
        map.getCell(posActual[0], posActual[1],true).setVisited(true);
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environment[0]);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        msg.setLanguage("fipa-sl");

        updateSheeps();
        String content = getNextMove();
        allSheeps.clear();

        System.out.println("TURN = " + turn);
        System.out.println("MONEY = " + money);
        System.out.println("WOOl = " + wool);
        System.out.println("X = " + posActual[0] + ", Y = " + posActual[1]);
        System.out.println("SHEEPS = " + sheeps.size());
        System.out.println("MOVE = " + content);
        System.out.println();
        msg.setContent(content);
        agent.this.addBehaviour(new infromEnvironment(agent.this, msg));
    }

    private String getNextMove() {
        String content = "";
        merchant[] m = new merchant[]{getClosestMerchant(buyers, false), getClosestMerchant(sellers, true)};
        System.out.println("ACTION = " + nextAction);
        switch (nextAction) {
            case 0: {
                content = moveToMerchant(true, m[1]);
                break;
            }
            case 1: {
                content = doTransaction(true, m[1]);
                ++actBuy;
                break;
            }
            case 2: {
                content = moveSheep();
                break;
            }
            case 3: {
                if (holdingSheeps()) {
                    nextAction = 2;
                    content = getNextMove();
                }
                else nextAction = 5;
                break;
            }
            case 4: {
                content = explore();
                nextAction = 4;
                break;
            }
            case 5: {
                sheep s = getNearUnshearedSheep();
                content = move(s.getX(), s.getY());
                if(content == "") content = "(shear-sheep :id \""+s.getId()+"\")";
                if(wool < sheeps.size()*10) nextAction = 5;
                else nextAction = 6;
                break;
            }
            case 6: {
                content = moveToMerchant(false, m[0]);
                break;
            }
            case 7: {
                content = doTransaction(false, m[0]);
                break;
            }
        }
        return content;
    }

    private sheep getNearUnshearedSheep() {
        float bestRatio = 0.0f;
        sheep s = sheeps.get(0);
        for(agents.sheep sheep : sheeps) {
            int distance = Math.abs(posActual[0] - sheep.getX()) + Math.abs(posActual[1] - sheep.getY());
            float ratio = sheep.getWool()/(distance+1);
            if(ratio > bestRatio) {
                bestRatio = ratio;
                s = sheep;
            }
        }
        return s;
    }

    private boolean enoughMoney(merchant m, int inc) {
        return (m.getPrice() < money *(1.0 - ((turn+inc)*0.01/100)))  && sheeps.size() <  map.getFertileCells() && (firstBuy || actBuy < maxBuy);
    }

    private boolean holdingSheeps() {
        for (agents.sheep sheep : sheeps) {
            if (sheep.getPosition()[0] == -1 && sheep.isAlive()) return true;
        }
        return false;
    }

    private boolean atDestination() {
        return posActual[0].equals(destination[0]) && posActual[1].equals(destination[1]);
    }

    private String moveSheep() {
        String content;
        if (destination == null) destination = map.bestGrassCell(posActual);
        if (destination[0] == -1) {
            nextAction = 5;
            content = getNextMove();
        }
        else {
            content = move(destination[0], destination[1]);
            if (content.equals("")) {
                content = "(drop-sheep :id \"" + getSheepID(-1, -1) + "\")";
                destination = null;
                nextAction = 3;
            } else nextAction = 2;
        }
        return content;
    }

    private String moveToMerchant(boolean buying, merchant m) {
        if (m != null) destination = m.getPosition();
        if (destination == null) {
            if (m == null) destination = map.getNextNotVisited(posActual);
        }
        else {
            if (m == null && atDestination()) destination = map.getNextNotVisited(posActual);
        }
        String content = move(destination[0],destination[1]);
        if (content.equals("")) {
            content = doTransaction(buying, m);
            destination = null;
            if(buying)nextAction = 1;
            else nextAction = 7;
        }
        else if(buying) nextAction = 0;
        else nextAction = 6;
        return content;
    }

    private String doTransaction(boolean buying, merchant m) {
        String content;
        if (buying) {
            content = "(buy-sheep)";
            money -= m.getPrice();
            if (enoughMoney(m,1)) nextAction = 1;
            else {
                nextAction = 2;
                actBuy = 0;
                firstBuy = false;
            }
        }
        else {
            content = "(sell-wool)";
            nextAction = 0;
        }
        return content;
    }

    private String getSheepID(int x, int y) {
        return sheeps.stream().filter(sheep -> sheep.getPosition()[0] == x && sheep.getPosition()[1] == y && sheep.isAlive()).findFirst().map(sheep::getId).orElse("none");
    }

    private void updateSheeps() {
        for (agents.sheep sheep : allSheeps) {
            int x = sheep.getPosition()[0];
            int y = sheep.getPosition()[1];
            map.getCell(x, y, false).setSheeps(map.getCell(x, y, false).getSheeps() + 1);
        }
        allSheeps.clear();

        ArrayList<sheep> aliveSheeps = new ArrayList<>();
        sheeps.stream().filter(sheep::isAlive).forEach(sh -> {
            if (!sh.isTransported() && sh.getTrun() != turn -1) sh.update();
            aliveSheeps.add(sh);
        });
        sheeps = aliveSheeps;
    }

    private String move(int x, int y) {
        int xDiff = x - posActual[0];
        if (xDiff != 0) {
            if (xDiff > 0) return "(move-right)";
            return "(move-left)";
        }

        int yDiff = y - posActual[1];
        if (yDiff != 0) {
            if (yDiff > 0) return "(move-up)";
            return "(move-down)";
        }
        return "";
    }

    private String explore() {
        if (destination == null) destination = map.getNextNotVisited(posActual);
        String content = move(destination[0],destination[1]);
        if (content.equals("")) {
            destination = null;
            return explore();
        }
        else return content;
    }

    private merchant getClosestMerchant(ArrayList<merchant> merchant, boolean buying) {
        if (merchant.size() > 0) {
            int index = 0;
            int minDist = abs(merchant.get(0).getPosition()[0] - posActual[0]) + abs(merchant.get(0).getPosition()[1] - posActual[1]);

            for (int i = 1; i < merchant.size(); ++i) {
                merchant m = merchant.get(i);
                int actDist = abs(m.getPosition()[0] - posActual[0]) + abs(m.getPosition()[1] - posActual[1]);
                if (actDist < minDist) {
                    if (buying) {
                        if (enoughMoney(m, 0)) {
                            minDist = actDist;
                            index = i;
                        }
                    } else {
                        minDist = actDist;
                        index = i;
                    }
                }
            }
            if (merchant.size() == 0) return null;
            else return merchant.get(index);
        }
        else return null;
    }

    class infromEnvironment extends AchieveREInitiator {
        public infromEnvironment(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected void handleRefuse(ACLMessage refuse) {
        }

        protected void handleFailure(ACLMessage failure) {
        }
    }

    public class parser extends CyclicBehaviour {

        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String msgS = msg.getContent();
                if(msg.getSender().getName().equals(environment[0].getName())) {
                    if (msgS != null) parseMsg(msgS, true, msg.getSender().getName());
                }
                else {  //other agent sent the message
                    if (msgS != null) parseMsg(msgS, false, msg.getSender().getName());
                }
            }
        }

        private void parseMsg(String msgS, boolean fromEnv, String sender) {
            //System.out.println(msgS);
            switch (getFirst(msgS)) {
                case "pos": {
                    String elem = getElem(msgS);
                    if (elem.equals("element")) {
                        String type = getTypeElem(msgS);
                        switch (type) {
                            case "grass": {
                                Integer[] pos = getPosCarElem(msgS);
                                float amount = getSingleFloat(msgS, true);
                                if(fromEnv) {
                                    cell c = map.getCell(pos[0], pos[1], true);
                                    c.setGrass(amount);
                                    c.setSheeps(0);
                                    c.setTurn(turn);
                                }
                                else if(dontLie.contains(sender)) {
                                    //si no miente guardamos la info si no nos la ha enviado ya el environment en el turno
                                    //y volvemos a asegurarnos que no miente si es asi
                                    cell c = map.getCell(pos[0], pos[1], true);
                                    if(c.getTurn() == turn) {
                                        if(c.getGrass() != pos[2]) Lie.add(sender);
                                    }
                                    else {
                                        c.setGrass(amount);
                                        c.setSheeps(0);
                                        c.setTurn(turn);
                                    }
                                }
                                else if(!Lie.contains(sender)) {
                                    cell c = map.getCell(pos[0], pos[1], false);
                                    if(c.getTurn() == turn) {
                                        if(c.getGrass() == pos[2]) {
                                            dontLie.add(sender);
                                            //System.out.println("Agent "+sender+" doesn't lie");
                                        }
                                        else {
                                            Lie.add(sender);
                                            //System.out.println("Agent "+sender+" lies");
                                        }
                                    }
                                }

                                for (AID a : shepherds) {
                                    String reciver = a.getName();
                                    if (agent.this.getName().equals(reciver)) return;
                                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                                    msg.addReceiver(a);
                                    msg.setLanguage("fipa-sl");
                                    int taxedAmount = pos[2]-(pos[2]/3);    //le restamos el 33% al pasto
                                    msg.setContent("(pos :element (grass :amount "+taxedAmount+") :x "+pos[0]+" :y "+pos[1]+")");
                                    send(msg);
                                }
                                break;
                            }

                            case "buyer": {
                                if(fromEnv) {
                                    merchant m = getMerchantElements(msgS, true);
                                    int index = getIndexMerchant(m.getPosition(), "buyers");
                                    if (index == -1) buyers.add(m);
                                    else buyers.set(index, m);
                                }
                                break;
                            }

                            case "seller": {
                                if(fromEnv) {
                                    merchant m2 = getMerchantElements(msgS, false);
                                    int index2 = getIndexMerchant(m2.getPosition(), "sellers");
                                    if (index2 == -1) sellers.add(m2);
                                    else sellers.set(index2, m2);
                                }
                               break;
                            }

                            case "agent": {
                                if (fromEnv) {
                                    Integer[] pos4 = new Integer[2];
                                    pos4[0] = getPosCarElem(msgS)[0];
                                    pos4[1] = getPosCarElem(msgS)[1];
                                    String nameAgent = getId(msgS);
                                    if (nameAgent.equals(agent.this.getName())) posActual = pos4;
                                }
                                break;
                            }

                            case "sheep": {
                                if (fromEnv) {
                                    sheep s = getSheepElements(msgS);
                                    int index3 = getIndexSheep(s.getId());
                                    if (s.getOwner().equals(agent.this.getName())) {
                                        if (index3 != -1) sheeps.set(index3, s);
                                        else if (s.isAlive()) sheeps.add(s);
                                    }
                                    if (s.getPosition()[0] != -1 && s.isAlive() && !s.isTransported()) allSheeps.add(s);
                                    break;
                                }
                            }
                        }
                    }
                    break;
                }

                case "start-new-turn": {
                    if (fromEnv &&getSingleNum(msgS) >= 0) turn = getSingleNum(msgS);
                    nextMove();
                    break;
                }

                case "money-available": {
                    if (fromEnv &&getSingleFloat(msgS, false) >= 0) money = getSingleFloat(msgS, false);
                    break;
                }

                case "wool-held": {
                    if (fromEnv && getSingleFloat(msgS, false) >= 0) wool = getSingleFloat(msgS, false);
                    break;
                }
            }
        }

        private String getFirst(String msg) {
            String[] arrMsg = msg.split(" ");
            if (arrMsg.length > 0) return (arrMsg[0].substring(1));
            else return "None";
        }

        private String getElem(String msg) {
            Pattern pat = Pattern.compile(":\\S+");
            Matcher m = pat.matcher(msg);
            ArrayList<String> arrMsg = new ArrayList<>();

            while (m.find()) arrMsg.add(m.group(0));
            if (arrMsg.size() != 0) return arrMsg.get(0).substring(1);
            return "None";
        }

        private String getTypeElem(String msg) {
            Pattern pat = Pattern.compile("\\(\\S+");
            Matcher m = pat.matcher(msg);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) arrMsg.add(m.group(0));
            if (arrMsg.size() != 0) return arrMsg.get(1).substring(1);
            return "None";
        }

        private Integer[] getPosCarElem(String msg) {
            Pattern pat = Pattern.compile(":\\S+ \\d+");
            Matcher m = pat.matcher(msg);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) arrMsg.add(m.group(0));
            int x = -1;
            int y = -1;
            int car = -1;

            for (String s : arrMsg) {
                String[] separed = s.split(" ");
                if (separed.length > 0) {
                    if (separed[0].equals(":x")) x = Integer.parseInt(separed[1]);
                    else if (separed[0].equals(":y")) y = Integer.parseInt(separed[1]);
                    else car = Integer.parseInt(separed[1]);
                }
            }
            Integer[] arr = new Integer[3];
            arr[0] = x;
            arr[1] = y;
            arr[2] = car;

            return arr;
        }

        private int getSingleNum(String msgS) {
            Pattern pat = Pattern.compile("\\d+");
            Matcher m = pat.matcher(msgS);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) arrMsg.add(m.group(0));
            if (arrMsg.size() > 0) return parseInt(arrMsg.get(arrMsg.size() - 1));
            else return -1;
        }

        private float getSingleFloat(String msgS, boolean amount) {
            Pattern pat = Pattern.compile("\\d+(?:\\.\\d*)?|\\.\\d+");
            Matcher m = pat.matcher(msgS);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) {
                arrMsg.add(m.group(0));
            }
            if (amount && arrMsg.size() > 0) return parseFloat(arrMsg.get(0));
            if (arrMsg.size() > 0) return parseFloat(arrMsg.get(arrMsg.size() - 1));
            else return (float) -1.0;
        }

        private float getWoolFloat(String msgS) {
            Pattern pat = Pattern.compile(":\\S+ \\d+(?:\\.\\d*)?|\\.\\d+");
            Matcher m = pat.matcher(msgS);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) {
                arrMsg.add(m.group(0));
            }

            if (arrMsg.size() > 0){
                String[] separed = arrMsg.get(0).split(" ");
                return parseFloat(separed[separed.length - 1]);
            }
            return -1;
        }

        private String getId(String msgS) {
            Pattern pat = Pattern.compile("\"([^\"]*)\"");
            Matcher m = pat.matcher(msgS);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) arrMsg.add(m.group(0));
            if (arrMsg.size() > 0) return arrMsg.get(0).substring(1, arrMsg.get(0).length() - 1);
            return "None";
        }

        private sheep getSheepElements(String msgS) {
            String id = "None";
            String owner = "None";
            Integer[] pos = new Integer[2];
            pos[0] = -1;
            pos[1] = -1;;
            boolean alive = false;
            boolean transported = false;

            Pattern pat = Pattern.compile(":\\S+ \\d+");
            Matcher m = pat.matcher(msgS);
            ArrayList<String> arrMsg = new ArrayList<>();
            while (m.find()) {
                arrMsg.add(m.group(0));
            }

            for (String s : arrMsg) {
                String[] separed = s.split(" ");
                if (separed.length > 0) {
                    if (separed[0].equals(":x")) pos[0] = Integer.parseInt(separed[1]);
                    else if (separed[0].equals(":y")) pos[1] = Integer.parseInt(separed[1]);
                }
            }

            pat = Pattern.compile(":\\S+? \"([^\"]*)\" ");
            m = pat.matcher(msgS);
            ArrayList<String> arrMsg2 = new ArrayList<>();
            while (m.find()) {
                arrMsg2.add(m.group(0));
            }
            for (String s : arrMsg2) {
                String[] separed = s.split(" ");
                if (separed.length > 0) {
                    if (separed[0].equals(":id")) id = separed[1];
                    else owner = separed[1];
                }
            }

            pat = Pattern.compile(":\\S+ \\S+");
            m = pat.matcher(msgS);
            ArrayList<String> arrMsg3 = new ArrayList<>();
            while (m.find()) arrMsg3.add(m.group(0));
            for (String s : arrMsg3) {
                String[] separed = s.split(" ");
                if (separed.length > 0) {
                    if (separed[0].equals(":alive?")) alive = separed[1].equals("true");
                    if (separed[0].equals(":transported?")) transported = separed[1].equals("true)");
                }
            }
            float wool2 = getWoolFloat(msgS);
            return new sheep(pos, wool2, id.substring(1, id.length() - 1), owner.substring(1, owner.length() - 1), alive, transported, turn);
        }

        private merchant getMerchantElements(String msgS, boolean isBuyer) {
            Integer[] elem = getPosCarElem(msgS);
            Integer[] pos = new Integer[2];
            pos[0] = elem[0];
            pos[1] = elem[1];
            return new merchant(pos, elem[2], isBuyer);
        }

    }

    private int getIndexMerchant(Integer[] pos, String nameArray) {
        int x = pos[0];
        int y = pos[1];
        if (nameArray.equals("sellers")) {
            for (int i = 0; i < sellers.size(); ++i) {
                if (sellers.get(i).getPosition()[0] == x && sellers.get(i).getPosition()[1] == y) return i;
            }
        } else {
            for (int i = 0; i < buyers.size(); ++i) {
                if (buyers.get(i).getPosition()[0] == x && buyers.get(i).getPosition()[1] == y) return i;
            }
        }
        return -1;
    }

    private int getIndexSheep(String id) {
        for (int i = 0; i < sheeps.size(); ++i) {
            if (sheeps.get(i).getId().equals(id)) return i;
        }
        return -1;
    }

    private void registerDF(String name) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setName(name);
        templateSd.setType("shepherd");
        template.addServices(templateSd);

        try {
            DFService.register(this, template);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void deregisterDF() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            Logger.getLogger(agent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private AID[] searchAgents(String type) {
        AID df = getDefaultDF();

        DFAgentDescription search_template = new DFAgentDescription();
        ServiceDescription sd2 = new ServiceDescription();
        sd2.setType(type);
        search_template.addServices(sd2);

        DFAgentDescription[] search_results;
        try {
            search_results = DFService.search(this, df, search_template);
            if (search_results.length > 0) {
                AID[] OtherAIDs = new AID[search_results.length];
                for (int i = 0; i < search_results.length; ++i) OtherAIDs[i] = search_results[i].getName();
                return OtherAIDs;
            }
        } catch (FIPAException ex) {
            Logger.getLogger(agent.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}