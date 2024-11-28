import circt.stage._
import EmittedModule.ExposedModules

object Elaborate extends App {
  def generatDir: String = "verilog-gen"
  val firtoolOptions = Seq(
    // For all lowering options, see https://github.com/llvm/circt/blob/main/include/circt/Support/LoweringOptions.h
    FirtoolOption(
      "--lowering-options=disallowLocalVariables" // for common verilog
        // + ",locationInfoStyle=wrapInAtSquareBracket" // for verilator
        + ",disallowPortDeclSharing,emitWireInPorts,emitBindComments,omitVersionComment"
        + ",mitigateVivadoArrayIndexConstPropBug" // for Vivado
        + ",disallowPackedArrays" // for Yosys
    ),
    FirtoolOption("--disable-all-randomization")
  )
  val extraArgs = Array("--target-dir", generatDir)

  ExposedModules.foreach { moduleGen =>
    val chiselStageOptions = Seq(
      chisel3.stage.ChiselGeneratorAnnotation(moduleGen),
      // Generate HDL in verilog format
      CIRCTTargetAnnotation(CIRCTTarget.Verilog)
    )

    val executeOptions = chiselStageOptions ++ firtoolOptions

    (new ChiselStage).execute(args ++ extraArgs, executeOptions)
  }
}
