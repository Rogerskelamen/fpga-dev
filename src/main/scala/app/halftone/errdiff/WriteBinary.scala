package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.{Decoupled, Fill}
import tools.bus.SimpleDataPortW

class WriteBinary(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new ErrorOut2WriteBinary(config.posWidth)))
    val write = new SimpleDataPortW(32, dwidth = config.pixelWidth)
    val out   = Decoupled(new Bundle {})
  })

  // Registers
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy

  io.out.bits := DontCare

  /*
   * Write binary pixel to ddr
   */
  io.write.req.valid := io.in.fire
  io.write.req.strb  := Fill(config.pixelWidth/8, 1.U(1.W))
  io.write.req.addr  := io.in.bits.pos + config.ddrBaseAddr.U
  io.write.req.data  := Mux(io.in.bits.bval, 255.U(config.pixelWidth.W), 0.U(config.pixelWidth.W))

  when(busy) {
    // write completed
    when(io.write.resp.valid) {
      resultValid := true.B
    }
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      busy := true.B
    }
  }
}
