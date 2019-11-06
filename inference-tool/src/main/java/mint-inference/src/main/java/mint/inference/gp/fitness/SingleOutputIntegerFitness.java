package mint.inference.gp.fitness;

import mint.inference.gp.tree.Node;
import mint.tracedata.types.VariableAssignment;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Created by neilwalkinshaw on 05/03/15.
 */
public class SingleOutputIntegerFitness extends SingleOutputFitness<Integer> {

	public SingleOutputIntegerFitness(MultiValuedMap<List<VariableAssignment<?>>, VariableAssignment<?>> evals,
			List<Integer> list,
			Node<VariableAssignment<Integer>> individual, int maxDepth) {
		super(evals, individual, maxDepth);
	}

	@Override
	protected double distance(Integer actual, Object expected) throws InvalidDistanceException {
		if (expected instanceof Integer) {
			Integer exp = (Integer) expected;
			return Math.abs(actual.intValue() - exp.intValue());
		} else if (expected instanceof Double) {
			Double exp = (Double) expected;
			return Math.abs(actual.intValue() - exp.intValue());
		} else
			throw new InvalidDistanceException();
	}
}
