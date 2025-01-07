package app.halftone

import chisel3._
import tools.bus.{BramNativePortFull, SimpleDataPortR}
import utils.FPGAModule

class HalftoneArbiter(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val mem = new SimpleDataPortR(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val core = new BramNativePortFull(config.bramDataBits, config.bramAddrBits)
    val pa = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
  })
}
