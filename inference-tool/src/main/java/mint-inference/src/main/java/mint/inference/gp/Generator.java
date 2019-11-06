package mint.inference.gp;

import mint.inference.evo.Chromosome;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NonTerminal;
import mint.inference.gp.tree.nonterminals.booleans.RootBoolean;
import mint.inference.gp.tree.nonterminals.doubles.RootDouble;
import mint.inference.gp.tree.nonterminals.integers.RootInteger;
import mint.inference.gp.tree.nonterminals.lists.RootListNonTerminal;
import mint.inference.gp.tree.nonterminals.strings.AssignmentOperator;
import mint.inference.gp.tree.nonterminals.strings.RootString;
import mint.inference.gp.tree.terminals.VariableTerminal;
import mint.tracedata.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 * A random expression generator - generating random tree-shaped expressions for
 * evaluation in a GP context.
 *
 * Created by neilwalkinshaw on 04/03/15.
 */
public class Generator {

	protected Random rand;
	protected List<NonTerminal<?>> dFunctions;
	protected List<NonTerminal<?>> iFunctions;
	protected List<VariableTerminal<?>> dTerminals;
	protected List<VariableTerminal<?>> iTerminals;
	protected List<NonTerminal<?>> sFunctions;
	protected List<VariableTerminal<?>> sTerminals;
	protected List<NonTerminal<?>> bFunctions;
	protected List<VariableTerminal<?>> bTerminals;
	protected AssignmentOperator aop;
	protected int listLength = 0;

	public void setListLength(int length) {
		listLength = length;
	}

	public Generator(Random r) {
		rand = r;
		dFunctions = new ArrayList<NonTerminal<?>>();
		iFunctions = new ArrayList<NonTerminal<?>>();
		dTerminals = new ArrayList<VariableTerminal<?>>();
		iTerminals = new ArrayList<VariableTerminal<?>>();
		sTerminals = new ArrayList<VariableTerminal<?>>();
		sFunctions = new ArrayList<NonTerminal<?>>();
		bTerminals = new ArrayList<VariableTerminal<?>>();
		bFunctions = new ArrayList<NonTerminal<?>>();
		aop = new AssignmentOperator();
	}

	public Random getRandom() {
		return rand;
	}

	public void setDoubleFunctions(List<NonTerminal<?>> doubleFunctions) {
		dFunctions = doubleFunctions;
	}

	public void setIntegerFunctions(List<NonTerminal<?>> intFunctions) {
		iFunctions = intFunctions;
	}

	public void setDoubleTerminals(List<VariableTerminal<?>> doubleTerms) {
		dTerminals = doubleTerms;
	}

	public void setIntegerTerminals(List<VariableTerminal<?>> intFunctions) {
		iTerminals = intFunctions;
	}

	public void setStringTerminals(List<VariableTerminal<?>> sTerms) {
		sTerminals = sTerms;
	}

	public void setStringFunctions(List<NonTerminal<?>> sFunctions) {
		this.sFunctions = sFunctions;
	}

	public void setBooleanTerminals(List<VariableTerminal<?>> bTerms) {
		bTerminals = bTerms;
	}

	public void setBooleanFunctions(List<NonTerminal<?>> bFunctions) {
		this.bFunctions = bFunctions;
	}

	/*
	 * public Chromosome generateRandomExpression(int maxD, List<NonTerminal<?>>
	 * nonTerms, List<VariableTerminal<?>> terms){ if(nonTerms.isEmpty()){ return
	 * selectRandomTerminal(terms); } if((maxD < 2 || rand.nextDouble() <
	 * threshold())&&!terms.isEmpty()){ return selectRandomTerminal(terms); } else
	 * return selectRandomNonTerminal(nonTerms, maxD); }
	 */

	public Chromosome generateRandomExpression(int maxD, List<NonTerminal<?>> nonTerms,
			List<VariableTerminal<?>> terms) {
		if (nonTerms.isEmpty() || maxD < 2) {
			return selectRandomTerminal(terms);
		} else {
			List nodes = new ArrayList();
			nodes.addAll(terms);
			nodes.addAll(nonTerms);
			Collections.shuffle(nodes);
			Object toBeAdded = nodes.get(0);
			if (terms.contains(toBeAdded)) {
				VariableTerminal<?> selected = (VariableTerminal<?>) toBeAdded;
				return selected.copy();
			} else {
				NonTerminal<?> selected = (NonTerminal<?>) toBeAdded;
				return selected.createInstance(this, maxD - 1);
			}
		}

	}

	public List<Chromosome> generateDoublePopulation(int size, int maxD) {
		List<Chromosome> population = new ArrayList<Chromosome>();
		for (int i = 0; i < size; i++) {
			RootDouble rd = new RootDouble();
			Chromosome individual = rd.createInstance(this, maxD).simp();
			while (population.contains(individual)) {
				individual = rd.createInstance(this, maxD).simp();
			}
			population.add(individual);
		}
		return population;
	}

	public List<Chromosome> generateBooleanPopulation(int size, int maxD) {
		List<Chromosome> population = new ArrayList<Chromosome>();
		for (int i = 0; i < size; i++) {
			RootBoolean rb = new RootBoolean();
			population.add(rb.createInstance(this, maxD).simp());
		}
		return population;
	}

	public List<Chromosome> generateIntegerPopulation(int size, int maxD) {
		List<Chromosome> population = new ArrayList<Chromosome>();
		List<String> stringPop = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			RootInteger ri = new RootInteger();
			Chromosome individual = ri.createInstance(this, maxD).simp();
			while (stringPop.contains(individual.toString())) {
				individual = ri.createInstance(this, maxD).simp();
			}
			population.add(individual);
			stringPop.add(individual.toString());
		}
		return population;
	}

	public List<Chromosome> generateStringPopulation(int size, int maxD) {
		List<Chromosome> population = new ArrayList<Chromosome>();
		for (int i = 0; i < size; i++) {
			RootString rs = new RootString();
			population.add(rs.createInstance(this, maxD));
		}
		return population;
	}

	/*
	 * public Chromosome generate(String type, int maxD){ if(type.equals("String"))
	 * return generateRandomStringExpression(maxD); else if(type.equals("Double"))
	 * return generateRandomDoubleExpression(maxD); else if(type.equals("Integer"))
	 * return generateRandomIntegerExpression(maxD); else if(type.equals("Boolean"))
	 * return generateRandomBooleanExpression(maxD); else return null; }
	 */

	public Node<DoubleVariableAssignment> generateRandomDoubleExpression(int maxD) {
		return (Node<DoubleVariableAssignment>) generateRandomExpression(maxD, dFunctions, dTerminals);
	}

	public Node<StringVariableAssignment> generateRandomStringExpression(int maxD) {
		return (Node<StringVariableAssignment>) generateRandomExpression(maxD, sFunctions, sTerminals);
	}

	public Node<IntegerVariableAssignment> generateRandomIntegerExpression(int maxD) {
		return (Node<IntegerVariableAssignment>) generateRandomExpression(maxD, iFunctions, iTerminals);
	}

	public Node<BooleanVariableAssignment> generateRandomBooleanExpression(int maxD) {
		return (Node<BooleanVariableAssignment>) generateRandomExpression(maxD, bFunctions, bTerminals);
	}

	private double threshold() {
		double numTerminals = dTerminals.size() + iTerminals.size();
		double numFunctions = dFunctions.size() + iFunctions.size();
		if (numTerminals == 0 && numFunctions == 0)
			return 0;
		return numTerminals / (numTerminals + numFunctions);
	}

	public NonTerminal<StringVariableAssignment> generateAssignment() {
		return aop.createInstance(this, 0);
	}

	/*
	 * private Chromosome selectRandomNonTerminal(List<NonTerminal<?>> nodes, int
	 * depth) { int index = rand.nextInt(nodes.size()); NonTerminal<?> selected =
	 * nodes.get(index); return selected.createInstance(this,depth-1); }
	 */

	public Node<? extends VariableAssignment<?>> selectRandomTerminal(List<VariableTerminal<?>> nodes) {
		int index = rand.nextInt(nodes.size());
		VariableTerminal<?> selected = nodes.get(index);

		return selected.copy();
	}

	public List<Chromosome> generateListPopulation(int size, int maxD, String typeString) {
		List<Chromosome> population = new ArrayList<Chromosome>();
		for (int i = 0; i < size; i++) {
			RootListNonTerminal rs = new RootListNonTerminal(typeString);
			population.add(rs.createInstance(this, maxD));
		}
		return population;
	}
}
