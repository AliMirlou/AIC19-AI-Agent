package client;

import client.model.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class AI {
	private int phase = 0;
	private int AP = -1;
	private Cell[] bestDestinations;
	private HashMap<Integer, Cell> destinations;  // Keys are hero IDs and values are the current destination of hero
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

	private boolean isInBadDistance(Cell target, Cell[] otherCells, World world) {
		for (Cell cell : otherCells) {
			if (cell == null) continue;
			int distance = world.manhattanDistance(cell, target);
			if (distance < 4 || distance > 4) return true;
		}
		return false;
	}

	private boolean isWorthDodging(Hero hero, Cell destination, int distance, Direction[] toTheObjective, Cell[] positions, World world) {
		Map map = world.getMap();
		int ID = hero.getId();
		Cell currentPosition = hero.getCurrentCell();
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
				return true;
			}
		}
		return false;
	}

	public void preProcess(World world) {
		destinations = new HashMap<>();
		dodgeInsteadOfMove = new HashMap<>();

		// Find 4 best objective cells for heroes to stand
		Cell[] objectiveZone = world.getMap().getObjectiveZone(), temp = new Cell[4];
		int numOfObjectives = objectiveZone.length;
		Cell nearest = objectiveZone[numOfObjectives / 2], compare = world.getMap().getMyRespawnZone()[0];

		for (int i1 = 0; i1 < numOfObjectives; ++i1) {
			temp[0] = objectiveZone[i1];
			for (int i2 = i1 + 1; i2 < numOfObjectives; ++i2) {
				if (isInBadDistance(objectiveZone[i2], temp, world)) continue;
				temp[1] = objectiveZone[i2];
				for (int i3 = i2 + 1; i3 < numOfObjectives; ++i3) {
					if (isInBadDistance(objectiveZone[i3], temp, world)) continue;
					temp[2] = objectiveZone[i3];
					for (int i4 = i3 + 1; i4 < numOfObjectives; ++i4) {
						if (isInBadDistance(objectiveZone[i4], temp, world)) continue;
						temp[3] = objectiveZone[i4];
						temp = Arrays.stream(temp).sorted(Comparator.comparingInt(cell -> world.manhattanDistance(compare, cell))).toArray(Cell[]::new);
						if (world.manhattanDistance(compare, temp[0]) < world.manhattanDistance(compare, nearest)) {
							bestDestinations = temp.clone();
							nearest = temp[0];
						}
						temp[3] = null;
					}
					temp[2] = null;
				}
				temp[1] = null;
			}
			temp[0] = null;
		}
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

		if (destinations.isEmpty() && bestDestinations != null && bestDestinations.length == 4) {
			int dummyIndex = 2;
			for (Hero hero : world.getMyHeroes()) {
				Cell destination;
				switch (hero.getName()) {
					case HEALER:
						destination = bestDestinations[0];
						break;
					case SENTRY:
						destination = bestDestinations[1];
						break;
					default:
						destination = bestDestinations[dummyIndex++];
				}
				destinations.put(hero.getId(), destination);
			}
		}

		// Decide hero movements in order of their distance from their destinations
		Hero[] heroes = Arrays.stream(world.getMyHeroes()).filter(hero -> hero.getCurrentHP() != 0).sorted(Comparator.comparingInt(
				hero -> destinations.containsKey(hero.getId()) ? world.manhattanDistance(hero.getCurrentCell(), destinations.get(hero.getId())) : Integer.MAX_VALUE
		)).toArray(Hero[]::new);
		Cell[] positions = Arrays.stream(heroes).map(Hero::getCurrentCell).toArray(Cell[]::new);
		AP = (AP == -1) ? world.getAP() : AP;
		Map map = world.getMap();

		heroes:
		for (int i = 0; i < heroes.length; ++i) {
			Hero hero = heroes[i];
			int ID = hero.getId();
			if (AP < hero.getMoveAPCost() || dodgeInsteadOfMove.containsKey(ID)) continue;
			Cell currentPosition = positions[i];

			// TODO fall back when health is low (get closer to Healer?)

			if (destinations.containsKey(ID)) {
				Cell destination = destinations.get(ID);
				if (!currentPosition.equals(destination)) {
					Direction[] toTheObjective = world.getPathMoveDirections(currentPosition, destination, positions);
					if (toTheObjective.length == 0) {  // FIXME have no idea why this happens
						System.out.println("ERROR in turn " + world.getCurrentTurn() + " and phase " + phase);
						System.out.println(hero.getName() + " should be in (" + currentPosition.getRow() + ", " + currentPosition.getColumn() + ")");
						System.out.println(hero.getName() + " is actually in (" + hero.getCurrentCell().getRow() + ", " + hero.getCurrentCell().getColumn() + ")");
						System.out.println("Destination is (" + destination.getRow() + ", " + destination.getColumn() + ")");
						System.out.println();
						continue;
					}

					if (isWorthDodging(hero, destination, world.manhattanDistance(currentPosition, destination), toTheObjective, positions, world)) continue;
					world.moveHero(ID, toTheObjective[0]);

					// Update AP
					AP -= hero.getMoveAPCost();

					// Update hero position
					positions[i] = getNewPosition(currentPosition, toTheObjective[0], map);
				}
				continue;
			}

			// Otherwise try to reach the nearest objective zone cell
			Pair[] objectiveZone = Arrays.stream(map.getObjectiveZone()).map(cell -> new Pair<>(cell, world.manhattanDistance(currentPosition, cell))).sorted(Comparator.comparingInt(Pair::getSecond)).toArray(Pair[]::new);

			// Try to select an objective zone and move toward it
			for (Pair objective : objectiveZone) {
				Cell destination = (Cell) objective.getFirst();
				int distance = (Integer) objective.getSecond();

				// Pass my hero positions as blocked cells so the path won't be blocked
				Direction[] toTheObjective = world.getPathMoveDirections(currentPosition, destination, positions);
				if (toTheObjective.length != 0) {
					destinations.put(ID, destination);

					// Check if it is worth to not to move and instead dodge at action phase
					if (isWorthDodging(hero, destination, distance, toTheObjective, positions, world)) continue heroes;
					world.moveHero(ID, toTheObjective[0]);

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
		var enemyHealths = Arrays.stream(visibleEnemies).collect(Collectors.toMap(o -> o, Hero::getCurrentHP));

		// Perform the decided dodges first
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

			// TODO use dodge when attack is predicted

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
				AbilityName abilityName = ability.getName();
				for (Hero enemy : visibleEnemies) {
					if (enemyHealths.get(enemy) <= 0) continue;
					Cell enemyPosition = enemy.getCurrentCell();

					// Non-lobbing abilities require the target cell to be visible by the caster
					if (!ability.isLobbing() && !world.isInVision(myPosition, enemyPosition)) continue;

					// Check if it hits anyone
					Hero[] impactedEnemies = world.getAbilityTargets(abilityName, myPosition, enemyPosition);
					if (impactedEnemies.length != 0) {
						world.castAbility(ID, abilityName, enemyPosition.getRow(), enemyPosition.getColumn());

						// Update enemy healths
						int power = ability.getPower();
						for (Hero enemy2 : impactedEnemies)
							enemyHealths.put(enemy2, enemyHealths.get(enemy2) - power);

						continue heroes;
					} else {
						// TODO get closer to target to be able to cast ability
					}
				}
			}
		}

		AP = -1;
	}
}
