theory DM_Inference
imports Inference "../examples/Drinks_Machine_2"
begin

lemma explode_coin: "(literal.explode STR ''coin'') = ''coin''"
  by (simp add: Literal.rep_eq zero_literal.rep_eq)

lemma explode_vend: "(literal.explode STR ''vend'') = ''vend''"
  by (simp add: Literal.rep_eq zero_literal.rep_eq)

(* Plus is append *)
(* value "STR ''vend'' + STR ''hello''" *)

lemma "max coin vend = vend"
  apply (simp add: max_def coin_def vend_def less_eq_transition_ext_def)
  by (simp add: String.less_literal_def explode_coin explode_vend)

lemma merge_q1_q2: "merge_states q1 q2 (T drinks2) = (\<lambda> (a,b) .
  if (a, b) = (q0, q1) then {|select|} else
  if (a, b) = (q1,q1) then {|vend_nothing, coin, vend_fail|} else
  if (a, b) = (q1, q3) then {|vend|} else {||})"
  apply (rule ext)
  apply clarify
  apply (simp add: merge_states_def drinks2_def merge_with_def)
  by auto

lemma defined_drinks2: "(defined (T drinks2)) = {(q0,q1), (q1,q1), (q1,q2), (q2,q2), (q2,q3)}"
  apply (simp add: defined_def drinks2_def)
  using prod.inject by fastforce

lemma finite_defined_drinks2: "finite (defined (T drinks2))"
  by (simp add: defined_drinks2)

lemma possible_transitions: "t \<noteq> select \<Longrightarrow> t \<noteq> vend_nothing \<Longrightarrow> t \<noteq> coin \<Longrightarrow> t \<noteq> vend_fail \<Longrightarrow> t \<noteq> vend \<Longrightarrow> t \<notin> (if s' = q0 \<and> s2 = q1 then {select} else if (s', s2) = (q1, q1) then {vend_nothing, coin, vend_fail} else if (s', s2) = (q1, q3) then {vend} else {})"
  by simp

lemma unequal_labels[simp]: "Label t1 \<noteq> Label t2 \<Longrightarrow> t1 \<noteq> t2"
  by auto

lemma unequal_arities[simp]: "Arity t1 \<noteq> Arity t2 \<Longrightarrow> t1 \<noteq> t2"
  by auto

lemma choice_vend_vend_nothing: "choice vend vend_nothing"
  apply (simp add: choice_def transitions)
  apply (rule_tac x="<R 2 := Num 100>" in exI)
  by simp

lemma vend_nothing_lt_vend: "vend_nothing < vend"
  by (simp add: less_transition_ext_def transitions conjoin_def)

lemma no_choice_vend_coin: "\<not> choice vend coin"
  by (simp add: choice_def transitions)

lemma coin_not_vend_fail: "coin \<noteq> vend_fail"
  by (simp add: transitions)

lemma no_choice_vend_vend_fail:  "\<not> choice vend vend_fail"
  apply (simp add: choice_def transitions)
  apply safe
  apply (case_tac " MaybeBoolInt (\<lambda>x y. y < x) (Some (Num 100)) (s (R 2))")
   apply simp
  by simp

lemma vend_nothing_lt_vend_fail: "vend_nothing < vend_fail"
  by (simp add: less_transition_ext_def transitions conjoin_def)

lemma choice_vend_nothing_vend_fail: "choice vend_nothing vend_fail"
  apply (simp add: choice_def transitions)
  apply (rule_tac x="<R 2 := Num 0>" in exI)
  by simp

lemma vend_vend_nothing_nondeterminism: "nondeterministic_pairs (merge_states q1 q2 (T drinks2)) \<noteq> {}"
  apply (simp add: nondeterministic_pairs_def merge_q1_q2)
  apply (rule_tac x=q1 in exI)
  apply (rule_tac x=q1 in exI)
  apply simp
  apply (rule_tac x=q1 in exI)
  apply simp
  apply (rule_tac x=vend_nothing in exI)
  apply (rule_tac x=vend_fail in exI)
  apply (simp add: vend_nothing_lt_vend_fail)
  by (simp add: choice_vend_nothing_vend_fail)

lemma nondeterminism_example: "(q1, (q1, q1), vend_nothing, vend_fail) \<in> nondeterministic_pairs (merge_states q1 q2 (T drinks2))"
  apply (simp add: merge_q1_q2 nondeterministic_pairs_def vend_nothing_lt_vend_fail)
  apply (simp add: transitions choice_def)
  apply (rule_tac x="<R 2 := Num 0>" in exI)
  by simp

lemma nond_transitions_not_none: "nondeterministic_transitions (merge_states q1 q2 (T drinks2)) \<noteq> None"
  apply (simp add: nondeterministic_transitions_def)
  apply (simp add: vend_vend_nothing_nondeterminism)
  by (metis surj_pair)

lemma nondeterminism_example_2: "(q1, (q1, q3), (vend_nothing, vend)) \<in> nondeterministic_pairs (merge_states q1 q2 (T drinks2))"
  apply (simp add: merge_q1_q2 nondeterministic_pairs_def vend_nothing_lt_vend)
  apply (simp add: transitions choice_def)
  apply (rule_tac x="<R 2 := Num 100>" in exI)
  by simp

lemma nondeterministic_pairs_aux_1: "\<And>xa a b aa ba.
       x = (q1, (a, b), aa, ba) \<Longrightarrow>
       aa |\<in>| (if a = q3 then {|vend|} else {||}) \<Longrightarrow>
       ba |\<in>| (if b = q1 then {|vend_nothing, coin, vend_fail|} else if (q1, b) = (q1, q3) then {|vend|} else {||}) \<Longrightarrow>
       aa < ba \<Longrightarrow> choice aa ba \<Longrightarrow> xa = q1 \<Longrightarrow> a \<noteq> q1 \<Longrightarrow> False"
proof -
fix xa :: statename and a :: statename and b :: statename and aa :: transition and ba :: transition
assume a1: "aa < ba"
assume a2: "ba |\<in>| (if b = q1 then {|vend_nothing, coin, vend_fail|} else if (q1, b) = (q1, q3) then {|vend|} else {||})"
assume a3: "aa |\<in>| (if a = q3 then {|vend|} else {||})"
assume a4: "choice aa ba"
have f5: "\<forall>f t. f \<noteq> {||} \<or> (t::transition) |\<notin>| f"
by auto
  have f6: "(if b = q1 then {|vend_nothing, coin, vend_fail|} else if (q1, b) = (q1, q3) then {|vend|} else {||}) \<noteq> {||}"
    using a2 by blast
  have f7: "aa |\<in>| {|vend|}"
using f5 a3 by presburger
then have "ba |\<notin>| {|vend_nothing, coin, vend_fail|}"
using f5 a4 a1 by (metis (no_types) choice_def finsertE label_vend label_vend_not_coin less_imp_not_less no_choice_vend_vend_fail vend_nothing_lt_vend)
then have "ba |\<in>| {|vend|}"
using f6 a2 by presburger
then show False
using f7 a1 by fastforce
qed

lemma nondeterministic_pairs_members: "x \<in> nondeterministic_pairs (merge_states q1 q2 (T drinks2)) \<Longrightarrow> x = (q1, (q1, q3), (vend_nothing, vend)) \<or> x = (q1, (q1, q1), vend_nothing, vend_fail)"
  apply (simp add: nondeterministic_pairs_def)
  apply clarify
  apply simp
  apply (case_tac "xa=q1")
   apply (case_tac "a=q1")
    apply (case_tac "b=q1")
     apply (simp add: merge_q1_q2)
     apply (metis choice_def label_vend_fail label_vend_not_coin label_vend_nothing not_less_iff_gr_or_eq vend_nothing_lt_vend_fail)
    apply (simp add: merge_q1_q2)
    apply (metis choice_symmetry fempty_iff finsertE no_choice_vend_coin no_choice_vend_vend_fail)
   apply (simp add: merge_q1_q2)
   apply (meson nondeterministic_pairs_aux_1)
  apply (simp add: merge_q1_q2)
  by (smt fempty_iff fsingleton_iff not_less_iff_gr_or_eq prod.inject)

lemma nondeterminisitic_pairs: "(nondeterministic_pairs (merge_states q1 q2 (T drinks2))) = {(q1, (q1, q3), (vend_nothing, vend)), (q1, (q1, q1), vend_nothing, vend_fail)}"
  using nondeterministic_pairs_members nondeterminism_example nondeterminism_example_2 by blast        

lemma no_nondeterminism_q0: "\<forall>aa b ab ba. nondeterministic_transitions (merge_states q1 q2 (T drinks2)) \<noteq> Some (q0, (aa, b), ab, ba)"
  by (simp add: nondeterministic_transitions_def nondeterminisitic_pairs max_def)

lemma no_nondeterminism_q2: "\<forall>aa b ab ba. (q2, (aa, b), ab, ba) \<notin> nondeterministic_pairs (merge_states q1 q2 (T drinks2))"
  by (simp add: merge_q1_q2 nondeterministic_pairs_def)

lemma no_nondeterminism_q2_2: "\<forall>aa b ab ba. nondeterministic_transitions (merge_states q1 q2 (T drinks2)) \<noteq> Some (q2, (aa, b), ab, ba)"
  by (simp add: nondeterministic_transitions_def nondeterminisitic_pairs max_def)

lemma no_nondeterminism_q3: "\<forall>aa b ab ba. (q3, (aa, b), ab, ba) \<notin> nondeterministic_pairs (merge_states q1 q2 (T drinks2))"
  by (simp add: merge_q1_q2 nondeterministic_pairs_def)

lemma no_nondeterminism_q3_2: "\<forall>aa b ab ba. nondeterministic_transitions (merge_states q1 q2 (T drinks2)) \<noteq> Some (q3, (aa, b), ab, ba)"
  by (simp add: nondeterministic_transitions_def nondeterminisitic_pairs max_def)

lemma only_nondeterminism_q1: "nondeterministic_transitions (merge_states q1 q2 (T drinks2)) = Some (a, (aa, b), aaa, ba) \<Longrightarrow> a = q1"
  apply (case_tac a)
  apply (simp add: no_nondeterminism_q0)
    apply simp
   apply (simp add: no_nondeterminism_q2_2)
  by (simp add: no_nondeterminism_q3_2)

lemma no_transitions_to_q0: "aa = q0 \<or> b = q0 \<Longrightarrow> (a, (aa, b), ab, ba) \<notin> nondeterministic_pairs (merge_states q1 q2 (T drinks2))"
  apply (cases a)
     apply (meson Pair_inject nondeterministic_pairs_members statename.distinct(1))
    apply (meson Pair_inject nondeterministic_pairs_members statename.distinct(1) statename.distinct(5))
   apply (simp add: no_nondeterminism_q2)
  by (simp add: no_nondeterminism_q3)

lemma no_transitions_to_q0_2: "aa = q0 \<or> b = q0 \<Longrightarrow> nondeterministic_transitions (merge_states q1 q2 (T drinks2)) \<noteq> Some (a, (aa, b), ab, ba)"
  apply (cases a)
     apply (simp add: no_nondeterminism_q0)
    apply (simp add: nondeterministic_transitions_def nondeterminisitic_pairs max_def)
    apply auto[1]
   apply (simp add: no_nondeterminism_q2_2)
  by (simp add: no_nondeterminism_q3_2)

lemma q1_nondeterminism_options: "(q1, (q1, q1), ab, ba) \<in> nondeterministic_pairs (merge_states q1 q2 (T drinks2)) \<Longrightarrow> ab = vend_fail \<and> ba = vend_nothing \<or> ab = vend_nothing \<and> ba = vend_fail"
  apply (simp add: nondeterministic_pairs_def merge_q1_q2 choice_def)
  apply safe
  by (simp_all add: transitions)

lemma q1_nondeterminism_options_2: "nondeterministic_transitions (merge_states q1 q2 (T drinks2)) = Some (q1, (q1, q1), ab, ba) \<Longrightarrow> ab = vend_fail \<and> ba = vend_nothing \<or> ab = vend_nothing \<and> ba = vend_fail"
  by (simp add: nondeterministic_transitions_def vend_vend_nothing_nondeterminism nondeterminisitic_pairs max_def)

lemma merge_self_state: "merge_states x x t = t"
  by (simp add: merge_states_def)

lemma medial_vend_nothing: "(medial c (Guard vend_nothing)) = c"
  by (simp add: transitions)

lemma medial_vend_fail: "medial select_posterior (Guard vend_fail) = \<lbrakk>V (R 1) \<mapsto> Bc True, V (R 2) \<mapsto> And (Eq (Num 0)) (Lt (Num 100))\<rbrakk>"
  apply (rule ext)
  by (simp add: transitions)

lemma vend_nothing_posterior: "posterior select_posterior vend_nothing = select_posterior"
  apply (simp only: posterior_def medial_vend_nothing)
  apply (simp add: consistent_select_posterior del: One_nat_def)
  apply (rule ext)
  by (simp add: transitions)

lemma consistent_medial_vend_fail: "consistent \<lbrakk>V (R 1) \<mapsto> cexp.Bc True, V (R 2) \<mapsto> And (cexp.Eq (Num 0)) (cexp.Lt (Num 100))\<rbrakk>"
  apply (simp add: consistent_def)
  apply (rule_tac x="<R 1 := Num 0, R 2 := Num 0>" in exI)
  by (simp add: connectives consistent_empty_4)

lemma vend_fail_posterior: "posterior select_posterior vend_fail = \<lbrakk>V (R 1) \<mapsto> Bc True, V (R 2) \<mapsto> And (Eq (Num 0)) (Lt (Num 100))\<rbrakk>"
  apply (simp only: posterior_def medial_vend_fail)
  apply (simp add: consistent_medial_vend_fail del: One_nat_def)
  apply (rule ext)
  by (simp add: transitions)

lemma vend_fail_subsumes_vend_nothing: "subsumes select_posterior vend_fail vend_nothing"
  apply (simp add: subsumes_def del: One_nat_def)
  apply safe
  using guard_vend_nothing medial_vend_fail apply auto[1]
    apply (simp add: transitions)
   apply (simp add: medial_vend_nothing del: One_nat_def)
   apply (simp add: vend_nothing_posterior del: One_nat_def)
   apply (simp only: vend_fail_posterior)
   apply (simp del: One_nat_def)
   apply (case_tac "r = V (R 2)")
    apply simp
    apply (case_tac "ValueLt (Some i) (Some (Num 100))")
     apply simp
    apply simp
   apply (case_tac "r = V (R 1)")
    apply simp
   apply simp
   apply (simp only: vend_fail_posterior)
  apply (simp add: consistent_def)
  apply (rule_tac x="<R 1 := Num 0, R 2 := Num 0>" in exI)
  by (simp add: connectives consistent_empty_4)

lemma posterior_select: "length (snd e) = Suc 0 \<Longrightarrow> (posterior \<lbrakk>\<rbrakk> (snd (the_elem (possible_steps drinks2 q0 Map.empty STR ''select'' (snd e))))) =
     (\<lambda>a. if a = V (R 2) then cexp.Eq (Num 0) else if a = V (R (Suc 0)) then cexp.Bc True else \<lbrakk>\<rbrakk> a)"
  apply (simp add: posterior_def)
  apply (simp add: possible_steps_q0)
  apply (simp add: select_def)
  apply (rule ext)
  apply simp
  by (smt One_nat_def Suc_1 aexp.inject(2) aexp.simps(18) n_not_Suc_n vname.inject(2) vname.simps(5))

lemma apply_updates_vend_nothing_2: "(EFSM.apply_updates (Updates vend_nothing)
           (case_vname Map.empty (\<lambda>n. if n = 2 then Some (Num 0) else if R n = R 1 then Some (hd (snd e)) else None))
           (\<lambda>a. if a = R 2 then Some (Num 0) else if a = R 1 then Some (hd (snd e)) else None)) = <R 2 \<mapsto> Num 0, R 1 \<mapsto> (hd (snd e))>"
  apply (rule ext)
  by (simp add: transitions)

lemma select_posterior_equiv: "\<lbrakk>V (R (Suc 0)) \<mapsto> cexp.Bc True, V (R 2) \<mapsto> cexp.Eq (Num 0)\<rbrakk> = select_posterior"
  apply (rule ext)
  by simp

lemma select_posterior_equiv_2: "(\<lambda>a. if a = V (R 2) then cexp.Eq (Num 0) else if a = V (R (Suc 0)) then cexp.Bc True else \<lbrakk>\<rbrakk> a) = select_posterior"
  apply (rule ext)
  by simp

lemma register_simp: "(\<lambda>x. if x = R (Suc 0)
          then aval (snd (R (Suc 0), V (R (Suc 0)))) (case_vname Map.empty (\<lambda>n. if n = 2 then Some (Num 0) else <R (Suc 0) := hd (snd e)> (R n)))
          else EFSM.apply_updates [(R 2, V (R 2))] (case_vname Map.empty (\<lambda>n. if n = 2 then Some (Num 0) else <R (Suc 0) := hd (snd e)> (R n)))
                <R (Suc 0) := hd (snd e), R 2 := Num 0> x) =  <R (Suc 0) := hd (snd e), R 2 := Num 0>"
  apply (rule ext)
  by simp

lemma observe_vend_nothing: "a = (STR ''vend'', []) \<Longrightarrow> (observe_all drinks2 q1 <R (Suc 0) := hd (snd e), R 2 := Num 0> (a # t)) = (vend_nothing, q1, [], <R (Suc 0) := hd (snd e), R 2 := Num 0>)#(observe_all drinks2 q1 <R (Suc 0) := hd (snd e), R 2 := Num 0> t)"
  apply simp
  apply (simp add: drinks2_vend_insufficient)
  apply (simp add: updates_vend_nothing outputs_vend_nothing)
  apply safe
   apply (rule ext)
   apply simp
  by (simp only: register_simp)

lemma error_trace: "t \<noteq> [] \<Longrightarrow> observe_all drinks2 q0 Map.empty t = [] \<Longrightarrow> observe_all drinks2 q0 Map.empty (t @ [e]) = []"
  sorry
  
lemma "valid_trace drinks2 p \<Longrightarrow> gets_us_to drinks2 q1 p \<Longrightarrow> p = (e#t) \<Longrightarrow> fst e = STR ''select'' \<and> length (snd e) = 1 \<Longrightarrow> t = map (\<lambda>e. (STR ''vend'', [])) t"
  apply (simp add: gets_us_to_def s0_drinks2)
  apply (simp add: possible_steps_q0 s0_drinks2 updates_select)



lemma gets_us_to_q1_anterior_context: "gets_us_to drinks2 q1 p \<Longrightarrow> anterior_context drinks2 p = \<lbrakk>V (R 1) \<mapsto> Bc True, V (R 2) \<mapsto> Eq (Num 0)\<rbrakk>"
proof (induct p rule: rev_induct)
  case Nil
  then show ?case
    by (simp add: s0_drinks2 gets_us_to_def)
next
  case (snoc e t)
  then show ?case
    apply (simp add: anterior_context_def gets_us_to_def s0_drinks2)
    apply (case_tac "observe_all drinks2 q0 Map.empty (t @ [e])")
     apply (simp add: s0_drinks2)
    apply simp
    apply (case_tac a)
    apply (simp add: s0_drinks2)
    apply (case_tac "observe_all drinks2 q0 Map.empty t")

     apply (simp add: s0_drinks2)
     apply (case_tac "t=[]")
      apply simp
      apply (case_tac "fst e = STR ''select'' \<and> length (snd e) = 1")
       apply (simp add: possible_steps_q0 s0_drinks2 select_posterior)
       apply (rule ext)
       apply simp
      apply (simp add: drinks2_q0_invalid s0_drinks2)
     apply (simp add: error_trace)
    apply (simp add: s0_drinks2)
    apply (case_tac ab)
    apply simp
    apply (case_tac "t")
     apply simp
    apply (case_tac "fst ad = STR ''select'' \<and> length (snd ad) = 1")
     apply (simp add: possible_steps_q0 s0_drinks2 select_posterior updates_select)

    









    (* apply (case_tac "e=(STR ''vend'', [])") *)
     (* apply (simp add: anterior_context_def) *)
     (* apply (simp add: aux1) *)

    (* apply (case_tac t) *)
     (* apply (simp add: anterior_context_def) *)
     (* apply (case_tac "fst e = STR ''select'' \<and> length (snd e) = 1") *)
      (* apply simp *)
      (* apply (simp add: possible_steps_q0 s0_drinks2 select_posterior) *)
      (* apply (rule ext) *)
      (* apply simp *)

     (* apply (simp add: drinks2_q0_invalid s0_drinks2) *)
     (* apply (rule ext) *)
     (* apply (simp add: gets_us_to_def) *)
     (* apply (simp add: drinks2_q0_invalid s0_drinks2) *)

    (* apply (case_tac "\<not> (fst a = STR ''select'' \<and> length (snd a) = 1)") *)
     (* apply (simp add: gets_us_to_def) *)
     (* apply (simp add: drinks2_q0_invalid s0_drinks2) *)

    (* apply simp *)
    (* apply (case_tac "list = []") *)
     (* apply simp *)
     (* apply (simp add: anterior_context_def s0_drinks2) *)
     (* apply (simp add: possible_steps_q0 select_posterior updates_select) *)
     (* apply (case_tac "\<not> (fst e = STR ''coin'' \<and> length (snd e) = 1)") *)
      (* apply (simp add: drinks2_q1_invalid) *)
      (* apply (rule ext) *)
      (* apply simp *)
     (* apply (simp add: gets_us_to_def) *)
     (* apply (simp add: s0_drinks2 possible_steps_q0 select_posterior updates_select) *)
     (* apply (simp add: drinks2_q1_coin) *)
    
    (* apply (case_tac "set list = {(STR ''vend'', [])}") *)
    

    (* apply (simp add: gets_us_to_def) *)
    (* apply (simp only: s0_drinks2) *)
    (* apply (simp add: possible_steps_q0) *)
    (* apply (simp only: updates_select) *)
    (* apply (case_tac list) *)
     (* apply simp *)
    (* apply (case_tac "\<not> (fst e = STR ''coin'' \<and> length (snd e) = 1)") *)
      (* apply (simp add: drinks2_q1_invalid) *)
      (* apply (simp add: anterior_context_def) *)
      (* apply (simp add: s0_drinks2 possible_steps_q0 updates_select) *)
      (* apply (simp add: drinks2_q1_invalid) *)

     (* apply simp *)
     (* apply (simp add: drinks2_q1_coin) *)

    (* apply simp *)




qed
  

lemma "directly_subsumes drinks2 q1 vend_fail vend_nothing"
  apply (simp add: directly_subsumes_def s0_drinks2)
  apply (simp add: gets_us_to_q1_anterior_context del: One_nat_def)
  using vend_fail_subsumes_vend_nothing
  by simp

lemma "merge_transitions \<lparr>s0 = q0, T = (merge_states q1 q2 (T drinks2))\<rparr> q1 q1 q1 vend_nothing vend_fail = None"
  oops

lemma "let t' = merge_states q1 q2 (T drinks2); t'a = merge_states q1 q1 t'
        in \<exists>x. nondeterministic_transitions t'a = Some x"
  apply (simp add: merge_q1_q2 nondeterministic_transitions_def nondeterminisitic_pairs)
  apply (simp only: merge_self_state)
  oops 

lemma "merge drinks2 q1 q2 = Some (\<lambda> (a,b) .
  if (a, b) = (q0, q1) then {|select|} else
  if (a, b) = (q1,q1) then {|vend_nothing, coin, vend_fail|} else
  if (a, b) = (q1, q3) then {|vend|} else {||})"
  apply (simp add: s0_drinks2)
  apply (case_tac "nondeterministic_transitions (merge_states q1 q2 (T drinks2))")
   apply (simp add: nond_transitions_not_none)
  apply clarify
  apply (simp add: only_nondeterminism_q1)
  apply (case_tac a)
     apply (simp add: no_nondeterminism_q0_2)
    prefer 2
    apply (simp add: no_nondeterminism_q2_2)
  prefer 2
   apply (simp add: no_nondeterminism_q3_2)
  apply simp
  apply (case_tac aa)
     apply simp
     apply (simp add: no_transitions_to_q0_2)
    apply simp
    apply (case_tac b)
       apply simp
       apply (simp add: no_transitions_to_q0_2)
      apply simp



  



end