package tools

import chisel3._
import utils.FPGAModule

class AXIModule extends FPGAModule {
  val io = FlatIO(new Bundle {
    val axi = Flipped(new AXI4(dataBits = 32))
  })

  dontTouch(io)
  io <> DontCare
}
