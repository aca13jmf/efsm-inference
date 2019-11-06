package mint.inference.gp.fitness;

import mint.inference.gp.tree.Node;
import mint.tracedata.types.VariableAssignment;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Created by neilwalkinshaw on 05/03/15.
 */
public class SingleOutputDoubleFitness extends SingleOutputFitness<Double> {

	protected double ceiling = 100000D;// Cannot make this Double.MAX, because overall fitness will yield a NaN from
										// multiple penalties.

//    private final static org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(SingleOutputDoubleFitness.class.getName());

	public SingleOutputDoubleFitness(MultiValuedMap<List<VariableAssignment<?>>, VariableAssignment<?>> evals,
			List<Double> list,
			Node<VariableAssignment<Double>> individual, int maxDepth) {
		super(evals, individual, maxDepth);
	}

	/**
	 *
	 * @param actual - returned by SUT
	 * @param exp    - produced by model
	 * @return
	 * @throws InvalidDistanceException
	 */
	@Override
	protected double distance(Double actual, Object exp) throws InvalidDistanceException {
		if (exp instanceof Double) {
			Double expected = (Double) exp;
			if (actual.isNaN() || actual.isInfinite()) {
				if (expected.isInfinite() || expected.isNaN())
					return 0;
				else
					return ceiling;
			}
			if (expected.isNaN() || expected.isInfinite())
				return ceiling;
			// prevent the fitness function from running away with massive errors.
			return Math.min(Math.abs(actual - expected), ceiling);
		} else if (exp instanceof Integer) {
			Integer intExp = (Integer) exp;
			return distance(actual, (double) intExp.intValue());
		} else
			throw new InvalidDistanceException();
	}
}
