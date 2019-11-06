package mint.inference.evo;

import mint.inference.gp.fitness.Fitness;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * Implements the Tournament Selection strategy for GP. Partition the population
 * of individuals into groups (of a given size). For each group, select the best
 * ones.
 *
 * Created by neilwalkinshaw on 05/03/15.
 */

public abstract class TournamentSelection implements Selection {

	protected Map<Chromosome, Double> fitnessCache;
	protected Map<Chromosome, String> summaryCache;
	protected List<Chromosome> totalPopulation;
	protected List<Chromosome> elite;
	protected double bestFitness;
	protected int maxDepth;
	protected final int maxElite = 3;
	protected double averageScore;

	public double getAverageScore() {
		return averageScore;
	}

	public TournamentSelection(List<Chromosome> totalPopulation, int maxDepth) {
		this.summaryCache = new HashMap<Chromosome, String>();
		this.elite = new ArrayList<Chromosome>();
		this.totalPopulation = totalPopulation;
		this.bestFitness = Double.MAX_VALUE;
		this.maxDepth = maxDepth;
		this.fitnessCache = new HashMap<Chromosome, Double>();
	}

	public double getBestFitness() {
		assert(bestFitness >= 0);
		return bestFitness;
	}

	public String getBestFitnessSummary() {
		if (elite.isEmpty())
			return "";
		return summaryCache.get(elite.get(0));
	}

	public List<Chromosome> select(GPConfiguration config) {
		List<List<Chromosome>> partitions = partition(config.getTournamentSize(), config.getPopulationSize());
		List<Chromosome> best = bestIndividuals(partitions);
		assert (best.size() == config.getPopulationSize());
		bestScoresAndElites(best);
		return best;
	}

	public double computeFitness(Chromosome toEvaluate) throws InterruptedException {
		if (fitnessCache.containsKey(toEvaluate))
			return fitnessCache.get(toEvaluate);
		else {
			Fitness f = getFitness(toEvaluate);
			double fitness = f.call();
			assert(fitness >= 0);
			fitnessCache.put(toEvaluate, fitness);
			summaryCache.put(toEvaluate, f.getFitnessSummary());
			return fitness;
		}
	}

	public abstract Fitness getFitness(Chromosome toEvaluate);

	protected List<List<Chromosome>> partition(int tournamentSize, int number) {
		List<List<Chromosome>> best = new ArrayList<List<Chromosome>>();
		int counter = 0;
		while (best.size() < number) {

			Collections.shuffle(totalPopulation);
			List<Chromosome> pop = new ArrayList<Chromosome>();
			if (counter < elite.size()) {
				pop.add(elite.get(counter));
			}
			for (int i = pop.size(); i < tournamentSize; i++) {
				pop.add(totalPopulation.get(i).copy());
			}
			best.add(pop);
			counter++;
		}
		return best;
	}

	protected List<Chromosome> bestIndividuals(List<List<Chromosome>> partitions) {
		List<Chromosome> bestIndividuals = new ArrayList<Chromosome>();
		for (List<Chromosome> p : partitions) {
			bestIndividuals.add(evaluatePopulation(p));
		}
		return bestIndividuals;
	}

	protected abstract Comparator<Chromosome> getComparator();

	protected void bestScoresAndElites(List<Chromosome> population) {
		Collections.sort(population, getComparator());
		if (population.isEmpty())
			return;
		bestFitness = fitnessCache.get(population.get(0));
		assert(bestFitness >= 0);
		elite.clear();
		elite.add(population.get(0));
		for (int i = 1; i < population.size() && elite.size() < maxElite; i++) {
			if (!elite.contains(population.get(i))) {
				elite.add(population.get(i));
			}
		}
	}

	protected Chromosome evaluatePopulation(Collection<Chromosome> population) {
		assert (!population.isEmpty());
		double bestScore = Double.MAX_VALUE;
		Chromosome best = null;
		Map<Future, Chromosome> solMap = new HashMap<Future, Chromosome>();
		Set<Future<Double>> set = new HashSet<Future<Double>>();
		ExecutorService pool = Executors.newFixedThreadPool(4);
		Fitness fitness = null;
		double totalScore = 0D;
		try {
			for (Chromosome node : population) {
				fitness = getFitness(node);
				Future<Double> future = pool.submit(fitness);
				solMap.put(future, node);
				set.add(future);
			}
			for (Future<Double> sol : set) {
				double score = 0D;
				try {
					score = sol.get(5000000, TimeUnit.MILLISECONDS);
					processResult(solMap, sol, score, fitness);
				} catch (Exception ex) {
					ex.printStackTrace();
					score = 1000D;
					processResult(solMap, sol, score, fitness);
				}
				if (score < bestScore) {
					bestScore = score;
					best = solMap.get(sol);
				} else if (best == null)
					best = solMap.get(sol);
				totalScore += score;
				sol.cancel(true);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			pool.shutdownNow();
		}
		averageScore = population.size() / totalScore;
		return best.copy();
	}

	protected void processResult(Map<Future, Chromosome> solMap, Future<Double> sol, double score, Fitness fitness) {
		fitnessCache.put(solMap.get(sol), score);
	}

	public Collection<Chromosome> getElites() {
		return elite;
	}

}
