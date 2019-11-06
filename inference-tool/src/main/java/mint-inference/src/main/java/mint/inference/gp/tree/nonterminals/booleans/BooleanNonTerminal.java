package mint.inference.gp.tree.nonterminals.booleans;

import mint.inference.gp.tree.Node;
import mint.inference.gp.tree.NonTerminal;
import mint.inference.gp.tree.Terminal;
import mint.inference.gp.tree.terminals.BooleanVariableAssignmentTerminal;
import mint.tracedata.types.BooleanVariableAssignment;

/**
 * Created by neilwalkinshaw on 26/05/15.
 */
public abstract class BooleanNonTerminal extends NonTerminal<BooleanVariableAssignment> {

    Node<BooleanVariableAssignment> base;

    public BooleanNonTerminal(){}

    public BooleanNonTerminal(Node<?> b){
        this.base = base;
    }

    @Override
    public String getType() {
        return "boolean";
    }

    @Override
    public Terminal<BooleanVariableAssignment> getTermFromVals(){
        BooleanVariableAssignment bvar = new BooleanVariableAssignment("res",(Boolean)vals.iterator().next());
        BooleanVariableAssignmentTerminal term = new BooleanVariableAssignmentTerminal(bvar,true);
        return term;
    }

}
