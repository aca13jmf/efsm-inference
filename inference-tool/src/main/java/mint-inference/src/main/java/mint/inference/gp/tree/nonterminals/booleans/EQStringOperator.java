package mint.inference.gp.tree.nonterminals.booleans;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import mint.inference.gp.Generator;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NodeVisitor;
import mint.inference.gp.tree.NonTerminal;
import mint.tracedata.types.BooleanVariableAssignment;

/**
 * Created by neilwalkinshaw on 26/05/15.
 */
public class EQStringOperator extends BooleanNonTerminal {

	public EQStringOperator(Node<?> a, Node<?> b) {
		super(null);
		addChild(a);
		addChild(b);
	}

	public EQStringOperator() {

	}

	@Override
	public NonTerminal<BooleanVariableAssignment> createInstance(Generator g, int depth) {
		return new EQStringOperator(g.generateRandomStringExpression(depth), g.generateRandomStringExpression(depth));
	}

	@Override
	protected String nodeString() {
		return "(= " + childrenString() + ")";
	}

	@Override
	public boolean accept(NodeVisitor visitor) throws InterruptedException {
		if (visitor.visitEnter(this)) {
			visitChildren(visitor);
		}
		return visitor.visitExit(this);
	}

	@Override
	public BooleanVariableAssignment evaluate() throws InterruptedException {
		checkInterrupted();
		Object from = children.get(0).evaluate().getValue();
		Object to = children.get(1).evaluate().getValue();
		BooleanVariableAssignment res = null;
		if (from == null || to == null)
			res = new BooleanVariableAssignment("result", false);
		else
			res = new BooleanVariableAssignment("result", to.equals(from));
		vals.add(res.getValue());
		return res;
	}

	@Override
	public Node<BooleanVariableAssignment> copy() {
		return new EQStringOperator(children.get(0).copy(), children.get(1).copy());

	}

	@Override
	public Expr toZ3(Context ctx) {
		return ctx.mkEq(getChild(0).toZ3(ctx), getChild(1).toZ3(ctx));
	}

}
