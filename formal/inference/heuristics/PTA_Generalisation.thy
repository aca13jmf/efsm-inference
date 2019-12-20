theory PTA_Generalisation
  imports "Symbolic_Regression"
begin

primrec insert_into_group :: "(tids \<times> 'a transition_ext) list list \<Rightarrow> (tids \<times> 'a transition_ext) \<Rightarrow> (tids \<times> 'a transition_ext) list list" where
  "insert_into_group [] pair = [[pair]]" |
  "insert_into_group (h#t) pair = (
    if \<forall>(_, t) \<in> set h. same_structure (snd pair) t then
      ((List.insert pair h))#t
    else
      h#(insert_into_group t pair)
    )"

(* Assign registers and inputs with associated outputs to the correct training set based on       *)
(* transition id                                                                                  *)
definition assign_training_set :: "(((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list \<Rightarrow> tids \<Rightarrow> label \<Rightarrow> inputs \<Rightarrow> registers \<Rightarrow> value list \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list" where
  "assign_training_set data tids label inputs registers outputs = map (\<lambda>gp. 
    let (transitions, trainingSet) = gp in
    if \<exists>(tids', _) \<in> set transitions. tids' = tids then
      (transitions, (registers, inputs, outputs)#trainingSet)
    else
      gp
  ) data"

\<comment> \<open>This will be replaced by symbolic regression in the executable\<close>
definition get_output :: "nat \<Rightarrow> value list \<Rightarrow> inputs list \<Rightarrow> registers list \<Rightarrow> value list \<Rightarrow> (aexp \<times> (vname \<Rightarrow>f String.literal)) option" where
  "get_output maxReg values I r P = (let
    possible_funs = {a. \<forall>(i, r, p) \<in> set (zip I (zip r P)). aval a (join_ir i r) = Some p}
    in
    if possible_funs = {} then None else Some (Eps (\<lambda>x. x \<in> possible_funs), (K$ STR ''int''))
  )"
declare get_output_def [code del]
code_printing constant get_output \<rightharpoonup> (Scala) "Dirties.getOutput"

definition get_outputs :: "nat \<Rightarrow> value list \<Rightarrow> inputs list \<Rightarrow> registers list \<Rightarrow> value list list \<Rightarrow> (aexp \<times> (vname \<Rightarrow>f String.literal)) option list" where
  "get_outputs maxReg values I r outputs = map (\<lambda>ps. get_output maxReg values I r ps) (transpose outputs)" 

fun put_outputs :: "(((aexp \<times> (vname \<Rightarrow>f String.literal)) option) \<times> aexp) list \<Rightarrow> aexp list" where
  "put_outputs [] = []" |
  "put_outputs ((None, p)#t) = p#(put_outputs t)" |
  "put_outputs ((Some (p, _), _)#t) = p#(put_outputs t)"

type_synonym output_types = "(nat \<times> (aexp \<times> vname \<Rightarrow>f String.literal) option) list"

(*This is where the types stuff originates*)
definition generalise_outputs :: "value list \<Rightarrow> ((tids \<times> transition) list \<times> (registers \<times> value list \<times> value list) list) list \<Rightarrow> (tids \<times> transition \<times> output_types) list list" where
  "generalise_outputs values groups = map (\<lambda>(maxReg, group).
    let
      I = map (\<lambda>(regs, ins, outs).ins) (snd group);
      R = map (\<lambda>(regs, ins, outs).regs) (snd group);
      P = map (\<lambda>(regs, ins, outs).outs) (snd group);
      outputs = get_outputs maxReg values I R P
    in
    map (\<lambda>(id, tran). (id, tran \<lparr>Outputs := put_outputs (zip outputs (Outputs tran))\<rparr>, enumerate 0 outputs)) (fst group)
  ) (enumerate 0 groups)"

definition replace_transition :: "iEFSM \<Rightarrow> tids \<Rightarrow> transition \<Rightarrow> iEFSM" where
  "replace_transition e uid new = (fimage (\<lambda>(uids, (from, to), t). if set uid \<subseteq> set uids then (uids, (from, to), new) else (uids, (from, to), t)) e)"

primrec replace_groups :: "(tids \<times> transition) list list \<Rightarrow> iEFSM \<Rightarrow> iEFSM" where
  "replace_groups [] e = e" |
  "replace_groups (h#t) e = (replace_groups t (fold (\<lambda>(id, t) acc. replace_transition acc id t) h e))"

definition insert_updates :: "transition \<Rightarrow> update_function list \<Rightarrow> transition" where
  "insert_updates t u = (
    let
      \<comment> \<open>Need to derestrict variables which occur in the updates but keep unrelated ones to avoid \<close>
      \<comment> \<open>nondeterminism creeping in too early in the inference process                            \<close>
      relevant_vars = image V (fold (\<lambda>(r, u) acc. acc \<union> (AExp.enumerate_vars u)) u {});
      \<comment> \<open>Want to filter out null updates of the form rn := rn. It doesn't affect anything but it  \<close>
      \<comment> \<open>does make things look cleaner                                                            \<close>
      necessary_updates = filter (\<lambda>(r, u). u \<noteq> V (R r)) u
    in
    \<lparr>Label = Label t, Arity = Arity t, Guard = (Guard t), Outputs = Outputs t, Updates = (filter (\<lambda>(r, _). r \<notin> set (map fst u)) (Updates t))@necessary_updates\<rparr>
  )"

definition get_updates :: "(tids \<times> update_function list) list \<Rightarrow> tids \<Rightarrow> update_function list" where
  "get_updates u t = List.maps snd (filter (\<lambda>(tids, _). set t \<subseteq> set tids) u)"

definition get_ids :: "(tids \<times> update_function list) list \<Rightarrow> tids \<Rightarrow> tids" where
  "get_ids u t = List.maps fst (filter (\<lambda>(tids, _). set t \<subseteq> set tids) u)"

fun add_groupwise_updates_trace :: "execution  \<Rightarrow> (tids \<times> update_function list) list \<Rightarrow> iEFSM \<Rightarrow> cfstate \<Rightarrow> registers \<Rightarrow> iEFSM" where
  "add_groupwise_updates_trace [] _ e _ _ = e" |
  "add_groupwise_updates_trace ((l, i, _)#trace) funs e s r = (
    let
      (id, s', t) = fthe_elem (i_possible_steps e s r l i);
      updated = apply_updates (Updates t) (join_ir i r) r;
      newUpdates = get_updates funs id;
      t' = insert_updates t newUpdates;
      updated' = apply_updates (Updates t') (join_ir i r) r;
      necessaryUpdates = filter (\<lambda>(r, _). updated $ r \<noteq> updated' $ r) newUpdates;
      t'' = insert_updates t necessaryUpdates;
      e' = replace_transition e id t''
    in
    add_groupwise_updates_trace trace funs e' s' updated'
  )"

primrec add_groupwise_updates :: "log  \<Rightarrow> (tids \<times> update_function list) list \<Rightarrow> iEFSM \<Rightarrow> iEFSM" where
  "add_groupwise_updates [] _ e = e" |
  "add_groupwise_updates (h#t) funs e = add_groupwise_updates t funs (add_groupwise_updates_trace h funs e 0 <>)"

lemma fold_add_groupwise_updates: "add_groupwise_updates log funs e = fold (\<lambda>trace acc. add_groupwise_updates_trace trace funs acc 0 <>) log e"
  by (induct log arbitrary: e, auto)

fun groupwise_put_updates :: "(tids \<times> transition) list list \<Rightarrow> log \<Rightarrow> value list \<Rightarrow> (nat \<times> (aexp \<times> vname \<Rightarrow>f String.literal)) \<Rightarrow> iEFSM \<Rightarrow> iEFSM" where
  "groupwise_put_updates [] _ _ _ e = e" |
  "groupwise_put_updates (gp#gps) log values (o_inx, (op, types)) e = (
    let
      walked = everything_walk_log op o_inx types log e (map fst gp);
      targeted = map (\<lambda>x. filter (\<lambda>(_, _, _, _, _, id, tran). (id, tran) \<in> set gp) x) (map (\<lambda>w. rev (target <> (rev w))) walked);
      group = fold List.union targeted [];
      group_updates = List.maps (\<lambda>x. case x of Some thing \<Rightarrow> [thing] | None \<Rightarrow> []) (groupwise_updates values [group]);
      updated = make_distinct (add_groupwise_updates log group_updates e)
    in
      groupwise_put_updates gps log values (o_inx, (op, types)) updated
  )"

definition is_defined_at :: "('a \<Rightarrow>f 'b) \<Rightarrow> 'a \<Rightarrow> bool" where
  "is_defined_at f a = (a \<in> {x. finfun_dom f $ x})"
code_printing constant is_defined_at \<rightharpoonup> (Scala) "_ isDefinedAt _"

fun assign_group :: "(transition option \<times> ((tids \<times> transition) list list)) list \<Rightarrow> transition option \<Rightarrow> tids \<times> transition \<Rightarrow> (transition option \<times> ((tids \<times> transition) list list)) list" where
  "assign_group [] k v = [(k, [[v]])]" |
  "assign_group ((k, groups)#t) k' v = (
    if same_structure_opt k k' then
      (k, insert_into_group groups v)#t
    else
      (k, groups)#(assign_group t k' v)
  )"

fun trace_group_transitions :: "iEFSM \<Rightarrow> execution \<Rightarrow> cfstate \<Rightarrow> registers \<Rightarrow> transition option \<Rightarrow> (transition option \<times> ((tids \<times> transition) list list)) list \<Rightarrow> (transition option \<times> ((tids \<times> transition) list list)) list" where
  "trace_group_transitions _ [] _ _ _ g = g" |
  "trace_group_transitions e ((l, i, _)#trace) s r prev g = (
    let
      (id, s', t) = fthe_elem (i_possible_steps e s r l i);
      r' = apply_updates (Updates t) (join_ir i r) r;
      g' = assign_group g prev (id, t)
    in
    trace_group_transitions e trace s' r' (Some t) g'
  )"

definition log_group_transitions :: "iEFSM \<Rightarrow> log \<Rightarrow> (transition option \<times> ((tids \<times> transition) list list)) list" where
  "log_group_transitions e l = (fold (\<lambda>t acc. trace_group_transitions e t 0 <> None acc) l ([]))"

(*
definition transition_groups :: "iEFSM \<Rightarrow> log \<Rightarrow> (tids \<times> transition) list list" where
  "transition_groups e l = fold (\<lambda>(tid, _, transition) acc. insert_into_group acc (tid, transition)) (sorted_list_of_fset e) []"
*)

definition transition_groups :: "iEFSM \<Rightarrow> log \<Rightarrow> (tids \<times> transition) list list" where
  "transition_groups e l = (
    let
      group_dict = log_group_transitions e l
    in
      fold (\<lambda>(k, v) acc. List.union acc v) group_dict []
  )"

fun put_updates :: "log \<Rightarrow> value list \<Rightarrow> output_types \<Rightarrow> iEFSM \<Rightarrow> iEFSM option" where
  "put_updates _ _ [] e = Some e" |
  "put_updates _ _ ((_, None)#_) _ = None" |
  "put_updates log values ((o_inx, (Some (op, types)))#ops) e = (
    if enumerate_aexp_regs op = {} then
      put_updates log values ops e
    else
      let
        groups = transition_groups e log;
        updated = groupwise_put_updates groups log values (o_inx, (op, types)) e
      in
        put_updates log values ops updated
  )"

(*This is where the types stuff originates*)
definition generalise_and_update :: "log \<Rightarrow> iEFSM \<Rightarrow> (tids \<times> transition) list \<Rightarrow> (registers \<times> value list \<times> value list) list \<Rightarrow> iEFSM option" where
  "generalise_and_update log e tt train = (
    let
      values = enumerate_log_values log;
      I = map (\<lambda>(regs, ins, outs).ins) train;
      R = map (\<lambda>(regs, ins, outs).regs) train;
      P = map (\<lambda>(regs, ins, outs).outs) train;
      max_reg = max_reg_total e;
      outputs = get_outputs max_reg values I R P;
      changes = map (\<lambda>(id, tran). (id, tran\<lparr>Outputs := put_outputs (zip outputs (Outputs tran))\<rparr>)) tt;
      generalised_model = fold (\<lambda>(id, t) acc. replace_transition acc id t) changes e;
      tran = snd (hd tt)
  in
  case put_updates log values (enumerate 0 outputs) generalised_model of
    None \<Rightarrow> None |
    Some e' \<Rightarrow> if satisfies (set log) (tm e') then Some e' else None
  )"

fun groupwise_generalise_and_update :: "log \<Rightarrow> iEFSM \<Rightarrow> ((tids \<times> transition) list \<times> (registers \<times> value list \<times> value list) list) list \<Rightarrow> iEFSM" where
  "groupwise_generalise_and_update _ e [] = e" |
  "groupwise_generalise_and_update log e ((tt, train)#t) = (
    case generalise_and_update log e tt train of
      None \<Rightarrow> groupwise_generalise_and_update log e t |
      Some e' \<Rightarrow> groupwise_generalise_and_update log e' t
  )"

definition standardise_group :: "(tids \<times> transition) list \<Rightarrow> (tids \<times> transition) list" where
  "standardise_group g = (
    let
      updates = remdups (List.maps (Updates \<circ> snd) g)
    in
      map (\<lambda>(id, t). (id, t\<lparr>Updates := updates\<rparr>)) g
  )"

(* Sometimes inserting updates without redundancy can cause certain transitions to not get a      *)
(* particular update function. This can lead to disparate groups of transitions which we want to  *)
(* standardise such that every group of transitions has the same update function                  *)
definition standardise_groups :: "iEFSM \<Rightarrow> log \<Rightarrow> iEFSM" where
  "standardise_groups e l = (
    let
      groups = transition_groups e l;
      standardised_groups = map standardise_group groups
    in
      replace_transitions e (fold (@) standardised_groups [])
  )"

fun trace_training_set :: "iEFSM \<Rightarrow> execution \<Rightarrow> cfstate \<Rightarrow> registers \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list" where
  "trace_training_set _ [] _ _ ts = ts" |
  "trace_training_set e ((label, inputs, outputs)#t) s r ts = (
    let (id, s', transition) = fthe_elem (i_possible_steps e s r label inputs) in
    trace_training_set e t s' (apply_updates (Updates transition) (join_ir inputs r) r) (assign_training_set ts id label inputs r outputs)
  )"

primrec log_training_set :: "iEFSM \<Rightarrow> log \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list" where
  "log_training_set _ [] ts = ts" |
  "log_training_set e (h#t) ts = log_training_set e t (trace_training_set e h 0 <> ts)"

(* This will generate the training sets in the same order that the PTA was built, i.e. traces     *)
(* that appear earlier in the logs will appear earlier in the list of training sets. This allows  *)
(* us to infer register updates according to trace precidence so  we won't get redundant updates  *)
(* on later transitions which spoil the data state                                                *)
definition make_training_set :: "iEFSM \<Rightarrow> log \<Rightarrow> (((tids \<times> transition) list) \<times> (registers \<times> value list \<times> value list) list) list" where
  "make_training_set e l = log_training_set e l (map (\<lambda>x. (x, [])) (transition_groups e l))"

(* Instead of doing all the outputs and all the updates, do it one output and update at a time    *)
(* This way if one update fails, we don't end up losing the rest by having to default back to the *)
(* original PTA if the normalised one doesn't reproduce the original behaviour                    *)
definition incremental_normalised_pta :: "iEFSM \<Rightarrow> log \<Rightarrow> iEFSM" where
  "incremental_normalised_pta pta log = (
    let
      values = enumerate_log_values log;
      training_set = make_training_set pta log
    in
    standardise_groups (groupwise_generalise_and_update log pta training_set) log
  )"                 

\<comment> \<open>Need to derestrict variables which occur in the updates but keep unrelated ones to avoid \<close>
\<comment> \<open>nondeterminism creeping in too early in the inference process                            \<close>
definition derestrict_transition :: "transition \<Rightarrow> transition" where
  "derestrict_transition t = (
    let relevant_vars = image V (fold (\<lambda>(r, u) acc. acc \<union> (AExp.enumerate_vars u)) (Updates t) {}) in
    t\<lparr>Guard := filter (\<lambda>g. \<forall>v \<in> relevant_vars. \<not> gexp_constrains g v) (Guard t)\<rparr>
  )"

definition derestrict :: "log \<Rightarrow> update_modifier \<Rightarrow> (iEFSM \<Rightarrow> nondeterministic_pair fset) \<Rightarrow> iEFSM" where
  "derestrict log m np = (
    let
      pta = make_pta log;     
      normalised = incremental_normalised_pta pta log;
      derestricted = fimage (\<lambda>(id, tf, tran). (id, tf, derestrict_transition tran)) normalised;
      nondeterministic_pairs = sorted_list_of_fset (np derestricted)
    in
    case resolve_nondeterminism [] nondeterministic_pairs pta derestricted m (satisfies (set log)) np of
      None \<Rightarrow> pta |
      Some resolved \<Rightarrow> resolved
  )"

end