package utils

import chisel3._

/** Usage:
  * class FPGASubModule extends FPGAModule(true/false)
  * @param rstN whether reset is effective in low voltage
  */
abstract class FPGAModule(rstN: Boolean = true)
  extends RawModule with ImplicitClock with ImplicitReset {
  val fpga_clk = IO(Input(Clock()))
  val fpga_rst = IO(Input(Bool()))

  protected def clkFPGAName: String = "clk"
  protected def rstFPGAName: String = if (rstN) "rstn" else "rst"

  fpga_clk.suggestName(clkFPGAName)
  fpga_rst.suggestName(rstFPGAName)

  override protected def implicitClock: Clock = fpga_clk
  override protected def implicitReset: Reset = if (rstN) (~fpga_rst).asBool else fpga_rst

  final def wrapModuleWithRst[T <: RawModule](subModule: T): T = {
    withClockAndReset(fpga_clk, (~fpga_rst).asBool) { subModule }
  }
}
