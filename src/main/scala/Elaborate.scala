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
      "--lowering-options=disallowLocalVariables" // for Vivado
//        + ",locationInfoStyle=wrapInAtSquareBracket" // for verilator
        + ",disallowPortDeclSharing,emitWireInPorts,emitBindComments,omitVersionComment"
        + ",disallowPackedArrays" // for Yosys
    ),
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptins ++ firtoolOptions
  (new ChiselStage).execute(args, executeOptions)
}
