import com.microsoft.z3
import exceptions._
import java.io._
import scala.io.Source
import scala.util.Random
import sys.process._
import Types._

object Dirties {

  def foldl[A, B](f: A => B => A, b: A, l: List[B]): A =
    l.par.foldLeft(b)(((x, y) => (f(x))(y)))

  def toZ3(a: VName.vname): String = a match {
    case VName.I(n) => s"i${Code_Numeral.integer_of_nat(n)}"
    case VName.R(n) => s"r${Code_Numeral.integer_of_nat(n)}"
  }

  def toZ3(a: AExp.aexp): String = a match {
    case AExp.L(Value.Numa(n)) => Code_Numeral.integer_of_int(n).toString
    case AExp.L(Value.Str(s)) => "\"" + s + "\""
    case AExp.V(v) => s"${toZ3(v)}Value"
    case AExp.Plus(a1, a2) => s"(+ (${toZ3(a1)}) (${toZ3(a2)}))"
    case AExp.Minus(a1, a2) => s"(- (${toZ3(a1)}) (${toZ3(a2)}))"
  }

  def toZ3(a: Type_Inference.typea): String = a match {
    case Type_Inference.NUM() => "Int"
    case Type_Inference.STRING() => "String"
    case Type_Inference.UNBOUND() => "String"
  }

  def toZ3(g: GExp.gexp, types: Map[VName.vname, Type_Inference.typea]): String = g match {
    case GExp.Bc(a) => a.toString()
    case GExp.Eq(a1, a2) => s"(= ${toZ3(a1)} ${toZ3(a2)})"
    case GExp.Gt(a1, a2) => s"(> ${toZ3(a1)} ${toZ3(a2)})"
    case GExp.Nor(g1, g2) => if (g1 == g2) s"(not ${toZ3(g1, types)})" else s"(not (or ${toZ3(g1, types)} ${toZ3(g2, types)}))"
    case GExp.Null(AExp.V(VName.I(n))) => s"(= i${Code_Numeral.integer_of_nat(n)} (as none (Option ${toZ3(types(VName.I(n)))})))"
    case GExp.Null(AExp.V(VName.R(n))) => s"(= r${Code_Numeral.integer_of_nat(n)} (as none (Option ${toZ3(types(VName.I(n)))})))"
    case GExp.Null(v) => throw new java.lang.IllegalArgumentException("Z3 does not handle null of more complex arithmetic expressions")
  }

  var sat_memo = scala.collection.immutable.Map[GExp.gexp, Boolean]()

  def satisfiable(g: GExp.gexp): Boolean = {
    if (sat_memo isDefinedAt g) {
      return sat_memo(g)
    } else {
      val maybe_types = Type_Inference.infer_types(g)
      maybe_types match {
        case None => false
        case Some(types) => {
          var z3String = s"(declare-datatype Option (par (X) ((none) (some (val X)))))\n" +
            types.map(t => t match {
              case (k, v) => s"(declare-const ${toZ3(k)} (Option ${toZ3(v)}))\n(declare-const ${toZ3(k)}Value (${toZ3(v)}))"
            }).foldLeft("")(((x, y) => x + y + "\n")) +
            s"(assert ${toZ3(g, types)})\n(check-sat)"

          // Log.root.debug(g.toString)
          // Log.root.debug(z3String)

          val ctx = new z3.Context()
          val solver = ctx.mkSimpleSolver()
          solver.fromString(z3String)
          val sat = solver.check()
          ctx.close()
          return sat == z3.Status.SATISFIABLE
        }
      }
    }
  }

  // def randomMember[A](f: FSet.fset[A]): Option[A] = f match {
  //   case FSet.Abs_fset(s) => s match {
  //     case Set.seta(l) => {
  //       if (l == List()) {
  //         None
  //       }
  //       else {
  //         Some(Random.shuffle(l).head)
  //       }
  //     }
  //   }
  // }

  def randomMember[A](f: FSet.fset[A]): Option[A] = f match {
    case FSet.fset_of_list(l) =>
      if (l == List()) {
        None
      } else {
        Some(Random.shuffle(l).head)
      }
  }

  def addLTL(f: String, e: String) = {
    val lines = Source.fromFile(f).getLines().toList.dropRight(1)

    val pw = new PrintWriter(new File(f))

    (lines :+ (e + "\nEND")).foreach(pw.println)

    pw.close()
  }

  // We're checking for the existence of a trace that gets us to the right
  // states in the respective machines such that we can take t2 so we check
  // the global negation of this to see if there's a counterexample
  def alwaysDifferentOutputsDirectSubsumption[A](
    e1: IEFSM,
    e2: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat,
    t2: Transition.transition_ext[A]): Boolean = {
    val f = "intermediate_" + System.currentTimeMillis()
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME O TO O_e1 IN e1) || (RENAME O TO O_e2 IN e2);\n" +
      s"canTake: THEOREM composition |- G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => NOT(input_sequence ! size?(I) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal(Transition.Guard(t2))}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{100}!canTake'").!!
    if (!output.toString.startsWith("Counterexample")) {
      println(s"G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => NOT(input_sequence ! size?(I) = ${Code_Numeral.integer_of_nat(Transition.Arity(t2))} AND ${efsm2sal.guards2sal(Transition.Guard(t2))}));")
      println(s"sal-smc --assertion='${f}{100}!canTake'")
    }
    return (output.toString.startsWith("Counterexample"))
  }

  def initiallyUndefinedContextCheck(e: IEFSM, r: Nat.nat, s: Nat.nat): Boolean = {
    println("initiallyUndefinedContextCheck")
    val f = "intermediate_" + System.currentTimeMillis()
    TypeConversion.efsmToSALTranslator(Inference.tm(e), f)

    addLTL("salfiles/" + f + ".sal", s"  initiallyUndefined: THEOREM MichaelsEFSM |- G(cfstate = State_${Code_Numeral.integer_of_nat(s)} => r_${Code_Numeral.integer_of_nat(r)} = value_option ! None);")

    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{100}!initiallyUndefined'").!!
    if (output.toString != "proved.\n") {
      print(output)
    }
    return (output.toString == "proved.\n")
  }

  def generaliseOutputContextCheck(v: Value.value, r: Nat.nat, s1: Nat.nat, s2: Nat.nat, e1: IEFSM, e2: IEFSM): Boolean = {
    val f = "intermediate_" + System.currentTimeMillis()
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(e1), "e1", Inference.tm(e2), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME O TO O_e1 IN e1) || (RENAME O TO O_e2 IN e2);\n" +
      s"checkRegValue: THEOREM composition |- G(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)} => r_${Code_Numeral.integer_of_nat(r)}.2 = Some(${TypeConversion.salValue(v)}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{100}!checkRegValue'").!!
    if (output.toString != "proved.\n") {
      print(output)
    }
    return (output.toString == "proved.\n")
  }

  // Here we check to see if globally we can never be in both states
  // If there's a counterexample then there exists a trace which gets us to
  // both states
  def acceptsAndGetsUsToBoth(a: IEFSM,
    b: IEFSM,
    s1: Nat.nat,
    s2: Nat.nat): Boolean = {
    val f = "intermediate_" + System.currentTimeMillis()
    TypeConversion.doubleEFSMToSALTranslator(Inference.tm(a), "e1", Inference.tm(b), "e2", f)
    addLTL(s"salfiles/${f}.sal", s"composition: MODULE = (RENAME O TO O_e1 IN e1) || (RENAME O TO O_e2 IN e2);\n" +
      s"getsUsToBoth: THEOREM composition |- G(NOT(cfstate.1 = ${TypeConversion.salState(s1)} AND cfstate.2 = ${TypeConversion.salState(s2)}));")
    val output = Seq("bash", "-c", s"cd salfiles; sal-smc --assertion='${f}{100}!getsUsToBoth'").!!
    return (output.toString.startsWith("Counterexample"))
  }

  def scalaDirectlySubsumes(a: IEFSM,
    b: IEFSM,
    s: Nat.nat,
    s_prime: Nat.nat,
    t1: Transition.transition_ext[Unit],
    t2: Transition.transition_ext[Unit]): Boolean = {
    if (Store_Reuse_Subsumption.generalise_output_direct_subsumption(t2, t1, b, a, s, s_prime)) {
      // println("n")
      return false
    }
    // else {
    println(s"Does ${PrettyPrinter.transitionToString(t1)} directly subsume ${PrettyPrinter.transitionToString(t2)}? (y/N)")
    val subsumes = readLine("") == "y"
    subsumes
    // }
  }
}
