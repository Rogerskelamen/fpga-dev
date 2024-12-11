package app.reg

import app.reg.ControlReg.{AWidth, DWidth}
import chisel3._
import tools.bus.{SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

object ControlReg {
  val AWidth = 32
  val DWidth = 32
}

class ControlReg extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read  = Flipped(new SimpleDataPortR(AWidth, DWidth))
    val write = Flipped(new SimpleDataPortW(AWidth, DWidth))
    // for debug
    val indicator = Output(Bool())
  })

  io.read <> DontCare

  val ledReg = RegInit(0.U(DWidth.W))
  val busy   = RegInit(false.B)
  io.write.resp.valid := busy
  io.write.resp.data := 0.U

  when(busy) {
    busy := false.B
  }.otherwise {
    when(io.write.req.valid) {
      when(io.write.req.addr === "h4000_0000".U) {
        ledReg := io.write.req.data
      }
      busy := true.B
    }
  }

  io.indicator := ledReg === 1.U
}
