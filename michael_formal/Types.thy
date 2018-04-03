theory Types
  imports "~~/src/HOL/IMP/Hoare" "Show.Show_Instances"
begin
type_synonym label = string
type_synonym arity = nat
type_synonym inputs = "int list"
type_synonym registers = state
type_synonym outputs = "int list"
type_synonym guard = "bexp"
type_synonym output_function = "aexp"
type_synonym update_function = "(string \<times> aexp)"
type_synonym statename = int
type_synonym trace = "(label \<times> inputs) list" (*Ideally written as label(i1, i2, ...)*)
type_synonym observation = "outputs list"

record transition =
  Label :: label
  Arity :: arity
  Guard :: "guard list"
  Outputs :: "output_function list"
  Updates :: "update_function list"

type_synonym destination = "(statename \<times> transition)"

record efsm =
  S :: "statename list"
  s0 :: statename
  T :: "(statename \<times> statename) \<Rightarrow> transition list"

definition join :: "state \<Rightarrow> state \<Rightarrow> state" where
  "join s1 s2 = (\<lambda>x. if (aval (V x) s1) \<noteq> 0 then (aval (V x) s1) else (aval (V x) s2))"

lemma "\<forall>z. \<exists>x y. (aval (V v) (join x y) = aval (V v) z)"
  apply (simp add: join_def)
  by auto

lemma "\<forall> x y. \<exists>z. (aval (V v) z) = aval (V v) (join x y)"
  by auto

lemmas shows_stuff = showsp_int_def showsp_nat.simps shows_string_def null_state_def

definition index :: "int \<Rightarrow> string" where
  "index i = ''i''@(showsp_int (nat i) i '''')"

lemma i1: "index 2 = ''i2''"
  by (simp add: shows_stuff index_def)

primrec input2state :: "int list \<Rightarrow> int \<Rightarrow> state" where
  "input2state [] _ = <>" |
  "input2state (h#t) i = (\<lambda>x. if x = (index i) then h else ((input2state t (i+1)) x))"

lemma "input2state [1, 2] 1 = <''i1'':=1, ''i2'':=2>"
  apply (rule ext)
  by (simp add: shows_stuff index_def)
end