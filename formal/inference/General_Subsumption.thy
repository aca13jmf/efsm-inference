theory General_Subsumption
imports "../Contexts" Trace_Matches
begin

declare One_nat_def [simp del]

lemma ctx_simp: "(\<lambda>r. and (if r = V (I i) then snd (V (I i), cexp.Eq s) else cexp.Bc True) (\<lbrakk>\<rbrakk> r)) = \<lbrakk>V (I i) \<mapsto> Eq s\<rbrakk>"
  apply (rule ext)
  by simp

lemma incoming_transition_alt_def: "incoming_transition_to e n = (\<exists>t from. ((from, n), t) |\<in>| e)"
  apply (simp add: incoming_transition_to_def)
  apply (simp add: ffilter_def fset_both_sides Abs_fset_inverse)
  apply (simp add: fmember_def)
  apply (simp add: Set.filter_def)
  by auto

lemma no_step_to: "\<not> incoming_transition_to t m \<Longrightarrow>
       step t n r aa b \<noteq> Some (uw, m, ux, r')"
  apply (simp add: incoming_transition_alt_def step_def)
  apply safe
  apply (case_tac "fthe_elem (possible_steps t n r aa b)")
  apply simp
  using singleton_dest by blast

lemma no_route_to_no_access: "\<not> incoming_transition_to t 0 \<Longrightarrow> \<forall>r s. s \<noteq> 0 \<longrightarrow> \<not>gets_us_to 0 t s r p"
proof(induct p)
  case Nil
  then show ?case
    by (simp add: no_further_steps)
next
  case (Cons a p)
  then show ?case
    apply clarify
    apply (rule gets_us_to.cases)
       apply simp
      apply simp
     apply clarify
     apply simp
     apply (metis no_step_to Cons.hyps)
    by simp
qed

lemma no_return_to_initial: "\<not> incoming_transition_to t 0 \<Longrightarrow> accepts_trace t p \<and> gets_us_to 0 t 0 Map.empty p \<Longrightarrow> p = []"
proof(induct p)
  case Nil
  then show ?case
    by simp
next
  case (Cons a p)
  then show ?case
    apply (simp add: accepts_trace_def)
    apply (rule gets_us_to.cases)
       apply auto[1]
      apply simp
     apply clarify
     apply simp
     apply (case_tac "s' = 0")
      apply (simp add: no_step_to)
     apply (simp add: no_route_to_no_access)
    apply clarify
    by (simp add: no_step_none)
qed

lemma anterior_context_no_return_to_zero: "\<not> incoming_transition_to t 0 \<Longrightarrow> accepts_trace t p \<and> gets_us_to 0 t 0 Map.empty p \<longrightarrow> anterior_context t' p = \<lbrakk>\<rbrakk>"
proof(induct p)
  case Nil
  then show ?case
    by (simp add: anterior_context_def)
next
  case (Cons a p)
  then show ?case
    using no_return_to_initial
    by auto
qed

lemma inconsistent_c_aux: "gval (cexp2gexp r (c r)) sa \<noteq> Some True \<Longrightarrow>
       (r = V (I i) \<longrightarrow> gval (cexp2gexp (V (I i)) (and (cexp.Eq s) (c (V (I i))))) sa \<noteq> Some True) \<and>
             (r \<noteq> V (I i) \<longrightarrow> gval (cexp2gexp r (c r)) sa \<noteq> Some True)"
proof-
  assume premise: "gval (cexp2gexp r (c r)) sa \<noteq> Some True"
  show ?thesis
    apply (simp only: gval_and)
    apply standard
    using gval_gAnd_True premise apply blast
    by (simp add: premise)
qed

lemma inconsistent_c: "\<not>consistent c \<Longrightarrow> \<not>consistent (\<lambda>r. and (if r = V (I i) then snd (V (I i), cexp.Eq s) else cexp.Bc True) (c r))"
proof-
  assume premise: "\<not> consistent c"
  show ?thesis
    using premise
    apply (simp add: consistent_def)
    unfolding cval_def
    apply (simp only: gval_And gval_gAnd maybe_and_true)
    by metis
qed

lemma not_undef_gval: "\<forall>r. c r = Undef \<or> gval (cexp2gexp r (c r)) s = Some True \<Longrightarrow>
         c (V (I i)) \<noteq> Undef \<Longrightarrow> gval (cexp2gexp (V (I i)) (c (V (I i)))) s = Some True"
  by auto

lemma ctx_simp2: "and (if r = i then snd (i, g) else cexp.Bc True) (c r) = 
       (if r = i then and g (c r) else c r)"
  by auto

lemma test2: "consistent (\<lambda>r. and (if r = V (I i) then snd (V (I i), cexp.Eq s) else cexp.Bc True) (c r)) \<Longrightarrow> consistent c"
  using inconsistent_c
  by auto

lemma gval_and_eq: "gval (cexp2gexp r (c r)) s \<noteq> Some True \<Longrightarrow> gval (cexp2gexp r (if r = i then and (cexp.Eq v) (c r) else c r)) s \<noteq> Some True"
  apply simp
  apply (simp only: gval_and gval_gAnd maybe_and_not_true)
  by auto

lemma test5: "gval (cexp2gexp r (if r = VIi then and c1 (c r) else c r)) s = Some True \<Longrightarrow>
           gval (cexp2gexp r (c r)) s = Some True"
  apply (case_tac "r = VIi")
   apply simp
   apply (simp only: gval_and gval_gAnd)
  using maybe_and_not_true apply blast
  by simp

lemma "gval (cexp2gexp uu (and a b)) s = maybe_and (gval (cexp2gexp uu a) s) (gval (cexp2gexp uu b) s)"
  by (simp add: gval_and)

lemma consistent_i_and: "consistent (\<lambda>r. if r = i then and x (c r) else c r) \<Longrightarrow> consistent c"
proof-
  assume premise: "consistent (\<lambda>r. if r = i then and x (c r) else c r)"
  have aux: "\<And>r i c s x. gval (cexp2gexp r (if r = i then and x (c r) else c r)) s = Some True \<Longrightarrow> gval (cexp2gexp r (c r)) s = Some True"
    apply (case_tac "r \<noteq> i")
     apply simp+
    apply (simp only: gval_and gval_gAnd)
    using maybe_and_not_true by blast
  show ?thesis
    using premise
    apply (simp add: consistent_def cval_def)
    apply clarify
    apply (rule_tac x=s in exI)
    apply clarify
    using aux
    by blast
qed

lemma gval_if_split: "(\<forall>r. gval (cexp2gexp r (if r = vIi then and c1 (c r) else c r)) s = Some True) =
((gval (cexp2gexp (vIi) (and c1 (c (vIi)))) s = Some True) \<and>
(\<forall>r. gval (cexp2gexp r (c r)) s = Some True))"
  apply safe
    apply (metis (full_types))
  using test5 apply blast
  by simp

lemma inconsistent_c_inconsistent_and: "\<not> consistent c \<Longrightarrow> \<not>consistent (\<lambda>r. if r = a then and c1 (c r) else c r)"
  apply (simp add: consistent_def cval_def)
  apply clarify
  apply (simp only: gval_and gval_gAnd)
  using maybe_and_true by fastforce

lemma generalise_subsumption_aux_1: "\<not>consistent c \<Longrightarrow> \<not>consistent (\<lambda>r. if r = v then and (c r) ce else pairs2context [] c r)"
  apply (simp add: consistent_def)
  apply (simp only: cval_And maybe_and_true)
  by auto

lemma generalise_subsumption: "c (V (R ri)) = Undef \<Longrightarrow> 
                               c (V (I i)) = Bc True \<Longrightarrow> 
                               subsumes c (remove_guard_add_update \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L v)], Outputs=[], Updates=[]\<rparr> i ri)
                                                                   \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L v)], Outputs=[], Updates=[]\<rparr>"
  apply (simp add: subsumes_def remove_guard_add_update_def ctx_simp2 medial_def)
  apply safe
    apply (simp add: cval_true)
   apply (simp add: posterior_def Let_def medial_def)
   apply (case_tac "consistent (\<lambda>r. if r = V (I i) then and (c r) (cexp.Eq v) else pairs2context [] c r)")
    apply (simp add: remove_input_constraints_alt)
    apply (case_tac "constrains_an_input r")
     apply simp+
    apply (simp add: consistent_def)
    apply clarify
  apply (metis (full_types) constrains_an_input.simps(3) pairs2context.simps(1))
   apply simp
  apply (simp add: posterior_def Let_def medial_def)
  apply (case_tac "consistent (\<lambda>r. if r = V (I i) then and (c r) (cexp.Eq v) else pairs2context [] c r)")
   apply (simp add: remove_input_constraints_alt inconsistent_false)
   apply standard
    apply (simp add: consistent_def)
    apply clarify
    apply (rule_tac x=s in exI)
  using valid_def valid_true apply presburger
  using generalise_subsumption_aux_1
   apply auto[1]
  by (simp add: inconsistent_false)

lemma remove_guard_add_update:  "\<lparr>Label=l, Arity=a, Guard=[], Outputs=[], Updates=[(R r, (V (I i)))]\<rparr> = remove_guard_add_update \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L s)], Outputs=[], Updates=[]\<rparr> i r"
  by (simp add: remove_guard_add_update_def)

lemma generalise_subsumption_empty: "subsumes \<lbrakk>\<rbrakk> \<lparr>Label=l, Arity=a, Guard=[], Outputs=[], Updates=[(R r, (V (I i)))]\<rparr> 
                  \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L s)], Outputs=[], Updates=[]\<rparr>"
  using remove_guard_add_update empty_variable_constraints generalise_subsumption
  by simp

lemma "subsumes \<lbrakk>\<rbrakk> (remove_guard_add_update \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L s)], Outputs=[], Updates=[]\<rparr> i r)
                  \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L s)], Outputs=[], Updates=[]\<rparr>"
  using remove_guard_add_update empty_variable_constraints generalise_subsumption
  by simp

lemma "\<not> incoming_transition_to t 0 \<Longrightarrow> directly_subsumes t t' 0 \<lparr>Label=l, Arity=a, Guard=[], Outputs=[], Updates=[(R r, (V (I i)))]\<rparr> 
                                                                 \<lparr>Label=l, Arity=a, Guard=[GExp.Eq (V (I i)) (L s)], Outputs=[], Updates=[]\<rparr>"
  apply (simp add: directly_subsumes_def anterior_context_no_return_to_zero)
  apply (simp add: generalise_subsumption_empty)
  using generalise_subsumption_empty
  by blast

lemma generalise_output_preserves_label: "Label (generalise_output t r p) = Label t"
  by (simp add: generalise_output_def)

lemma generalise_output_preserves_arity: "Arity (generalise_output t r p) = Arity t"
  by (simp add: generalise_output_def)

lemma generalise_output_preserves_output_length: "length (Outputs (generalise_output t r p)) = length (Outputs t)"
  by (simp add: generalise_output_def)

lemma generalise_output_preserves_guard: "Guard (generalise_output t r p) = Guard t"
  by (simp add: generalise_output_def)

lemma generalise_output_preserves_updates: "Updates (generalise_output t r p) = Updates t"
  by (simp add: generalise_output_def)

lemmas generalise_output_preserves = generalise_output_preserves_label generalise_output_preserves_arity
generalise_output_preserves_output_length generalise_output_preserves_guard

lemma "nth (Outputs t) p = L v \<Longrightarrow> c (V (R r)) = Eq v  \<Longrightarrow> subsumes c (generalise_output t r p) t"
  apply (simp add: subsumes_def generalise_output_preserves)
  apply safe
     prefer 2
     apply (simp add: generalise_output_def)
     apply (rule_tac x=i in exI)
     apply (rule_tac x="<R r := v>" in exI)
     apply simp
  oops

lemma posterior_input: "(posterior \<lbrakk>\<rbrakk> aaa) (V(I i)) = Bc False \<Longrightarrow> (posterior \<lbrakk>\<rbrakk> aaa) = (\<lambda>x. Bc False)"
  apply (simp add: posterior_def Let_def)
  apply (case_tac "consistent (medial \<lbrakk>\<rbrakk> (Guard aaa))")
   apply simp
   apply (simp add: remove_input_constraints_def)
  by simp

lemma filter_not_f_a: " \<not> f a \<Longrightarrow> filter f g = filter f (a#g)"
  by simp

lemma and_false_not_undef: "and (cexp.Bc False) c \<noteq> Undef"
  apply (induct_tac c)
        apply simp
       apply (case_tac x)
  by auto

lemma and_And_false: "x \<noteq> cexp.Bc True \<and> x \<noteq> Bc False \<Longrightarrow> and (Bc False) x = And (Bc False) x"
  apply (case_tac x)
  by auto

lemma cval_medial_true_requires_cval_anterior_true: "cval (medial c g r) r i = Some True \<Longrightarrow> cval (c r) r i = Some True"
proof(induct g)
  case Nil
  then show ?case by (simp add: medial_def)
next
  case (Cons a g)
  then show ?case
    apply (simp add: medial_def)
    using cval_pairs2context_not_true by blast
qed

lemma cval_pairs2context_true_cval_true: "cval (pairs2context G m x) y i = Some True \<Longrightarrow> cval (m x) y i = Some True"
proof(induct G)
  case Nil
  then show ?case by simp
next
  case (Cons a G)
  then show ?case
    apply (case_tac a)
    apply simp
    apply (case_tac "x = aa")
     apply simp
     apply (simp only: cval_And maybe_and_true)
    by simp
qed

lemma cval_medial_var_update: "cval (medial m g x) y i = Some True \<Longrightarrow>
             cval (m x) y i = Some True"
  by (simp add: medial_def cval_pairs2context_true_cval_true)

lemma cval_conjoin: "cval (Contexts.conjoin c c' r) aa s = maybe_and (cval (c r) aa s) (cval (c' r) aa s)"
  by (simp add: cval_def)

lemma gval_conjoin:  "gval (cexp2gexp aa (Contexts.conjoin c c' r)) s = maybe_and (gval (cexp2gexp aa (c r)) s) (gval (cexp2gexp aa (c' r)) s)"
  by (simp only: conjoin.simps gval_and gval_gAnd)

lemma cval_none_cval_pairs2context_none: "cval (c r) a s = None \<Longrightarrow> cval (pairs2context G c r) a s = None"
proof(induct G)
  case Nil
  then show ?case by simp
next
  case (Cons a G)
  then show ?case
    apply (case_tac a)
    apply simp
    apply (simp only: cval_And)
    by auto
qed

lemma cval_medial_none: "cval (c r) a s = None \<Longrightarrow> cval (medial c g r) a s = None"
  by (simp add: medial_def cval_none_cval_pairs2context_none)

lemma and_true: "and c (cexp.Bc True) = c"
  apply (case_tac c)
        apply simp
       apply (case_tac x2)
  by auto

lemma and_self: "and x x = x"
  apply (case_tac x)
        apply simp
       apply (case_tac x2)
  by auto

lemma conjoin_true:  "Contexts.conjoin (\<lambda>i. cexp.Bc True) x = x"
  by simp

lemma "cval (pairs2context (guard2pairs c (G)) c r) a s = Some True \<Longrightarrow>
    aa \<Longrightarrow>
    cval
     (pairs2context (guard2pairs (pairs2context (guard2pairs c (G)) c) (G))
       (pairs2context (guard2pairs c (G)) c) r)
     a s =
    Some True"

lemma "cval (medial c g r) a s = Some True \<Longrightarrow> aa \<Longrightarrow> cval (medial (medial c g) g r) a s = Some True"
  apply (simp add: medial_def)


lemma "context_equiv (medial (medial c g) g) (medial c g)"
  apply (simp add: context_equiv_def cexp_equiv_def)
  apply (rule allI)+
  apply (case_tac "cval (medial c g r) a s")
   apply (simp add: cval_medial_none)
  apply (case_tac aa)
   apply simp

lemma test: "(cval (Contexts.apply_updates (medial (medial c g) g) (medial c g) u r) r i = Some True \<Longrightarrow>
        cval (Contexts.apply_updates (medial c g) c u r) r i = Some True) \<Longrightarrow>
       cval (apply_update (medial (medial c g) g) (Contexts.apply_updates (medial (medial c g) g) (medial c g) u) (aa, b) r) r i =
       Some True \<Longrightarrow>
       cval (apply_update (medial c g) (Contexts.apply_updates (medial c g) c u) (aa, b) r) r i = Some True"
proof(induct b)
  case (L x)
  then show ?case
    by auto
next
  case (V x)
  then show ?case
    using cval_medial_var_update by auto
next
  case (Plus b1 b2)
  then show ?case
    apply (case_tac "r \<noteq> V aa")
     apply simp+





next
  case (Minus b1 b2)
  then show ?case sorry
qed

lemma last: "cval (Contexts.apply_updates (medial (medial c g) g) (medial c g) u r) r i = Some True \<Longrightarrow>
       cval (Contexts.apply_updates (medial c g) c u r) r i = Some True"
proof(induct u)
  case Nil
  then show ?case
    by (simp add: cval_medial_true_requires_cval_anterior_true)
next
  case (Cons a u)
  then show ?case
    apply simp
    apply (case_tac a)
    apply simp
    sorry
qed

lemma "subsumes c t t"
  apply (simp add: subsumes_def)
  apply safe
  apply (simp add: posterior_def Let_def)
  apply (case_tac "consistent (medial (medial c (Guard t)) (Guard t))")
   apply (simp add: consistent_medial_requires_consistent_anterior remove_input_constraints_alt)
  apply (case_tac "constrains_an_input r")
    apply simp
   apply simp
   defer
  using CExp.satisfiable_def unsatisfiable_false apply auto[1]
  by (simp add: last)
end