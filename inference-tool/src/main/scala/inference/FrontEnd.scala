import scala.io.Source
import java.io._
import org.apache.commons.io.FilenameUtils
import scala.collection.mutable.ListBuffer
import Types._

object FrontEnd {
  def main(args: Array[String]): Unit = {
    val t1 = System.nanoTime
    Config.parseArgs(args)

    Log.root.info(args.mkString(" "))
    Log.root.info(s"Building PTA - ${Config.config.log.length} ${if (Config.config.log.length == 1) "trace" else "traces"}")

    var pta: EFSM.efsm_ext[Unit] = null;

    if (Config.config.abs) {
      pta = Inference.make_pta_abstract(Config.config.log, new EFSM.efsm_exta[Unit](FSet.bot_fset, FSet.bot_fset, ()))
    }
    else {
      pta = Inference.make_pta(Config.config.log, new EFSM.efsm_exta[Unit](FSet.bot_fset, FSet.bot_fset, ()))
    }
    Config.numStates = Code_Numeral.integer_of_nat(FSet.size_fset(EFSM.S(pta)))
    Config.ptaNumStates = Config.numStates

    Log.root.info(s"PTA has ${Config.numStates} states and ${Code_Numeral.integer_of_nat(FSet.size_fset(EFSM.T(pta)))} transitions")

    PrettyPrinter.EFSM2dot(pta, s"pta_gen")

    try {
      val inferred = Inference.learn(
        Nat.Nata(Config.config.k),
        pta,
        Config.config.log,
        Config.config.strategy,
        Config.heuristics,
        Config.config.nondeterminismMetric)

        // TypeConversion.doubleEFSMToSALTranslator(pta, "pta", inferred, "vend1", "compositionTest")

        Log.root.info("The inferred machine is " +
          (if (Inference.nondeterministic(Inference.toi_efsm(inferred), Inference.nondeterministic_pairs))
            "non"
            else "") + "deterministic")

        val basename = (if (Config.config.outputname == null) (FilenameUtils.getBaseName(Config.config.file.getName()).replace("-", "_")) else Config.config.outputname.replace("-", "_"))
        TypeConversion.efsmToSALTranslator(inferred, basename)

        PrettyPrinter.EFSM2dot(inferred, s"${basename}_gen")
        val seconds = (System.nanoTime - t1) / 1e9d
        val minutes = (seconds / 60) % 60
        val hours = seconds / 3600
        Log.root.info(s"States: ${Code_Numeral.integer_of_nat(FSet.size_fset(EFSM.S(inferred)))}")
        Log.root.info(s"Transitions: ${Code_Numeral.integer_of_nat(FSet.size_fset(EFSM.T(inferred)))}")
        Log.root.info(s"Completed in ${if (hours > 0) s"${hours.toInt}h " else ""}${if (minutes > 0) s"${minutes.toInt}m " else ""}${seconds % 60}s")
    }
    catch {
      case e: Throwable => {
        val sw: StringWriter = new StringWriter()
        e.printStackTrace(new PrintWriter(sw));
        Log.root.error(sw.toString())
      }
    }
  }
}
