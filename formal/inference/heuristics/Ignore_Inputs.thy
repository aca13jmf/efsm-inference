theory Ignore_Inputs
imports "../Inference"
begin

definition enumerate_outputs :: "iEFSM \<Rightarrow> label \<Rightarrow> arity \<Rightarrow> vname  aexp list fset" where
  "enumerate_outputs e l a = (fimage (\<lambda>(_, _, t). Outputs t) (ffilter (\<lambda>(_, _, t). Label t = l \<and> Arity t = a) e))"

definition drop_guards :: "transition \<Rightarrow> transition" where
  "drop_guards t = \<lparr>Label = Label t, Arity = Arity t, Guard = [], Outputs = Outputs t, Updates = Updates t\<rparr>"

lemma can_take_transition_right_length: "length i = Arity t \<Longrightarrow> can_take_transition (drop_guards t) i c"
  by (simp add: drop_guards_def can_take_transition_def can_take_def)

definition drop_inputs :: "update_modifier" where
  "drop_inputs t1ID t2ID s new old _ np = (let
     t1 = (get_by_ids new t1ID);
     t2 = (get_by_ids new t2ID) in
     if fis_singleton (enumerate_outputs new (Label t1) (Arity t1)) then
     Some (fimage 
      (\<lambda>(id, route, t). 
       if id = t1ID then
         (id, route, drop_guards t)
       else (id, route, t)) (ffilter (\<lambda>(id, _). id \<noteq> t2ID) new))
     else None
   )"

definition transitionwise_drop_inputs :: update_modifier where
  "transitionwise_drop_inputs t1ID t2ID s new _ old np = (let
     t1 = (get_by_ids new t1ID);
     t2 = (get_by_ids new t2ID) in
     if Outputs t1 = Outputs t2 then
       Some (replace_transitions new [(t1ID, drop_guards t1), (t2ID, drop_guards t1)])
     else
       None)"

lemma drop_inputs_subsumption: "subsumes (drop_guards t1) c t1"
  by (simp add: can_take_def can_take_transition_def drop_guards_def subsumption)

end