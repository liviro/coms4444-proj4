package outpost.group7;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
	static int size = 100;
	static Point[] grid = new Point[size*size];
	static Random random = new Random();
	static int counter = 0;

	// place two adjacent outposts for protection, only turned on for L=12 (because L=40 causes lack of land cells)
	static int LAND_THRESHOLD = 500; 		// set this to a large value, e.g. 10000, to turn off protection for stationary outposts
	static int LAND_THRESHOLD2 = 1000;		// set this to a large value, e.g. 10000, to turn off protection for grid outposts
	// threshold for changing strategies
	static int WATER_THRESHOLD = 50;		// water threshold for changing strategy between GET_STUFF and ARMY

	////////////////////////////////////////////////////////////////////
	static Pair HOME_CELL;
	static int playerId;
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
	static ArrayList<Pair> waterPositions;
	static Mastermind mastermind;
	static boolean tenthTurn = false;
	////////////////////////////////////////////////////////////////////

	double LACK_WATER;
	double LACK_LAND;
	double WATER_OWNED;
	double LAND_OWNED;
	int[][] water = new int[size][size];
	int[][] land = new int[size][size];


	////////////////////////////////////////////////////////////////////
	enum Strategy {
		PEEL,
		ARMY,
		GET_STUFF
	}

	class Mastermind {
		public Strategy strategy;

		public Mastermind(Strategy strategy) {
			this.strategy = strategy;
		}

		public void changeStrategy(Strategy newStrategy) {
			strategy = newStrategy;
		}

		public void think() {
			computeResources(myOutposts);
			//System.out.printf("LACKWATER = %f, LACKLAND = %f\n", LACK_WATER, LACK_LAND);
			if (LACK_WATER < WATER_THRESHOLD)
				changeStrategy(Strategy.GET_STUFF);
			else
				changeStrategy(Strategy.ARMY);
		}

		public void dispatch() {
			if (strategy == Strategy.PEEL) {
				//System.out.println("Strategy is PEEL");
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

			if (strategy == Strategy.GET_STUFF) {
				// System.out.println("Strategy is GET_STUFF");
				ArrayList<Pair> positions = waterPositions;
				for (Outpost outpost : myOutposts) {
					if (!outpost.station) {
						outpost.target = null;
						ArrayList<Pair> candidates = (ArrayList<Pair>) positions.clone();
						while (outpost.target == null) {
							double minDistance = 500;
							Pair closest = null;
							for (Pair candidate : candidates) {
								if (manhattanDistance(outpost.position, candidate) < minDistance) {
									minDistance = manhattanDistance(outpost.position, candidate);
									closest = candidate;
								}
								if (manhattanDistance(outpost.position, candidate) > 500) {
									//System.out.println("What the fuck");
								}
							}
							candidates.remove(closest);
							boolean overlap = false;
							for (Outpost other : myOutposts) {
								if (other.station) {
									// if (closest == null) {
									// 	System.out.println("Positions size = " + positions.size());
									// 	System.out.println("Candidates size = " + candidates.size());
									// 	System.out.println(minDistance);
									// 	System.out.println("closest is null");
									// }
									if (closest != null && overlapWithTolerance(other.target, closest)) {
										overlap = true;
									}
								}
							}
							if (!overlap) {
								if (closest != null) {
									outpost.target = closest;
									outpost.station = true;
									outpost.protect = false;
								}
								else {
									//System.out.println("closest was null");
									outpost.target = HOME_CELL;
								}
							}
						}
					}

				}

				for (Outpost outpost : myOutposts) {
					Stack<Direction> moves = new Stack<Direction>();
					if (outpost.target.equals(outpost.position)) {
						moves.push(STAY);
					}
					else {
						// perform BFS, ignoring other players' outposts
						// target cannot be a water cell
						int[] cx = {-1, 0, 1, 0};
						int[] cy = {0, -1, 0, 1};
						Queue<Pair> q = new LinkedList<Pair>();
						q.add(outpost.position);
						boolean[][] vst = new boolean[size][size];
						int[][] parentx = new int[size][size];
						int[][] parenty = new int[size][size];
						for (int i = 0; i < size; ++i) {
							for (int j = 0; j < size; ++j) {
								vst[i][j] = false;
								parentx[i][j] = -1;
								parenty[i][j] = -1;
							}
						}
						vst[outpost.position.x][outpost.position.y] = true;
						while (!q.isEmpty()) {
							Pair p = new Pair(q.peek());
							q.poll();
							for (int i = 0; i < 4; ++i) {
								int x = p.x + cx[i];
								int y = p.y + cy[i];
								if (x < 0 || x >= size || y < 0 || y >= size || vst[x][y] || grid[x * size + y].water)
									continue;
								vst[x][y] = true;
								parentx[x][y] = p.x;
								parenty[x][y] = p.y;
								q.add(new Pair(x, y));
								if (x == outpost.target.x && y == outpost.target.y) {
									while (!(parentx[x][y] == outpost.position.x && parenty[x][y] == outpost.position.y)) {
										int newx = parentx[x][y];
										int newy = parenty[x][y];
										x = newx;
										y = newy;
									}
									moves.push(toDirection(x - outpost.position.x, y - outpost.position.y));
									q.clear();
									break;
								}
							}
						}
					}
					outpost.assignMoves(moves);
				}
			}

			if (strategy == Strategy.ARMY) {
				//System.out.println("Strategy is ARMY");
				// general strategy
				// (1) generate bestPositions according to weights
				//computeResources(myOutposts);
				//System.out.printf("LACKWATER = %f, LACKLAND = %f\n", LACK_WATER, LACK_LAND);
				ArrayList<Pair> bestPositions = new ArrayList<Pair>();
				// add protection
				if (L == 12 && LACK_LAND >= LAND_THRESHOLD) {
					for (Outpost soldier : myOutposts) {
						if (soldier.station)
							continue;
						for (Outpost outpost : myOutposts) {
							if (outpost.station && !outpost.protect) {
								outpost.protect = true;
								soldier.protect = true;
								soldier.station = true;
								int[] cx = {-1, 0, 1, 0};
								int[] cy = {0, -1, 0, 1};
								for (int i = 0; i < 4; ++i) {
									int x = outpost.position.x + cx[i];
									int y = outpost.position.y + cy[i];
									if (x < 0 || x >= size || y < 0 || y >= size || isInWater(new Pair(x, y)))
										continue;
									soldier.target = new Pair(x, y);
									break;
								}
								//System.out.printf("set protection: %d %d\n", soldier.target.x, soldier.target.y);
								break;
							}
						}
					}
				}
				int rem = 0;
				for (Outpost outpost : myOutposts) {
					if (!outpost.station)
						rem += 1;
				}
				bestPositions.addAll(findBestPositions(rem));

				// (2) assign bestPositions as target positions to outposts
				// currently assigned to the nearest outpost based on manhattan distance
				for (Outpost outpost : myOutposts) {
					if (outpost.station)
						continue;
					outpost.target = null;
				}
				Collections.sort(bestPositions, new PairComparator());
				for (Pair position : bestPositions) {
					int minDist = Integer.MAX_VALUE;
					int oid = -1;
					for (int i = 0; i < myOutposts.size(); ++i) {
						Outpost outpost = myOutposts.get(i);
						if (outpost.station || outpost.target != null)
							continue;
						int dist = manhattanDistance(position, outpost.position);
						if (dist < minDist) {
							minDist = dist;
							oid = i;
						}
					}
					myOutposts.get(oid).target = new Pair(position);
					//myOutposts.get(oid).protect = true;
				}
				/*System.out.println("xxx");
				for (Outpost outpost : myOutposts) {
					if (outpost.station) continue;
					System.out.printf("outpost target: (%d, %d) -- (%d, %d)\n", outpost.position.x, outpost.position.y, outpost.target.x, outpost.target.y);
					//System.out.printf("water = %d, land = %d\n", water[outpost.target.x][outpost.target.y], land[outpost.target.x][outpost.target.y]);
				}*/

				// soldier strategy
				for (Outpost outpost : myOutposts) {
					Stack<Direction> moves = new Stack<Direction>();
					if (outpost.target.equals(outpost.position)) {
						moves.push(STAY);
					}
					else {
						// perform BFS, ignoring other players' outposts
						// target cannot be a water cell
						int[] cx = {-1, 0, 1, 0};
						int[] cy = {0, -1, 0, 1};
						Queue<Pair> q = new LinkedList<Pair>();
						q.add(outpost.position);
						boolean[][] vst = new boolean[size][size];
						int[][] parentx = new int[size][size];
						int[][] parenty = new int[size][size];
						for (int i = 0; i < size; ++i) {
							for (int j = 0; j < size; ++j) {
								vst[i][j] = false;
								parentx[i][j] = -1;
								parenty[i][j] = -1;
							}
						}
						vst[outpost.position.x][outpost.position.y] = true;
						while (!q.isEmpty()) {
							Pair p = new Pair(q.peek());
							q.poll();
							for (int i = 0; i < 4; ++i) {
								int x = p.x + cx[i];
								int y = p.y + cy[i];
								if (x < 0 || x >= size || y < 0 || y >= size || vst[x][y] || grid[x * size + y].water)
									continue;
								vst[x][y] = true;
								parentx[x][y] = p.x;
								parenty[x][y] = p.y;
								q.add(new Pair(x, y));
								if (x == outpost.target.x && y == outpost.target.y) {
									while (!(parentx[x][y] == outpost.position.x && parenty[x][y] == outpost.position.y)) {
										int newx = parentx[x][y];
										int newy = parenty[x][y];
										x = newx;
										y = newy;
									}
									moves.push(toDirection(x - outpost.position.x, y - outpost.position.y));
									q.clear();
									break;
								}
							}
						}
					}
					outpost.assignMoves(moves);
				}
			}
		}

		// find best positions based on map and resources
		// weight the cells to get the best
		public ArrayList<Pair> findBestPositions(int n) {
			int xAway, xBack, yAway, yBack;
			if (HOME_CELL.x == 0) {
				xAway = 1;
				xBack = -1;
			}
			else {
				xAway = -1;
				xBack = 1;
			}
			if (HOME_CELL.y == 0) {
				yAway = 1;
				yBack = -1;
			}
			else {
				yAway = -1;
				yBack = 1;
			}
			ArrayList<Pair> positions = new ArrayList<Pair>();
			int cnt = 0;
			int start = 0;
			while (cnt < n) {
				int row = HOME_CELL.x;
				int col = HOME_CELL.y + start * (2*R - 2) * yAway;
				while (cnt < n) {
					if (col < 0 || col >= size) break;
					if (!grid[row * size + col].water) {
						++cnt;
						positions.add(new Pair(row, col));
						// add protection
						if (cnt < n && (LACK_LAND >= LAND_THRESHOLD2 && L == 12)) {
							if (row + xAway >= 0 && row + xAway < size && !isInWater(new Pair(row + xAway, col)))
								positions.add(new Pair(row + xAway, col));
							else if (row + xBack >= 0 && row + xBack < size && !isInWater(new Pair(row + xBack, col)))
								positions.add(new Pair(row + xBack, col));
							else if (col + yAway >= 0 && col + yAway < size && !isInWater(new Pair(row, col + yAway)))
								positions.add(new Pair(row, col + yAway));
							else if (col + yBack >= 0 && col + yBack < size && !isInWater(new Pair(row, col + yBack)))
								positions.add(new Pair(row, col + yBack));
							++cnt;
						}
					}
					row += (R - 1) * xAway;
					col += (R - 1) * yBack;
				}
				++start;
			}
			return positions;
		}

		public ArrayList<Pair> findBestPositionsTight(int n) {
			ArrayList<Pair> positions = new ArrayList<Pair>();
			int cnt = 0;
			int start = 0;
			while (cnt < n) {
				int row = 0;
				int col;
				col = start;
				while (col >= 0 && cnt < n) {
					int newXPos, newYPos;
					if( X_AWAY == Direction.RIGHT ) {
						newXPos = R * row;
					} else {
						newXPos = size-1 - R * row;
					}
					if( Y_AWAY == Direction.DOWN ) {
						newYPos = R * col;
					} else {
						newYPos = size-1 - R * col;
					}
					Pair newPair = new Pair(newXPos, newYPos);
					if(!grid[newXPos * size + newYPos].water) {
						++cnt;
						positions.add(newPair);
					}
					++row;
					--col;
				}
				++start;
			}
			return positions;
		}

		boolean hasWater(Pair a) {
			return (water[a.x][a.y] > 0);
		}

		boolean overlap(Pair a, Pair b) {
			for (int i = a.x - R; i <= a.x + R; ++i) {
				for (int j = a.y - R; j <= a.y + R; ++j) {
					if (manhattanDistance(a, new Pair(i, j)) > R) continue;
					if (manhattanDistance(b, new Pair(i, j)) <= R)
						return true;
				}
			}
			return false;
		}

		boolean overlapWithTolerance(Pair a, Pair b) {
			int tolerance = 3;
			int Rr = R - tolerance;
			for (int i = a.x - Rr; i <= a.x + Rr; ++i) {
				for (int j = a.y - Rr; j <= a.y + Rr; ++j) {
					if (manhattanDistance(a, new Pair(i, j)) > Rr) continue;
					if (manhattanDistance(b, new Pair(i, j)) <= Rr)
						return true;
				}
			}
			return false;
		}

		// compute owned resources
		public void computeResources(ArrayList<Outpost> outposts) {
			double landNeeded = outposts.size() * L;
			double waterNeeded = outposts.size() * W;
			double waterOwned = 0;
			double landOwned = 0;
			for (int i = 0; i < size; ++i) {
				for (int j = 0; j < size; ++j) {
					Point p = grid[i * size + j];
					for (int f = 0; f < p.ownerlist.size(); ++f) {
						if (p.ownerlist.get(f).x == playerId) {
							if (p.water) {
								waterOwned += 1/p.ownerlist.size(); // as in the simulator
							}
							else {
								landOwned += 1/p.ownerlist.size();
							}
						}
					}
				}
			}
			WATER_OWNED = waterOwned;
			LAND_OWNED = landOwned;
			LACK_WATER = waterOwned - waterNeeded;
			LACK_LAND = landOwned - landNeeded;
		}
	}

	static int manhattanDistance(Pair a, Pair b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}

	static double euclidDistance(Pair a, Pair b) {
		return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

	public boolean isInWater(Pair p) {
		return grid[p.x * size + p.y ].water;
	}

	Direction toDirection(int x, int y) {
		if (x == -1 && y == 0)
			return Direction.LEFT;
		else if (x == 1 && y == 0)
			return Direction.RIGHT;
		else if (x == 0 && y == -1)
			return Direction.UP;
		else if (x == 0 && y == 1)
			return Direction.DOWN;
		return Direction.STAY;
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
		public Pair target;
		public Stack<Direction> moves;
		public boolean deleted;
		public boolean station;
		public boolean protect;

		public Outpost(int id) {
			this.id = id;
			position = HOME_CELL;
			deleted = false;
			station = false;
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

	class PairComparator implements Comparator<Pair> {
		@Override
		public int compare (Pair a, Pair b) {
			int d3 = manhattanDistance(new Pair(a.x, a.y), HOME_CELL);
			int d4 = manhattanDistance(new Pair(b.x, b.y), HOME_CELL);
			if (d3 == d4) {
				double d1 = euclidDistance(new Pair(a.x, a.y), new Pair(50, 50));
				double d2 = euclidDistance(new Pair(b.x, b.y), new Pair(50, 50));
				return Double.compare(d2, d1);
			}
			return d4 - d3;
		}
	}

	class Position {
		int x;
		int y;
		int score;

		Position() {
			x = HOME_CELL.x;
			y = HOME_CELL.y;
			score = 0;
		}
		Position(Pair p) {
			x = p.x;
			y = p.y;
			score = 0;
		}
		Position(int x, int y) {
			this.x = x;
			this.y = y;
			score = 0;
		}
		Position(int x, int y, int score) {
			this.x = x;
			this.y = y;
			this.score = score;
		}

		public Pair toPair() {
			return new Pair(x, y);
		}
	}

	class PositionComparator implements Comparator<Position> {
		@Override
		public int compare (Position a, Position b) {
			if (a.score == b.score) {
				return manhattanDistance(new Pair(b.x, b.y), HOME_CELL) - manhattanDistance(new Pair(a.x, a.y), HOME_CELL);
			}
			return a.score - b.score;
		}
	}

	////////////////////////////////////////////////////////////////////

	public Player(int id_in) {
		super(id_in);
	}

	public void init() {
	}

	public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
		int del = random.nextInt(king_outpostlist.get(id).size());
		myOutposts.get(del).deleted = true;
		return del;
	}

	static void deepCopyGrid(Point[] gridIn) {
		for (int i = 0; i < gridIn.length; i++) {
			grid[i] = new Point(gridIn[i]);
			grid[i].ownerlist.addAll(gridIn[i].ownerlist);
		}
	}

	public ArrayList<Pair> waterPositions() {
			ArrayList<Pair> positions = new ArrayList<Pair>();
			for (int x = 0; x < size; x++) {
				for (int y = 0; y < size; y++) {
					if (!isInWater(new Pair(x, y))) {
						ArrayList<Pair> neighbors = new ArrayList<Pair>();
						if (x < size - 1) {
							neighbors.add(new Pair(x + 1, y));
						}
						if (x > 0) {
							neighbors.add(new Pair(x - 1, y));
						}
						if (y < size - 1) {
							neighbors.add(new Pair(x, y + 1));
						}
						if (y > 0) {
							neighbors.add(new Pair(x, y - 1));
						}

						boolean allWater = true;
						for (Pair neighbor : neighbors) {
							if (!isInWater(neighbor)) {
								allWater = false;
							}
						}
						boolean allLand = true;
						for (Pair neighbor : neighbors) {
							if (isInWater(neighbor)) {
								allLand = false;
							}
						}
						if (!allWater && !allLand) {
							positions.add(new Pair(x, y));
						}
					}
				}
			}
			// for (Pair position : positions) {
			// 	System.out.println(position.x + ", " + position.y);
			// }
			return positions;
	}

	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int t) {
		// Initialize once
		if (counter == 0) {
			playerId = this.id;
			Pair firstOutpost = king_outpostlist.get(this.id).get(0);
			this.HOME_CELL = new Pair(firstOutpost.x, firstOutpost.y);
			//System.out.println("Home cell: (" + HOME_CELL.x + ", " + HOME_CELL.y + ")");
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

			//mastermind = new Mastermind(Strategy.PEEL);
			//mastermind.dispatch();
			mastermind = new Mastermind(Strategy.GET_STUFF);

			deepCopyGrid(gridin);
			waterPositions = waterPositions();
		}

		// Update internal representation of game
		deepCopyGrid(gridin);

		// compute available resources
		if (counter == 0) {
			for (int i = 0; i < size; ++i) {
				for (int j = 0; j < size; ++j) {
					water[i][j] = 0;
					land[i][j] = 0;
					if (grid[i * size + j].water) continue;
					for (int x = i - R; x <= i + R; ++x) {
						if (x < 0 || x >= size) continue;
						for (int y = j - R; y <= j + R; ++y) {
							if (y < 0 || y >= size) continue;
							if (manhattanDistance(new Pair(x, y), new Pair(i, j)) > R)
								continue;
							if (grid[x * size + y].water)
								water[i][j] += 1;
							else
								land[i][j] += 1;
						}
					}
				}
			}

		}

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
			// If still less
			while (updatedOutposts.size() < myOutposts.size()) {
				for (i = 0; i < myOutposts.size(); i++) {
					boolean found = false;
					for (Pair outpost : updatedOutposts) {
						if (myOutposts.get(i).position.equals(outpost)) {
							found = true;
							break;
						}
					}
					if (!found) {
						myOutposts.remove(i);
						break;
					}
				}
				while (i < myOutposts.size()) {
					myOutposts.get(i).id--;
					i++;
				}
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

		mastermind.think();
		mastermind.dispatch();
		// For now have mastermind re-dispatch everyone on the tenth turn
		//if (tenthTurn) {
		//	mastermind.dispatch();
		//}

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
