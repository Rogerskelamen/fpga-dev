import chisel3.emitVerilog
import circt.stage._

object Elaborate extends App {
  def top = new MemAccess

  // Generate HDL in verilog format
  val chiselStageOptins = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top),
    CIRCTTargetAnnotation(CIRCTTarget.Verilog)
  )
  val firtoolOptions = Seq(
    // For all lowering options, see https://github.com/llvm/circt/blob/main/include/circt/Support/LoweringOptions.h
    FirtoolOption(
      "--lowering-options=disallowLocalVariables" // for common verilog
//        + ",locationInfoStyle=wrapInAtSquareBracket" // for verilator
        + ",disallowPortDeclSharing,emitWireInPorts,emitBindComments,omitVersionComment"
        + ",mitigateVivadoArrayIndexConstPropBug" // for Vivado
        + ",disallowPackedArrays" // for Yosys
    ),
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptins ++ firtoolOptions
  val extraArg = Array("--target-dir", "verilog-gen")
  (new ChiselStage).execute(args ++ extraArg, executeOptions)
}
