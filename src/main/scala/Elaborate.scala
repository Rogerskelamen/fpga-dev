import app.led.PulseLight
import circt.stage._

import chisel3._
import utils.DownCounter

class DownCounterGen(val n: Int) extends Module {
  val io = IO(new Bundle {
    val wrap = Output(Bool())
  })
  io.wrap := DownCounter(n)
}

object Elaborate extends App {
  def top = new DownCounterGen(8)
  def generatDir: String = "verilog-gen"

  // Generate HDL in verilog format
  val chiselStageOptins = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top),
    CIRCTTargetAnnotation(CIRCTTarget.Verilog)
  )
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

  val executeOptions = chiselStageOptins ++ firtoolOptions
  val extraArg = Array("--target-dir", generatDir)
  (new ChiselStage).execute(args ++ extraArg, executeOptions)
}
