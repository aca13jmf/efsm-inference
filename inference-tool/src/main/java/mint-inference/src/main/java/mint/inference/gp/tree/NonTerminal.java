package mint.inference.gp.tree;

import mint.inference.gp.Generator;
import mint.tracedata.types.VariableAssignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * Created by neilwalkinshaw on 03/03/15.
 */
public abstract class NonTerminal<T extends VariableAssignment<?>> extends Node<T> {
	
	public Set<T> varsInTree() {
		Set<T> vars = new HashSet<T>();
		for (Node<?> child: this.getChildren()) {
			for (VariableAssignment<?> var: child.varsInTree()) {
				vars.add((T) var);
			}
		}
		return vars;
	}

    protected List<Node<?>> children;

    public NonTerminal() {
        super();
        children = new ArrayList<Node<?>>();
    }

    protected void visitChildren(NodeVisitor visitor) throws InterruptedException {
        Stack<Node<?>> childrenStack = new Stack<Node<?>>();
        for (Node<?> child : children) {
            childrenStack.push(child);

        }
        while(!childrenStack.isEmpty()){
            childrenStack.pop().accept(visitor);
        }
    }

    public void simplify(){
        if (vals.size() == 1) {
            Terminal<T> term = getTermFromVals();
            swapWith(term);
        } else if (vals.size() > 1) {
            for (Node<?> child : getChildren()) {
                child.simplify();
            }
        }
    }

    /**
     * Get the first value from vals (there must be one)
     * and return as a terminal. Used only for simplification
     * @return
     */
    protected abstract Terminal<T> getTermFromVals();

    public List<Node<?>> getChildren(){
        return children;
    }

    public void addChild(Node<?> child){
        children.add(child);
        child.setParent(this);
    }
    
    public void clearChildren() {
    	children.clear();
    }

    @Override
    public void mutate(Generator g, int depth){
        //subtree-mutation
        int childPos = g.getRandom().nextInt(children.size());
        Node<?> child = children.get(childPos);
        if(child.getType().equals("boolean")){
            child = g.generateRandomBooleanExpression(depth-1);
        }
        else if(child.getType().equals("double")) {
            child = g.generateRandomDoubleExpression(depth - 1);
        }
        else if(child.getType().equals("integer")){
            child = g.generateRandomIntegerExpression(depth-1);
        }
        else{
            child = g.generateRandomStringExpression(depth-1);
        }
        children.set(childPos,child);
    }


    public abstract NonTerminal<T> createInstance(Generator g, int depth);


    public String toString(){
        //if(allChildrenAreConstants())
        //    return evaluate().getValue().toString();
        //else
            return nodeString();
    }


    /**
     * String that returns a summary of the node and its children.
     * @return
     */
    protected abstract String nodeString();

    protected String childrenString(){
        //simplify();
        String retString = "";
        for(int i = 0; i<children.size(); i++){
            if(i>0)
                retString+=" ";
            retString += children.get(i).toString();
        }
        return retString;
    }

    @Override
    public int size(){
        return 1+childrenSizes();
    }

    private int childrenSizes() {
        int sizes = 0;
        for(Node<?> n : children){
            sizes += n.size();
        }
        return sizes;
    }

    protected Node<?> getChild(int x){
        return children.get(x);
    }

    public int numVarsInTree(){
        int vars = 0;
        for(Node<?> n : children){
            vars += n.numVarsInTree();
        }
        return vars;
    }
    
    public int numConstsInTree(){
        int consts = 0;
        for(Node<?> n : children){
            consts += n.numConstsInTree();
        }
        return consts;
    }
    
    public int numOpsInTree() {
        int ops = 1;
        for(Node<?> n : children){
        	ops += n.numConstsInTree();
        }
        return ops;
    }
}
