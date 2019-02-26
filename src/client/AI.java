package client;

import client.model.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class AI {
	private int phase = 0;
	private int AP = -1;
	private HashMap<Integer, Cell> dodgeInsteadOfMove;  // Keys are hero IDs and values are the destination of dodge

	private Cell getNewPosition(Cell cell, Direction direction, Map map) {
		int rowChange = 0, columnChange = 0;
		switch (direction) {
			case DOWN:
				++rowChange;
				break;
			case UP:
				--rowChange;
				break;
			case LEFT:
				--columnChange;
				break;
			default:
				++columnChange;
		}
		return map.getCell(cell.getRow() + rowChange, cell.getColumn() + columnChange);
	}

	public void preProcess(World world) {
		dodgeInsteadOfMove = new HashMap<>();
	}

	public void pickTurn(World world) {
		switch (world.getCurrentTurn()) {
			case 0:
				world.pickHero(HeroName.HEALER);
				break;
			case 1:
				world.pickHero(HeroName.BLASTER);
				break;
			case 2:
				world.pickHero(HeroName.SENTRY);
				break;
			case 3:
				world.pickHero(HeroName.BLASTER);
				break;
		}
	}

	public void moveTurn(World world) {
		++phase;
		Hero[] heroes = world.getMyHeroes();
		Cell[] positions = Arrays.stream(heroes).map(Hero::getCurrentCell).toArray(Cell[]::new);
		AP = (AP == -1) ? world.getAP() : AP;
		Map map = world.getMap();

		heroes:
		for (int i = 0; i < heroes.length; ++i) {
			Hero hero = heroes[i];
			int ID = hero.getId();
			if (hero.getCurrentHP() == 0 || AP < hero.getMoveAPCost() || dodgeInsteadOfMove.containsKey(ID)) continue;
			Cell currentPosition = positions[i];

			// If in objective zone, hold ground
			if (currentPosition.isInObjectiveZone()) continue;

			// Otherwise try to reach the nearest objective zone cell
			Pair[] objectiveZone = Arrays.stream(map.getObjectiveZone()).map(cell -> new Pair<>(cell, world.manhattanDistance(currentPosition, cell))).sorted(Comparator.comparingInt(Pair::getSecond)).toArray(Pair[]::new);

			// Try to select an objective zone and move toward it
			for (Pair objective : objectiveZone) {  // FIXME spread around the zone
				Cell destination = (Cell) objective.getFirst();
				int distance = (Integer) objective.getSecond();

				// Pass my hero positions as blocked cells so the path won't be blocked
				Direction[] toTheObjective = world.getPathMoveDirections(currentPosition, destination, positions);
				if (toTheObjective.length != 0) {
					// Check if it is worth to not to move and instead dodge at action phase
					Ability dodge = hero.getDodgeAbilities()[0];
					if (phase == 1 && AP >= dodge.getAPCost() && dodge.isReady() && toTheObjective.length - (7 - phase) >= distance - dodge.getRange()) {
						Cell nearer = currentPosition;
						for (int j = 0; j < dodge.getRange(); ++j) {
							Cell next = nearer;
							int left = distance;
							for (int k = 0; k < 4; ++k) {
								Cell temp = getNewPosition(nearer, Direction.values()[k], map);
								int leftTemp = world.manhattanDistance(destination, temp);
								if (leftTemp < left) {
									next = temp;
									left = leftTemp;
								}
							}
							nearer = next;
						}
						if (!nearer.isWall() && world.getPathMoveDirections(nearer, destination, positions).length < toTheObjective.length) {
							dodgeInsteadOfMove.put(ID, nearer);
							AP -= dodge.getAPCost();
							continue heroes;
						}
					}
					world.moveHero(ID, toTheObjective[0]);  // TODO heroes should get closer to each other to cast defensive abilities

					// Update AP
					AP -= hero.getMoveAPCost();

					// Update hero position
					positions[i] = getNewPosition(currentPosition, toTheObjective[0], map);

					continue heroes;
				}
			}
		}
	}

	public void actionTurn(World world) {
		phase = 0;
		Hero[] heroes = Arrays.stream(world.getMyHeroes()).sorted(Comparator.comparingInt(Hero::getCurrentHP)).toArray(Hero[]::new);
		Hero[] visibleEnemies = Arrays.stream(world.getOppHeroes()).filter(hero -> hero.getCurrentCell().isInVision()).sorted(Comparator.comparingInt(Hero::getCurrentHP)).toArray(Hero[]::new);

		// First perform the scheduled dodges
		for (int ID : dodgeInsteadOfMove.keySet()) {
			Cell dest = dodgeInsteadOfMove.get(ID);
			world.castAbility(ID, world.getHero(ID).getDodgeAbilities()[0].getName(), dest.getRow(), dest.getColumn());
		}

		heroes:
		for (Hero hero : heroes) {
			int ID = hero.getId();
			Cell myPosition = hero.getCurrentCell();

			// If hero has performed dodge earlier, then it can't perform any other abilities
			if (dodgeInsteadOfMove.containsKey(ID)) {
				dodgeInsteadOfMove.remove(ID);
				continue;
			}

			switch (hero.getName()) {
				case HEALER:
					// Heal if there is a damaged hero
					Ability heal = hero.getAbility(AbilityName.HEALER_HEAL);
					if (!heal.isReady()) break;
					for (Hero otherHero : heroes) {
						// Heal the other hero if lost HP is more than heal power (so it won't be wasted)
						if (otherHero.getMaxHP() - otherHero.getCurrentHP() >= heal.getPower()) {
							Cell otherHeroPosition = otherHero.getCurrentCell();
							if (world.manhattanDistance(myPosition, otherHeroPosition) <= heal.getRange()) {
								world.castAbility(ID, heal.getName(), otherHeroPosition.getRow(), otherHeroPosition.getColumn());
								continue heroes;
							}
						}
					}
				case GUARDIAN:  // TODO low range so they should get close to enemy... need support
			}

			// Attack with whatever you got
			for (Ability ability : hero.getOffensiveAbilities()) {
				if (!ability.isReady()) continue;
				for (Hero enemy : visibleEnemies) {
					Cell enemyPosition = enemy.getCurrentCell();
					if (world.isInVision(hero.getCurrentCell(), enemy.getCurrentCell())) {
						if (world.getAbilityTargets(ability.getName(), myPosition, enemyPosition).length != 0) {
							world.castAbility(ID, ability.getName(), enemyPosition.getRow(), enemyPosition.getColumn());
							continue heroes;
						}
					}
				}
			}
		}

		AP = -1;
	}
}
