package mint.tracedata.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Holder for a list of variable objects
 *
 * TODO partially implemented - refactoring needed! Created by neilwalkinshaw on
 * 24/06/15.
 */
public class ListVariableAssignment extends VariableAssignment<List> {

	public ListVariableAssignment(String name) {
		super(name);
	}

	/**
	 * The String value is assumed to be separated by commas.
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	@Override
	public VariableAssignment<?> createNew(String name, String value) {
		ListVariableAssignment lva = null;
		if (value != null) {
			List vals = new ArrayList();
			StringTokenizer st = new StringTokenizer(value);
			while (st.hasMoreElements()) {
				Object toAdd = st.nextElement();
				vals.add(toAdd);
			}
			lva = new ListVariableAssignment(name, vals);
		}
		return lva;
	}

	/**
	 * The String value is assumed to be separated by commas.
	 * 
	 * @param s
	 */
	@Override
	public void setStringValue(String s) {
		List vals = new ArrayList();
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreElements()) {
			Object toAdd = st.nextElement();
			vals.add(toAdd);
		}
		setValue(vals);
	}

	public ListVariableAssignment(String name, List vals) {
		super(name);
		if (vals != null)
			setNull(false);
		this.value = vals;
	}

	@Override
	public String printableStringOfValue() {
		String listString = "";
		for (int i = 0; i < value.size(); i++) {
			listString += value.get(i);
			if (i < value.size() - 1)
				listString += ",";
		}
		return listString;
	}

	@Override
	public String typeString() {
		return "List";
	}

	@Override
	public VariableAssignment<List> copy() {
		ArrayList copiedLIst = new ArrayList();
		for (Object el : value) {
			copiedLIst.add(el);
		}
		return new ListVariableAssignment(name, copiedLIst);
	}

	/**
	 * Not implemented
	 * 
	 * @return
	 */
	@Override
	protected List generateRandom() {
		return null;
	}

	@Override
	public List<List> getValues() {
		return VariableAssignment.getListValues();
	}

}
