package mint.inference.gp.tree.nonterminals.doubles;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import mint.inference.gp.Generator;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NodeVisitor;
import mint.inference.gp.tree.NonTerminal;
import mint.inference.gp.tree.Terminal;
import mint.tracedata.types.BooleanVariableAssignment;
import mint.tracedata.types.VariableAssignment;

/**
 * Created by neilwalkinshaw on 26/05/15.
 */
public class IfThenElseOperator extends NonTerminal<VariableAssignment<?>> {

	public IfThenElseOperator() {
		super();
	}

	public IfThenElseOperator(Node<BooleanVariableAssignment> a, Node<?> yes, Node<?> no) {
		super();
		addChild(a);
		addChild(yes);
		addChild(no);
	}

	@Override
	public NonTerminal<VariableAssignment<?>> createInstance(Generator g, int depth) {
		return new IfThenElseOperator(g.generateRandomBooleanExpression(depth), g.generateRandomDoubleExpression(depth),
				g.generateRandomDoubleExpression(depth));
	}

	@Override
	protected String nodeString() {
		return "(ite " + childrenString() + ")";
	}

	@Override
	public boolean accept(NodeVisitor visitor) throws InterruptedException {
		if (visitor.visitEnter(this)) {
			visitChildren(visitor);
		}
		return visitor.visitExit(this);
	}

	@Override
	public void simplify() {
		for (Node<?> child : getChildren()) {
			child.simplify();
		}
		if (getChild(0).toString().equals("true")) {
			swapWith(getChild(1));
		} else if (getChild(0).toString().equals("false")) {
			swapWith(getChild(2));
		}
	}

	@Override
	public Terminal<VariableAssignment<?>> getTermFromVals() {
		return null;
	}

	@Override
	public VariableAssignment<?> evaluate() throws InterruptedException {
		checkInterrupted();
		boolean condition = (Boolean) getChild(0).evaluate().getValue();
		if (condition) {
			VariableAssignment<?> val = getChild(1).evaluate();
			vals.add(val);
			return val;
		} else {
			VariableAssignment<?> val = getChild(2).evaluate();
			vals.add(val);
			return val;
		}
	}

	@Override
	public Node<VariableAssignment<?>> copy() {
		return new IfThenElseOperator((Node<BooleanVariableAssignment>) children.get(0).copy(), children.get(1).copy(),
				children.get(2).copy());
	}

	@Override
	public String getType() {
		return "double";
	}

	@Override
	public Expr toZ3(Context ctx) {
		BoolExpr b1 = (BoolExpr) getChild(0).toZ3(ctx);
		return ctx.mkITE(b1, getChild(1).toZ3(ctx), getChild(2).toZ3(ctx));
	}
}
