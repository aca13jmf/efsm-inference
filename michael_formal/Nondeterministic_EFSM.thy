theory Nondeterministic_EFSM
imports EFSM
begin

definition nondeterministic_step :: "transition_matrix \<Rightarrow> nat \<Rightarrow> datastate \<Rightarrow> label \<Rightarrow> inputs \<Rightarrow> (transition \<times> nat \<times> outputs \<times> datastate) option" where
"nondeterministic_step e s r l i = (
  if \<exists>x. x |\<in>| (possible_steps e s r l i) then (
    let (s', t) =  (Eps (\<lambda>x. x |\<in>| (possible_steps e s r l i))) in
    Some (t, s', (apply_outputs (Outputs t) (join_ir i r)), (apply_updates (Updates t) (join_ir i r) r)))
  else None)"

primrec nondeterministic_observe_all :: "transition_matrix \<Rightarrow> nat \<Rightarrow> datastate \<Rightarrow> trace \<Rightarrow> (transition \<times> nat \<times> outputs \<times> datastate) list" where
  "nondeterministic_observe_all _ _ _ [] = []" |
  "nondeterministic_observe_all e s r (h#t) =
    (case (nondeterministic_step e s r (fst h) (snd h)) of
      (Some (transition, s', outputs, updated)) \<Rightarrow> (((transition, s', outputs, updated)#(observe_all e s' updated t))) |
      _ \<Rightarrow> []
    )"

definition nondeterministic_observe_trace :: "transition_matrix \<Rightarrow> nat \<Rightarrow> datastate \<Rightarrow> trace \<Rightarrow> observation" where
  "nondeterministic_observe_trace e s r t \<equiv> map (\<lambda>(t,x,y,z). y) (nondeterministic_observe_all e s r t)"

inductive nondeterministic_simulates_trace :: "transition_matrix \<Rightarrow> transition_matrix \<Rightarrow> nat \<Rightarrow> nat \<Rightarrow> datastate \<Rightarrow> datastate \<Rightarrow> trace \<Rightarrow> (nat \<Rightarrow> nat) \<Rightarrow> bool" where
  base: "nondeterministic_simulates_trace e2 e1 s2 s1 d2 d1 [] _" |
  step_some: "s2 = H s1 \<Longrightarrow>
              nondeterministic_step e1 s1 d1 l i = Some (tr1, s1', p', d1') \<Longrightarrow>
              (s2', tr2) |\<in>| possible_steps e2 s2 d2 l i \<Longrightarrow>
              d2' = (apply_updates (Updates tr2) (join_ir i d2) d2) \<Longrightarrow>
              nondeterministic_simulates_trace e2 e1 s2' s1' d2' d1' t H \<Longrightarrow>
              nondeterministic_simulates_trace e2 e1 s2 s1 d2 d1 ((l, i)#t) H" |
  step_none: "nondeterministic_step e1 s1 d1 l i = None \<Longrightarrow> nondeterministic_simulates_trace e2 e1 s2 s1 d2 d1 ((l, i)#t) _"

lemma nondeterministic_step_none: "nondeterministic_step e s r l i = None \<Longrightarrow> step e s r l i = None"
proof-
  assume premise: "nondeterministic_step e s r l i = None"
  have aux1: "\<forall>a b. (a, b) |\<notin>| possible_steps e s r l i \<Longrightarrow>
        possible_steps e s r l i = {||}"
    by auto
  show ?thesis
    using premise
    apply (simp add: step_def nondeterministic_step_def)
    apply (case_tac "\<exists>a b. (a, b) |\<in>| possible_steps e s r l i")
     apply simp
     apply (cases "SOME x. x |\<in>| possible_steps e s r l i")
     apply simp
    apply simp
    apply clarify
    by (simp add: aux1)
qed

definition nondeterministic_simulates :: "transition_matrix \<Rightarrow> transition_matrix \<Rightarrow> (nat \<Rightarrow> nat) \<Rightarrow> bool" where
  "nondeterministic_simulates m2 m1 H = (\<forall>t. nondeterministic_simulates_trace m2 m1 0 0 <> <> t H)"

end