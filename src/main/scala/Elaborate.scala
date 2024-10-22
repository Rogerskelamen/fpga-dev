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
    FirtoolOption(
      "--lowering-options=disallowLocalVariables" // for Vivado
//        + ",locationInfoStyle=wrapInAtSquareBracket" // for verilator
        + ",disallowPortDeclSharing,emitWireInPorts,emitBindComments"
        + ",disallowPackedArrays"
    ),
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptins ++ firtoolOptions
  (new ChiselStage).execute(args, executeOptions)
}
