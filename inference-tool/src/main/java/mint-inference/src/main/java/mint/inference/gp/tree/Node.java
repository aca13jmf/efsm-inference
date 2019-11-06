package mint.inference.gp.tree;

import mint.inference.evo.Chromosome;
import mint.inference.gp.CallableNodeExecutor;
import mint.inference.gp.Generator;
import mint.inference.gp.fitness.InvalidDistanceException;
import mint.inference.gp.fitness.SingleOutputBooleanFitness;
import mint.inference.gp.fitness.SingleOutputDoubleFitness;
import mint.inference.gp.fitness.SingleOutputFitness;
import mint.inference.gp.fitness.SingleOutputIntegerFitness;
import mint.inference.gp.fitness.SingleOutputListFitness;
import mint.inference.gp.fitness.SingleOutputStringFitness;
import mint.inference.gp.tree.nonterminals.booleans.RootBoolean;
import mint.inference.gp.tree.nonterminals.doubles.RootDouble;
import mint.inference.gp.tree.nonterminals.lists.RootListNonTerminal;
import mint.inference.gp.tree.nonterminals.strings.AssignmentOperator;
import mint.tracedata.types.VariableAssignment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;

import java.util.Map.Entry;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

/**
 * Represents a node in a GP tree.
 *
 * If a GP tree is to be associated with a memory, the setMemory method must
 * only be called after the tree has been completed.
 *
 * Created by neilwalkinshaw on 03/03/15.
 */
public abstract class Node<T extends VariableAssignment<?>> implements Chromosome {

	protected static int ids = 0;

	protected int id;

	protected AssignmentOperator def;

	protected NonTerminal<?> parent;

	protected Set<Object> vals = new HashSet<Object>();

	public Node() {
		id = ids++;
	}

	public NonTerminal<?> getParent() {
		return parent;
	}

	public AssignmentOperator getDef() {
		return def;
	}

	public void setDef(AssignmentOperator def) {
		this.def = def;
	}

	public abstract void simplify();

	public void reset() {
		for (Node<?> child : getChildren()) {
			child.reset();
		}
	}

	public abstract boolean accept(NodeVisitor visitor) throws InterruptedException;

	protected void setParent(NonTerminal<?> parent) {
		this.parent = parent;
	}

	public abstract List<Node<?>> getChildren();

	public abstract T evaluate() throws InterruptedException;

	public abstract Node<T> copy();

	public abstract void mutate(Generator g, int depth);

	public boolean swapWith(Node<?> alternative) {
		assert (!(this instanceof RootDouble));
		assert (!(this instanceof RootBoolean));
		assert (!(this instanceof RootListNonTerminal));
		if (parent == null) {
			return false;
		}
		if (!alternative.getType().equals(getType()))
			return false;
		int thisIndex = parent.getChildren().indexOf(this);
		parent.getChildren().set(thisIndex, alternative);
		alternative.setParent(parent);
		return true;
	}

	public abstract String getType();

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Node))
			return false;

		Node<?> node = (Node<?>) o;

		if (id != node.id)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}

	public abstract int numVarsInTree();
	public abstract int numConstsInTree();
	public abstract int numOpsInTree();

	public abstract Set<T> varsInTree();

	public abstract int size();

	/**
	 * Returns the depth of this specific node within the tree.
	 * 
	 * @return
	 */
	public int depth() {
		if (parent == null)
			return 0;
		else
			return 1 + parent.depth();
	}

	protected void checkInterrupted() throws InterruptedException {
		if (Thread.interrupted()) {
			throw new InterruptedException();
		}
	}

	/**
	 * Returns the maximum depth of the subtree of which this node is the root.
	 * 
	 * @return
	 */
	public int subTreeMaxdepth() {
		int maxDepth = 0;
		if (getChildren().isEmpty())
			maxDepth = depth();
		else {

			for (Node<?> child : getChildren()) {
				int childMaxDepth = child.subTreeMaxdepth();
				if (childMaxDepth > maxDepth) {
					maxDepth = childMaxDepth;
				}
			}

		}
		return maxDepth;
	}

	public abstract Expr toZ3(Context ctx);
	
	public Node<T> simp() {
		try {
			Context ctx = new Context();
			Expr z3Expr = this.toZ3(ctx).simplify();
			Node<T> retVal = (Node<T>) NodeSimplifier.fromZ3(z3Expr);
			ctx.close();
			return retVal;
		}
		catch (Exception e) {
			return this;
		}
	}
	
	public boolean isCorrect(MultiValuedMap<List<VariableAssignment<?>>, VariableAssignment<?>> evals) {
		int maxDepth = 0;
		try {
		if (this.getType().equals("string"))
			return new SingleOutputStringFitness(evals, VariableAssignment.getStringValues(),
					(Node<VariableAssignment<String>>) this, maxDepth).correct();
		else if (this.getType().equals("double"))
			return new SingleOutputDoubleFitness(evals, VariableAssignment.getDoubleValues(),
					(Node<VariableAssignment<Double>>) this, maxDepth).correct();
		else if (this.getType().equals("integer"))
			return new SingleOutputIntegerFitness(evals, VariableAssignment.getIntValues(),
					(Node<VariableAssignment<Integer>>) this, maxDepth).correct();
		else if (this.getType().equals("List"))
			return new SingleOutputListFitness(evals, VariableAssignment.getListValues(),
					(Node<VariableAssignment<List>>) this, maxDepth).correct();
		else {
			assert (this.getType().equals("boolean"));
			return new SingleOutputBooleanFitness(evals, VariableAssignment.getBooleanValues(),
					(Node<VariableAssignment<Boolean>>) this, maxDepth).correct();
		}
		}
		catch (Exception e) {
			return false;
		}
	}
	
}
