package app.reg

import chisel3._
import chisel3.util.Enum
import tools.bus.{SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

class ControlReg extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read = Flipped(new SimpleDataPortR(32, 32))
    val write = Flipped(new SimpleDataPortW(32, 32))
    // for debug
    val indicator = Output(Bool())
  })

  val ledReg = Reg(UInt(32.W))

  // FSM
  val sIdle :: sBusy :: Nil = Enum(2)
  // io.write.req.valid -> ledReg := io.write.req.bits
  when(io.write.req.addr === "h4000_0000".U) {
    ledReg := io.write.req.data
  }

  io.indicator := ledReg === 1.U
}
