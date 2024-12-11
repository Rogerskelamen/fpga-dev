package app.reg

import app.reg.ControlReg.{AWidth, DWidth}
import chisel3._
import chisel3.util.Enum
import tools.bus.{SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

object ControlReg {
  val AWidth = 32
  val DWidth = 32
}

class ControlReg extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read = Flipped(new SimpleDataPortR(AWidth, DWidth))
    val write = Flipped(new SimpleDataPortW(AWidth, DWidth))
    // for debug
    val indicator = Output(Bool())
  })

  val ledReg = Reg(UInt(DWidth.W))

  // FSM
  val sIdle :: sBusy :: Nil = Enum(2)
  // io.write.req.valid -> ledReg := io.write.req.bits
  when(io.write.req.addr === "h4000_0000".U) {
    ledReg := io.write.req.data
  }

  io.indicator := ledReg === 1.U
}
