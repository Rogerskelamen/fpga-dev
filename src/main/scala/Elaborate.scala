import circt.stage._

object Elaborate extends App {
  def top = new MemAccess

  // Generate HDL in verilog format
  val chiselStageOptins = Seq(
    chisel3.stage.ChiselGeneratorAnnotation(() => top),
    CIRCTTargetAnnotation(CIRCTTarget.Verilog)
  )
  val firtoolOptins = Seq(
    FirtoolOption("--disable-all-randomization")
  )

  val executeOptions = chiselStageOptins ++ firtoolOptins
  (new ChiselStage).execute(args, executeOptions)
}
