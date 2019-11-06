package mint.inference.gp.tree.nonterminals.doubles;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

import mint.inference.gp.Generator;
import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NodeVisitor;
import mint.inference.gp.tree.NonTerminal;
import mint.tracedata.types.DoubleVariableAssignment;

/**
 * Created by neilwalkinshaw on 03/03/15.
 */
public class AddDoublesOperator extends DoubleNonTerminal {

	public AddDoublesOperator() {
	}

	public AddDoublesOperator(Node<DoubleVariableAssignment> a, Node<DoubleVariableAssignment> b) {
		super();
		addChild(a);
		addChild(b);
	}

	@Override
	public DoubleVariableAssignment evaluate() throws InterruptedException {
		checkInterrupted();
		DoubleVariableAssignment childRes1 = null;
		DoubleVariableAssignment childRes2 = null;
		Double c1 = null;
		Double c2 = null;
		try {
			childRes1 = (DoubleVariableAssignment) getChild(0).evaluate();
			childRes2 = (DoubleVariableAssignment) getChild(1).evaluate();
			c1 = childRes1.getValue();
			c2 = childRes2.getValue();
			DoubleVariableAssignment res = copyResVar();
			res.setValue(c1 + c2);
			vals.add(res.getValue());
			return res;
		} catch (Exception e) {
		}

		return null;
	}

	@Override
	public NonTerminal<DoubleVariableAssignment> createInstance(Generator g, int depth) {
		DoubleNonTerminal created = new AddDoublesOperator(g.generateRandomDoubleExpression(depth),
				g.generateRandomDoubleExpression(depth));
		created.setResVar(copyResVar());
		return created;
	}

	@Override
	public Node<DoubleVariableAssignment> copy() {
		DoubleNonTerminal created = new AddDoublesOperator((Node<DoubleVariableAssignment>) getChild(0).copy(),
				(Node<DoubleVariableAssignment>) getChild(1).copy());
		created.setResVar(copyResVar());
		return created;
	}

	@Override
	public String nodeString() {
		return "(+ " + childrenString() + ")";
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
		return ctx.mkAdd(b1, b2);
	}
}
