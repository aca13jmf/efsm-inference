theory GExp
imports AExp Option_Logic
begin
datatype gexp = Bc bool | Eq aexp aexp | Gt aexp aexp | Nor gexp gexp | Null vname

abbreviation gNot :: "gexp \<Rightarrow> gexp" where
  "gNot g \<equiv> Nor g g"

abbreviation Lt :: "aexp \<Rightarrow> aexp \<Rightarrow> gexp" where
  "Lt a b \<equiv> Gt b a"

abbreviation
  Le :: "aexp \<Rightarrow> aexp \<Rightarrow> gexp" where
  "Le v va \<equiv> gNot (Gt v va)"

abbreviation
  Ge :: "aexp \<Rightarrow> aexp \<Rightarrow> gexp" where
  "Ge v va \<equiv> gNot (Lt v va)"

abbreviation
  Ne :: "aexp \<Rightarrow> aexp \<Rightarrow> gexp" where
  "Ne v va \<equiv> gNot (Eq v va)"

abbreviation gOr :: "gexp \<Rightarrow> gexp \<Rightarrow> gexp" where
  "gOr v va \<equiv> Nor (Nor v va) (Nor v va)"

lemma "\<not> (x \<or> y) = (\<not> x \<and> \<not> y)"
  by simp

abbreviation gAnd :: "gexp \<Rightarrow> gexp \<Rightarrow> gexp" where
  "gAnd v va \<equiv> Nor (Nor v v) (Nor va va)"

fun gval :: "gexp \<Rightarrow> datastate \<Rightarrow> bool option" where
  "gval (Bc b) _ = Some b" |
  "gval (Gt a\<^sub>1 a\<^sub>2) s = ValueGt (aval a\<^sub>1 s) (aval a\<^sub>2 s)" |
  "gval (Eq a\<^sub>1 a\<^sub>2) s = Some (aval a\<^sub>1 s = aval a\<^sub>2 s)" |
  "gval (Nor a\<^sub>1 a\<^sub>2) s = (case (gval a\<^sub>1 s, gval a\<^sub>2 s) of 
    (Some x, Some y) \<Rightarrow> Some (\<not> (x \<or> y)) |
    _ \<Rightarrow> None
  )" |
  "gval (Null v) s = Some (s v = None)"

lemma or_equiv: "gval (gOr x y) r = maybe_or (gval x r) (gval y r)"
  apply simp
  apply (cases "gval x r")
   apply (cases "gval y r")
    apply simp
   apply simp
  apply (cases "gval y r")
   apply simp
  by simp

lemma not_equiv: "maybe_not (gval x s) = gval (gNot x) s"
  apply simp
  apply (cases "gval x s")
   apply simp
  by simp

lemma nor_equiv: "gval (gNot (gOr a b)) s = gval (Nor a b) s"
  by (simp add: option.case_eq_if)

definition satisfiable :: "gexp \<Rightarrow> bool" where
  "satisfiable g \<equiv> (\<exists>s. gval g s = Some True)"

lemma not_satisfiable_gt_string: "\<not> satisfiable (Gt v (L (Str s)))"
  by (simp add: satisfiable_def)

definition gexp_valid :: "gexp \<Rightarrow> bool" where
  "gexp_valid g \<equiv> (\<forall>s. gval g s = Some True)"

definition gexp_equiv :: "gexp \<Rightarrow> gexp \<Rightarrow> bool" where
  "gexp_equiv a b \<equiv> \<forall>s. gval a s = gval b s"

lemma gexp_equiv_reflexive: "gexp_equiv x x"
  by (simp add: gexp_equiv_def)

lemma gexp_equiv_symmetric: "gexp_equiv x y \<Longrightarrow> gexp_equiv y x"
  by (simp add: gexp_equiv_def)

lemma gexp_equiv_transitive: "gexp_equiv x y \<and> gexp_equiv y z \<Longrightarrow> gexp_equiv x z"
  by (simp add: gexp_equiv_def)

lemma gval_subst: "gexp_equiv x y \<Longrightarrow> P (gval x s) \<Longrightarrow> P (gval y s)"
  by (simp add: gexp_equiv_def)

lemma gexp_equiv_satisfiable: "gexp_equiv x y \<Longrightarrow> satisfiable x = satisfiable y"
  by (simp add: gexp_equiv_def satisfiable_def)

lemma gAnd_reflexivity: "gexp_equiv (gAnd x x) x"
  apply (simp add: gexp_equiv_def)
  apply (rule allI)
  apply (case_tac "gval x s")
   apply simp
  by simp

lemma gAnd_zero: "gexp_equiv (gAnd (Bc True) x) x"
  apply (simp add: gexp_equiv_def)
  apply (rule allI)
  apply (case_tac "gval x s")
  by simp_all

lemma gAnd_symmetry: "gexp_equiv (gAnd x y) (gAnd y x)"
  apply (simp add: gexp_equiv_def)
  apply (rule allI)
  apply (case_tac "gval y s")
   apply (case_tac "gval x s")
    apply simp
   apply simp
  apply (case_tac "gval x s")
   apply simp
  by auto

lemma satisfiable_gAnd_self: "satisfiable (gAnd x x) = satisfiable x"
  by (simp add: gAnd_reflexivity gexp_equiv_satisfiable)

fun counterexample :: "gexp \<Rightarrow> gexp" where
  "counterexample (Eq x y) = Eq (Plus x (L (Num 1))) y" |
  "counterexample x = x"

(*This isn't true either because counterexample exists*)
lemma "gexp_equiv s t \<Longrightarrow> gexp_equiv (P s) (P t)"
  oops

end