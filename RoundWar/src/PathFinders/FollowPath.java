/*******************************************************************************
 * Copyright (c) 2014
 *
 * @author Elisabet Romero Vaquero
 *******************************************************************************/
package PathFinders;

import java.util.ArrayList;
import java.util.Collections;

import Entities.LivingEntity;

import com.badlogic.gdx.math.Vector2;

public class FollowPath extends PathFinder {
	/** The set of nodes that have been searched through */
	private ArrayList<Node> closed;
	/** The set of nodes that we do not yet consider fully searched */
	private SortedList open;
	/** The nodes visited */
	private int[][] visited;
	/** The maximum depth of search we're willing to accept before giving up */
	private int maxSearchDistance;
	
	/**
	 * Create a path finder with the default heuristic - closest to target.
	 * 
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param allowDiagMovement True if the search should try diaganol movement
	 */
	public FollowPath() {
		super();
		this.maxSearchDistance = 10;
		
		closed = new ArrayList<Node>();
		open = new SortedList();
		visited = new int[getLayer().getWidth()][getLayer().getHeight()];
	}
	
	/**
	 * @see FollowPath#findPath(entity, int, int, int, int)
	 */
	@Override
	public Vector2 findNext(LivingEntity entity, LivingEntity entityTarget) {
		int initialCellX = (int)(entity.getCenterX()/getLayer().getTileWidth());
		int initialCellY = (int)(entity.getCenterY()/getLayer().getTileWidth());
		int finalCellX   = (int)(entityTarget.getCenterX()/getLayer().getTileWidth());
		int finalCellY   = (int)(entityTarget.getCenterY()/getLayer().getTileWidth());
		
		// easy first check, if the destination is blocked, we can't get there
		if (nodes[finalCellX][finalCellY].cost == -1) {
			return null;
		}
		
		// initial state for A*. The closed group is empty. Only the starting
		// tile is in the open list and it'e're already there
		nodes[initialCellX][initialCellY].cost = 0;
		nodes[initialCellX][initialCellY].depth = 0;
		closed.clear();
		open.clear();
		open.add(nodes[initialCellX][initialCellY]);
		
		nodes[finalCellX][finalCellY].parent = null;
		
		// while we haven'n't exceeded our max search depth
		int maxDepth = 0;
		while ((maxDepth < maxSearchDistance) && (open.size() != 0)) {
			// pull out the first node in our open list, this is determined to 
			// be the most likely to be the next step based on our heuristic
			Node current = getFirstInOpen();
			if (current == nodes[finalCellX][finalCellY]) {
				break;
			}
			removeFromOpen(current);
			addToClosed(current);
			
			// search through all the neighbours of the current node evaluating
			// them as next steps
			for (int x=-1;x<2;x++) {
				for (int y=-1;y<2;y++) {
					// not a neighbour, its the current tile
					if ((x == 0) && (y == 0)) {
						continue;
					}
					// determine the location of the neighbour and evaluate it
					int xp = x + current.x;
					int yp = y + current.y;
					if (nodes[xp][yp].cost != -1) { //isValidLocation(entity,initialX,initialY,xp,yp)) {
						// the cost to get to this node is cost the current plus the movement
						// cost to reach this node. Note that the heursitic value is only used
						// in the sorted open list

						float nextStepCost = current.cost + 1;//getMovementCost(entity, current.x, current.y, xp, yp);
						Node neighbour = nodes[xp][yp];
						visited[xp][yp] = 1;//map.pathFinderVisited(xp, yp);
						
						// if the new cost we've determined for this node is lower than 
						// it has been previously makes sure the node hasn'e've
						// determined that there might have been a better path to get to
						// this node so it needs to be re-evaluated

						if (nextStepCost < neighbour.cost) {
							if (inOpenList(neighbour)) {
								removeFromOpen(neighbour);
							}
							if (inClosedList(neighbour)) {
								removeFromClosed(neighbour);
							}
						}
						
						// if the node hasn't already been processed and discarded then
						// reset it's cost to our current cost and add it as a next possible
						// step (i.e. to the open list)
						if (!inOpenList(neighbour) && !(inClosedList(neighbour))) {
							neighbour.cost = nextStepCost;
							neighbour.heuristic = getHeuristicCost(xp, yp, finalCellX, finalCellY); // ???
							maxDepth = Math.max(maxDepth, neighbour.setParent(current));
							addToOpen(neighbour);
						}
					}
				}
			}
		}

		// since we'e've run out of search 
		// there was no path. Just return null
		if (nodes[finalCellX][finalCellY].parent == null) {
			return null;
		}
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		Node target = nodes[finalCellX][finalCellY];
		while (target.parent != nodes[initialCellX][initialCellY]) {
			//path.addFirst(new Vector2(target.x*layer.getTileWidth(), target.y*layer.getTileHeight()));//.prependStep(target.x, target.y);
			target = target.parent;
		}
		return nodeToVector(target);
	}
	
	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	private Node getFirstInOpen() {
		return (Node) open.first();
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	private void addToOpen(Node node) {
		open.add(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	private boolean inOpenList(Node node) {
		return open.contains(node);
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	private void removeFromOpen(Node node) {
		open.remove(node);
	}
	
	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	private void addToClosed(Node node) {
		closed.add(node);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	private boolean inClosedList(Node node) {
		return closed.contains(node);
	}
	
	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	private void removeFromClosed(Node node) {
		closed.remove(node);
	}
	
	/**
	 * Check if a given location is valid for the supplied entity
	 * 
	 * @param entity The entity that would hold a given location
	 * @param initialX The starting x coordinate
	 * @param initialY The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @return True if the location is valid for the given entity
	 */
	/*private boolean isValidLocation(LivingEntity entity, int initialX, int initialY, int posX, int posY) {
		boolean invalid = (posX < 0) || (posY < 0) || (posX >= getLayer().getWidth()) || (posY >= getLayer().getHeight());
		//Aquí se verá si choca contra otro bicho ?
		if ((!invalid) && ((initialX != posX) || (initialY != posY))) {
			if(nodes[posX][posY].cost == -1) invalid = true;
			else invalid = false;
		}
		return !invalid;
	}*/
	
	/**
	 * A simple sorted list
	 *
	 * @author kevin
	 */
	private class SortedList {
		/** The list of elements */
		private ArrayList<Node> list = new ArrayList<Node>();
		
		/**
		 * Retrieve the first element from the list
		 *  
		 * @return The first element from the list
		 */
		public Object first() {
			return list.get(0);
		}
		
		/**
		 * Empty the list
		 */
		public void clear() {
			list.clear();
		}
		
		/**
		 * Add an element to the list - causes sorting
		 * 
		 * @param o The element to add
		 */
		public void add(Node node) {
			list.add(node);
			Collections.sort(list);
		}
		
		/**
		 * Remove an element from the list
		 * 
		 * @param o The element to remove
		 */
		public void remove(Object o) {
			list.remove(o);
		}
	
		/**
		 * Get the number of elements in the list
		 * 
		 * @return The number of element in the list
 		 */
		public int size() {
			return list.size();
		}
		
		/**
		 * Check if an element is in the list
		 * 
		 * @param o The element to search for
		 * @return True if the element is in the list
		 */
		public boolean contains(Object o) {
			return list.contains(o);
		}
	}
}
