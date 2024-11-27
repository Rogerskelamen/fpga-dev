package app.mem

import chisel3._
import utils.FPGAModule

class ResetTestModule extends FPGAModule {
  val io = FlatIO(new Bundle {
    val m_in = Input(Bool())
    val m_out = Output(Bool())
  })

  val reg = RegInit(false.B)
  when(!io.m_in) {
    reg := true.B
  }

  io.m_out := fpga_rst.asBool
}
