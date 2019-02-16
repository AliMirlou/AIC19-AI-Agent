package client;

import client.model.*;

import java.util.Arrays;
import java.util.Comparator;

public class AI {
	public void preProcess(World world) {
	}

	public void pickTurn(World world) {
		world.pickHero(HeroName.values()[world.getCurrentTurn()]);
	}

	public void moveTurn(World world) {
		Hero[] heroes = world.getMyHeroes();
		Cell[] positions = Arrays.stream(heroes).map(Hero::getCurrentCell).toArray(Cell[]::new);

		for (Hero hero : heroes) {
			Cell currentPosition = hero.getCurrentCell();
			// If in objective zone, hold ground
			if (currentPosition.isInObjectiveZone()) continue;

			// Otherwise try to reach the nearest objective zone cell
			Cell[] objectiveZone = Arrays.stream(world.getMap().getObjectiveZone()).sorted(Comparator.comparingInt(o -> world.manhattanDistance(currentPosition, o))).toArray(Cell[]::new);
			// Try to select an objective zone and move toward it
			for (Cell objective : objectiveZone) {
				// Pass my hero positions as blocked cells so the path won't be blocked
				Direction[] toTheObjective = world.getPathMoveDirections(currentPosition, objective, positions);
				if (toTheObjective.length != 0)
					world.moveHero(hero.getId(), toTheObjective[0]);  // TODO heroes should get closer to each other to cast defensive abilities
			}
		}
	}

	public void actionTurn(World world) {
		Hero[] heroes = Arrays.stream(world.getMyHeroes()).sorted(Comparator.comparingInt(Hero::getCurrentHP)).toArray(Hero[]::new);
		Hero[] visibleEnemies = Arrays.stream(world.getOppHeroes()).filter(hero -> hero.getCurrentCell().isInVision()).sorted(Comparator.comparingInt(Hero::getCurrentHP)).toArray(Hero[]::new);
		heroes:
		for (Hero hero : heroes) {
			Cell myPosition = hero.getCurrentCell();
			switch (hero.getName()) {
				case HEALER:
					// Heal if there is a damaged hero
					Ability heal = hero.getAbility(AbilityName.HEALER_HEAL);
					for (Hero otherHero : heroes) {
						// Heal the hero if lost HP is higher than heal power (so it won't be wasted)
						if (otherHero.getMaxHP() - otherHero.getCurrentHP() >= heal.getPower()) {
							Cell otherHeroPosition = otherHero.getCurrentCell();
							if (world.manhattanDistance(myPosition, otherHeroPosition) <= heal.getRange()) {
								world.castAbility(hero.getId(), heal.getName(), otherHeroPosition.getRow(), otherHeroPosition.getColumn());
								continue heroes;
							}
						}
					}
			}
			// Attack with whatever you got
			for (Ability ability : hero.getOffensiveAbilities()) {
				if (ability.isReady()) {
					for (Hero enemy : visibleEnemies) {
						Cell enemyPosition = enemy.getCurrentCell();
						if (world.isInVision(hero.getCurrentCell(), enemy.getCurrentCell())) {
							if (world.getAbilityTargets(ability.getName(), myPosition, enemyPosition).length != 0) {
								world.castAbility(hero.getId(), ability.getName(), enemyPosition.getRow(), enemyPosition.getColumn());
								continue heroes;
							}
						}
					}
				}
			}
		}
	}
}
