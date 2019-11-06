package mint.inference.gp.tree.nonterminals.integers;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import mint.inference.gp.Generator;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NodeVisitor;
import mint.inference.gp.tree.NonTerminal;
import mint.tracedata.types.IntegerVariableAssignment;

/**
 * Created by neilwalkinshaw on 03/03/15.
 */
public class SubtractIntegersOperator extends IntegerNonTerminal {

	public SubtractIntegersOperator() {
	}

	public SubtractIntegersOperator(Node<IntegerVariableAssignment> a, Node<IntegerVariableAssignment> b) {
		super();
		addChild(a);
		addChild(b);
	}

	@Override
	public IntegerVariableAssignment evaluate() throws InterruptedException {
		checkInterrupted();
		IntegerVariableAssignment res = copyResVar();
		res.setValue((Integer) getChild(0).evaluate().getValue() - (Integer) getChild(1).evaluate().getValue());
		vals.add(res.getValue());
		return res;
	}

	@Override
	public Node<IntegerVariableAssignment> copy() {
		SubtractIntegersOperator sdo = new SubtractIntegersOperator(
				(Node<IntegerVariableAssignment>) getChild(0).copy(),
				(Node<IntegerVariableAssignment>) getChild(1).copy());
		sdo.setResVar(copyResVar());
		return sdo;
	}

	@Override
	public NonTerminal<IntegerVariableAssignment> createInstance(Generator g, int depth) {
		SubtractIntegersOperator sdo = new SubtractIntegersOperator(g.generateRandomIntegerExpression(depth),
				g.generateRandomIntegerExpression(depth));
		sdo.setResVar(copyResVar());
		return sdo;
	}

	@Override
	public String nodeString() {
		return "(- " + childrenString() + ")";
	}

	@Override
	public boolean accept(NodeVisitor visitor) throws InterruptedException {
		if (visitor.visitEnter(this)) {
			visitChildren(visitor);
		}
		return visitor.visitExit(this);
	}

	@Override
	public Expr toZ3(Context ctx) {
		ArithExpr b1 = (ArithExpr) getChild(0).toZ3(ctx);
		ArithExpr b2 = (ArithExpr) getChild(1).toZ3(ctx);
		return ctx.mkSub(b1, b2);
	}
}
