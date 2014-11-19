package outpost.group7;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
    static int size = 100;
    static Point[] grid = new Point[size*size];
    static Random random = new Random();
    static int[] theta = new int[100];
    static int counter = 0;

    ////////////////////////////////////////////////////////////////////
    static Pair HOME_CELL;
    static Direction X_AWAY;
    static Direction X_BACK;
    static Direction Y_AWAY;
    static Direction Y_BACK;
    static Direction STAY;
    static int R;
    static int L;
    static int W;
    static int T;
    static ArrayList<Outpost> myOutposts;
    static Mastermind mastermind;
    static boolean tenthTurn = false;
    ////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////
    enum Strategy {
        PEEL
    }

    class Mastermind {
        public Strategy strategy;

        public Mastermind(Strategy strategy) { 
            this.strategy = strategy;  
        }

        public void changeStrategy(Strategy newStrategy) {
            strategy = newStrategy;
        }

        public void dispatch() {
            if (strategy == Strategy.PEEL) {
                System.out.println("Strategy is PEEL");
                for (Outpost outpost : myOutposts) {
                    Stack<Direction> moves = new Stack<Direction>();
                    int i;
                    for (i = 0; i < outpost.id; i++) {
                        if (outpost.id % 2 == 0) {
                            moves.push(X_AWAY);
                        }
                        else {
                            moves.push(Y_AWAY);
                        }
                    }
                    while (i < 10) {
                        if (outpost.id % 2 == 0) {
                            moves.push(Y_AWAY);
                        }
                        else {
                            moves.push(X_AWAY);
                        }
                        i++;
                    }
                    outpost.assignMoves(moves);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////
    enum Direction {
        LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1), STAY(0, 0);
        public int dx;
        public int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    class Outpost {
        public int id;
        public Pair position;
        public Stack<Direction> moves;
        public boolean deleted;

        public Outpost(int id) {
            this.id = id;
            position = HOME_CELL;
            deleted = false;
        }

        public void updatePosition(Pair newPosition) {
            position = newPosition;
        }

        public void assignMoves(Stack<Direction> moves) {
            this.moves = moves;
        }

        public Direction move() {
            return moves.pop();
        }
    }
    ////////////////////////////////////////////////////////////////////
	
    public Player(int id_in) {
		super(id_in);
		// TODO Auto-generated constructor stub
	}

	public void init() {
    	for (int i=0; i<100; i++) {
    		theta[i]=random.nextInt(4);
    	}
    }

    public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
        int del = random.nextInt(king_outpostlist.get(id).size());
        myOutposts.get(del).deleted = true;
        return del;
    }
    
    static double distance(Point a, Point b) {
        return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y));
    }

    static void deepCopyGrid(Point[] gridIn) {
        for (int i = 0; i < gridIn.length; i++) {
            grid[i] = new Point(gridIn[i]);
        }
    }
    
    public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int t) {
    	// Initialize once
        if (counter == 0) {
            Pair firstOutpost = king_outpostlist.get(this.id).get(0);
            this.HOME_CELL = new Pair(firstOutpost.x, firstOutpost.y);
            System.out.println("Home cell: (" + HOME_CELL.x + ", " + HOME_CELL.y + ")");
            // Init meaning of directions
            if (HOME_CELL.x == 0 && HOME_CELL.y == 0) {
                X_AWAY = Direction.RIGHT;
                X_BACK = Direction.LEFT;
                Y_AWAY = Direction.DOWN;
                Y_BACK = Direction.UP;
            }
            else if (HOME_CELL.x == 0 && HOME_CELL.y > 0) {
                X_AWAY = Direction.RIGHT;
                X_BACK = Direction.LEFT;
                Y_AWAY = Direction.UP;
                Y_BACK = Direction.DOWN;
            }
            else if (HOME_CELL.x > 0 && HOME_CELL.y == 0) {
                X_AWAY = Direction.LEFT;
                X_BACK = Direction.RIGHT;
                Y_AWAY = Direction.DOWN;
                Y_BACK = Direction.UP;
            }
            else {
                X_AWAY = Direction.LEFT;
                X_BACK = Direction.RIGHT;
                Y_AWAY = Direction.UP;
                Y_BACK = Direction.DOWN;
            }
            STAY = Direction.STAY;

            this.R = r;
            this.L = L;
            this.W = W;
            this.T = t;

            myOutposts = new ArrayList<Outpost>();
            myOutposts.add(new Outpost(0));

            mastermind = new Mastermind(Strategy.PEEL);
            mastermind.dispatch();
        }

        // Update internal representation of game 
        deepCopyGrid(gridin);

        ArrayList<Pair> updatedOutposts = king_outpostlist.get(this.id); 
        if (updatedOutposts.size() < myOutposts.size()) {
            // We lost outposts :(
            // Update outpost list and outpost ID's stored internally
            int i;
            for (i = 0; i < myOutposts.size(); i++) {
                if (myOutposts.get(i).deleted) {
                    myOutposts.remove(i);
                    break;
                }
            }
            while (i < myOutposts.size()) {
                myOutposts.get(i).id--;
                i++;
            }
        }
        else {
            int i;
            for (i = 0; i < myOutposts.size(); i++) {
                myOutposts.get(i).updatePosition(updatedOutposts.get(i));
            }
            // Did we get new outposts?
            while (i < updatedOutposts.size()) {
                myOutposts.add(new Outpost(i));
                i++;
            }
        }

        // Update counter
        counter++;
        if (counter % 10 == 0) {
            tenthTurn = true;
        }
        else {
            tenthTurn = false;
        }

        // For now have mastermind re-dispatch everyone on the tenth turn
        if (tenthTurn) {
            mastermind.dispatch();
        }
    	
        // Get moves from each outpost
        ArrayList<movePair> nextList = new ArrayList<movePair>();
        for (Outpost outpost : myOutposts) {
            Pair currentPosition = outpost.position;
            Direction move = outpost.move();
            Pair newPosition = new Pair(currentPosition.x + move.dx, currentPosition.y + move.dy);
            nextList.add(new movePair(outpost.id, newPosition));
        }

        return nextList;
    }
   
}
