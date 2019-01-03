package client.model;

import common.network.data.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

public class Game
{
    private Map map;
    private GameConstants gameConstants;
    private AbilityConstants[] abilityConstants;
    private HeroConstants[] heroConstants;

    private Hero[] myHeroes;
    private Hero[] oppHeroes;
    private Hero[] myDeadHeroes;
    private Hero[] oppDeadHeroes;

    private Cell[] brokenWalls;
    private Cell[] createdWalls;

    private int AP;
    private int score;


    public Game(Consumer<Message> sender)
    {

    }

    public void handleInitMessage(Message msg)
    {
        /* TODO */// get objects from msg.args.get(0).getAsJsonObject()

    }


    public void handleTurnMessage(Message msg)
    {

    }

    public void handlePickMessage(Message msg)
    {

    }

    public Hero getHero(int id)
    {
        for (int i = 0; i < myHeroes.length; i++)
        {
            if (myHeroes[i].getId() == id)
                return myHeroes[i];
        }
        for (int i = 0; i < oppHeroes.length; i++)
        {
            if (oppHeroes[i].getId() == id)
                return oppHeroes[i];
        }
        return null;
    }

    public Hero getMyHero(Cell cell)
    {
        for (Hero hero : myHeroes)
        {
            if (hero.getCurrentCell() == cell)
                return hero;
        }
        return null;
    }

    public Hero getMyHero(int cellRow, int cellColumn)
    {
        if (!isInMap(cellRow, cellColumn)) return null;
        return getMyHero(map.getCell(cellRow, cellColumn));
    }

    public Hero getOppHero(Cell cell)
    {
        for (Hero hero : oppHeroes)
        {
            if (hero.getCurrentCell() == cell)
                return hero;
        }
        return null;
    }

    public Hero getOppHero(int cellRow, int cellColumn)
    {
        if (!isInMap(cellRow, cellColumn)) return null;
        return getOppHero(map.getCell(cellRow, cellColumn));
    }

    public void castAbility(int heroId, AbilityName abilityName, int targetCellRow, int targetCellColumn)
    {
        /* TODO */
    }

    public void castAbility(int heroId, AbilityName abilityName, Cell targetCell)
    {
        castAbility(heroId, abilityName, targetCell.getRow(), targetCell.getColumn());
    }

    public void castAbility(Hero hero, AbilityName abilityName, Cell targetCell)
    {
        castAbility(hero.getId(), abilityName, targetCell.getRow(), targetCell.getColumn());
    }

    public void castAbility(Hero hero, AbilityName abilityName, int targetCellRow, int targetCellColumn)
    {
        castAbility(hero.getId(), abilityName, targetCellRow, targetCellColumn);
    }

    public void castAbility(int heroId, Ability ability, Cell targetCell)
    {
        castAbility(heroId, ability.getAbilityConstants().getName(), targetCell.getRow(), targetCell.getColumn());
    }

    public void castAbility(int heroId, Ability ability, int targetCellRow, int targetCellColumn)
    {
        castAbility(heroId, ability.getAbilityConstants().getName(), targetCellRow, targetCellColumn);
    }

    public void castAbility(Hero hero, Ability ability, Cell targetCell)
    {
        castAbility(hero.getId(), ability.getAbilityConstants().getName(), targetCell.getRow(), targetCell.getColumn());
    }

    public void castAbility(Hero hero, Ability ability, int targetCellRow, int targetCellColumn)
    {
        castAbility(hero.getId(), ability.getAbilityConstants().getName(), targetCellRow, targetCellColumn);
    }

    public void moveHero(int heroId, Direction[] directions)
    {
        /* TODO */
    }

    public void moveHero(Hero hero, Direction[] directions)
    {
        moveHero(hero.getId(), directions);
    }

    /* TODO */// moveHero direction
    public void pickHero(HeroName heroName)
    {
        /* TODO */
    }

    private boolean isInMap(int cellRow, int cellColumn)
    {
        return cellRow >= 0 && cellColumn >= 0 && cellRow < map.getRowNum() && cellColumn < map.getColumnNum();
    }

    public boolean isAccessible(int cellRow, int cellColumn)
    {
        if (!isInMap(cellRow, cellColumn))
            return false;
        return !map.getCell(cellRow, cellColumn).isWall();
    }

    private Cell getNextCell(Cell cell, Direction direction)
    {
        switch (direction)
        {
            case UP:
                if (isAccessible(cell.getRow() - 1, cell.getColumn()))
                    return map.getCell(cell.getRow() - 1, cell.getColumn());
                else
                    return null;
            case DOWN:
                if (isAccessible(cell.getRow() + 1, cell.getColumn()))
                    return map.getCell(cell.getRow() + 1, cell.getColumn());
                else
                    return null;
            case LEFT:
                if (isAccessible(cell.getRow(), cell.getColumn() - 1))
                    return map.getCell(cell.getRow(), cell.getColumn() - 1);
                else
                    return null;
            case RIGHT:
                if (isAccessible(cell.getRow(), cell.getColumn() + 1))
                    return map.getCell(cell.getRow(), cell.getColumn() + 1);
                else
                    return null;
        }
        return null; // never happens
    }

    /**
     * In case of start cell and end cell being the same cells, return empty array.
     *
     * @param startCell
     * @param endCell
     * @return
     */
    public Direction[] getPathMoveDirections(Cell startCell, Cell endCell)
    {
        if (startCell == endCell) return new Direction[0];
        if (startCell.isWall() || endCell.isWall()) return null;

        HashMap<Cell, Pair<Cell, Direction>> lastMoveInfo = new HashMap<>(); // saves parent cell and direction to go from parent cell to current cell
        Cell[] bfsQueue = new Cell[map.getRowNum() * map.getColumnNum() + 10];
        int queueHead = 0, queueTail = 0;

        lastMoveInfo.put(startCell, new Pair<Cell, Direction>(null, null));
        bfsQueue[queueTail++] = startCell;

        while (queueHead != queueTail)
        {
            Cell currentCell = bfsQueue[queueHead++];
            if (currentCell == endCell)
            {
                ArrayList<Direction> directions = new ArrayList<>();
                while (currentCell != startCell)
                {
                    directions.add(lastMoveInfo.get(currentCell).getSecond());
                    currentCell = lastMoveInfo.get(currentCell).getFirst();
                }
                Collections.reverse(directions);
                Direction[] directionsArray = new Direction[directions.size()];
                return directions.toArray(directionsArray);
            }
            for (Direction direction : Direction.values())
            {
                Cell nextCell = getNextCell(currentCell, direction);
                if (nextCell != null && !lastMoveInfo.containsKey(nextCell))
                {
                    lastMoveInfo.put(nextCell, new Pair<>(currentCell, direction));
                    bfsQueue[queueTail++] = nextCell;
                }
            }
        }
        return null;
    }


    public Direction[] getPathMoveDirections(int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn)) return null;
        return getPathMoveDirections(map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    public boolean isReachable(Cell startCell, Cell targetCell)
    {
        return getPathMoveDirections(startCell, targetCell) != null;
    }

    public boolean isReachable(int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn))
            return false;
        return isReachable(map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    /* TODO */// Mahdavi Check

    /**
     * Get all the cells that collide with the ray line in at least one non corner point, before reaching a wall.
     * If it hits a wall cell just in the corner, it would also stop too.
     *
     * @param startCell
     * @param targetCell
     * @return
     */
    private Cell[] getRayCells(Cell startCell, Cell targetCell)
    {
        ArrayList<Cell> path = new ArrayList<>();
        dfs(startCell, startCell, targetCell, new HashMap<>(), path);
        Cell[] pathArray = new Cell[path.size()];
        return path.toArray(pathArray);
    }

    private void dfs(Cell currentCell, Cell startCell, Cell targetCell, HashMap<Cell, Boolean> isSeen, ArrayList<Cell> path)
    {
        isSeen.put(currentCell, true);
        path.add(currentCell);
        for (Direction direction : Direction.values())
        {
            Cell nextCell = getNextCell(currentCell, direction);
            if (nextCell != null && !isSeen.containsKey(nextCell))
            {
                int collisionState = squareCollision(startCell, targetCell, nextCell);

                if ((collisionState == 0 || collisionState == 1) && nextCell.isWall())
                    return;
                if (collisionState == 1)
                {
                    dfs(nextCell, startCell, targetCell, isSeen, path);
                    return;
                }
            }
        }
        for (int dRow = -1; dRow <= 1; dRow += 2)
            for (int dColumn = -1; dColumn <= 1; dColumn += 2)
            {
                int newRow = currentCell.getRow() + dRow;
                int newColumn = currentCell.getColumn() + dColumn;
                if (!isInMap(newRow, newColumn)) continue;
                Cell nextCell = map.getCell(newRow, newColumn);
                if (!isSeen.containsKey(nextCell))
                {
                    int collisionState = squareCollision(startCell, targetCell, nextCell);

                    if (collisionState == 1 && nextCell.isWall())
                        return;
                    if (collisionState == 1)
                    {
                        dfs(nextCell, startCell, targetCell, isSeen, path);
                    }
                }
            }
    }

    /**
     * Checks the state of collision between the start cell to target cell line and cell square.
     * -1 -> doesn't pass through square at all
     * 0 -> passes through just one corner
     * 1 -> passes through the square
     *
     * @param startCell
     * @param targetCell
     * @param cell
     * @return
     */
    private int squareCollision(Cell startCell, Cell targetCell, Cell cell)
    {
        boolean hasNegative = false, hasPositive = false, hasZero = false;
        for (int row = 2 * cell.getRow(); row <= 2 * (cell.getRow() + 1); row += 2)
            for (int column = 2 * cell.getColumn(); column <= 2 * (cell.getColumn() + 1); column += 2)
            {
                int crossProduct = crossProduct(2 * startCell.getRow() + 1, 2 * startCell.getColumn() + 1,
                        2 * targetCell.getRow() + 1, 2 * targetCell.getColumn() + 1, row, column);
                if (crossProduct < 0) hasNegative = true;
                else if (crossProduct > 0) hasPositive = true;
                else hasZero = true;
            }
        if (hasNegative && hasPositive) return 1;
        if (hasZero) return 0;
        return -1;
    }

    /**
     * This method calculates the cross product.
     * negative return value -> point1-point2 line is on the left side of point1-point3 line
     * zero return value -> the three points lie on the same line
     * positive return value -> point1-point2 line is on the right side of point1-point3 line
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x3
     * @param y3
     * @return
     */
    private int crossProduct(int x1, int y1, int x2, int y2, int x3, int y3)
    {
        return (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
    }

    public int manhattanDistance(Cell startCell, Cell endCell)
    {
        return Math.abs(startCell.getRow() - endCell.getRow()) + Math.abs(startCell.getColumn() - endCell.getColumn());
    }

    public int manhattanDistance(int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn)) return -1;
        return manhattanDistance(map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    /**
     * If start cell is a wall we would return it as the impact point.
     *
     * @param abilityName
     * @param startCell
     * @param targetCell
     * @return
     */
    public Cell getImpactCell(AbilityName abilityName, Cell startCell, Cell targetCell)/* TODO */// add abilityName
    {
        if (startCell.isWall())
            return startCell;
        if (startCell == targetCell)
            return startCell;

        AbilityConstants currentAbilityConstants = null;
        for (AbilityConstants abilityConstants : this.abilityConstants) /* TODO */// naming is shit
        {
            if (abilityConstants.getName() == abilityName)
            {
                currentAbilityConstants = abilityConstants;
                break;
            }
        }

        Cell[] rayCells = getRayCells(startCell, targetCell);
        Cell lastValidCell = null; // would not remain null cause range is not zero
        for (Cell cell : rayCells)
        {
            if (getOppHero(cell) != null)
                return cell;
            if (manhattanDistance(startCell, cell) > currentAbilityConstants.getRange())
                return lastValidCell;
            lastValidCell = cell;
        }
        return lastValidCell;
    }

    public Cell getImpactCell(AbilityName abilityName, int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn)) return null;
        return getImpactCell(abilityName, map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    public Cell getImpactCell(Ability ability, Cell startCell, Cell targetCell)
    {
        return getImpactCell(ability.abilityConstants.getName(), startCell, targetCell);
    }

    public Cell getImpactCell(Ability ability, int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn)) return null;
        return getImpactCell(ability.abilityConstants.getName(), map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    public boolean isInVision(Cell startCell, Cell endCell)
    {
        if (startCell.isWall() || endCell.isWall())
            return false;
        Cell[] rayCells = getRayCells(startCell, endCell);
        Cell lastCell = rayCells[rayCells.length - 1];
        return lastCell == endCell;
    }


    public boolean isInVision(int startCellRow, int startCellColumn, int endCellRow, int endCellColumn)
    {
        if (!isInMap(startCellRow, startCellColumn) || !isInMap(endCellRow, endCellColumn)) return false;
        return isInVision(map.getCell(startCellRow, startCellColumn), map.getCell(endCellRow, endCellColumn));
    }

    public Hero[] getMyHeroes()
    {
        return myHeroes;
    }

    public void setMyHeroes(Hero[] myHeroes)
    {
        this.myHeroes = myHeroes;
    }

    public Hero[] getOppHeroes()
    {
        return oppHeroes;
    }

    public void setOppHeroes(Hero[] oppHeroes)
    {
        this.oppHeroes = oppHeroes;
    }

    public Hero[] getMyDeadHeroes()
    {
        return myDeadHeroes;
    }

    public void setMyDeadHeroes(Hero[] myDeadHeroes)
    {
        this.myDeadHeroes = myDeadHeroes;
    }

    public Hero[] getOppDeadHeroes()
    {
        return oppDeadHeroes;
    }

    public void setOppDeadHeroes(Hero[] oppDeadHeroes)
    {
        this.oppDeadHeroes = oppDeadHeroes;
    }

    public Map getMap()
    {
        return map;
    }

    public void setMap(Map map)
    {
        this.map = map;
    }

    public GameConstants getGameConstants()
    {
        return gameConstants;
    }

    public void setGameConstants(GameConstants gameConstants)
    {
        this.gameConstants = gameConstants;
    }

    public AbilityConstants[] getAbilityConstants()
    {
        return abilityConstants;
    }

    public void setAbilityConstants(AbilityConstants[] abilityConstants)
    {
        this.abilityConstants = abilityConstants;
    }

    public HeroConstants[] getHeroConstants()
    {
        return heroConstants;
    }

    public void setHeroConstants(HeroConstants[] heroConstants)
    {
        this.heroConstants = heroConstants;
    }

    public Cell[] getBrokenWalls()
    {
        return brokenWalls;
    }

    public void setBrokenWalls(Cell[] brokenWalls)
    {
        this.brokenWalls = brokenWalls;
    }

    public Cell[] getCreatedWalls()
    {
        return createdWalls;
    }

    public void setCreatedWalls(Cell[] createdWalls)
    {
        this.createdWalls = createdWalls;
    }

    public int getAP()
    {
        return AP;
    }

    public void setAP(int AP)
    {
        this.AP = AP;
    }

    public int getScore()
    {
        return score;
    }

    public void setScore(int score)
    {
        this.score = score;
    }
}