package mint.inference.gp.selection;

import mint.inference.evo.Chromosome;
import mint.inference.evo.TournamentSelection;
import mint.tracedata.types.VariableAssignment;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;

/**
 * Created by neilwalkinshaw on 18/05/2016.
 */
public abstract class IOTournamentSelection<T> extends TournamentSelection {

    protected MultiValuedMap<List<VariableAssignment<?>>, T> evals;

    public IOTournamentSelection(MultiValuedMap<List<VariableAssignment<?>>, T> evals, List<Chromosome> totalPopulation, int maxDepth){
        super(totalPopulation,maxDepth);
        this.evals = evals;
    }

}
