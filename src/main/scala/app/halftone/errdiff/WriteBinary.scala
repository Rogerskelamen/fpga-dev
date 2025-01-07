package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.Decoupled
import tools.bus.BramNativePortFull

class WriteBinary(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ErrorOut2WriteBinary(config.posWidth)))
    val img = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out = Decoupled()
  })

  // Registers
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy

  // useless signals
  io.out.bits := DontCare

  /*
   * Write binary pixel to image storage
   */
  io.img.en   := io.in.fire
  io.img.we   := io.in.fire
  io.img.addr := io.in.bits.pos + config.ddrBaseAddr.U
  io.img.din  := Mux(io.in.bits.bval, 255.U(config.pixelWidth.W), 0.U(config.pixelWidth.W))

  when(busy) {
    // write completed
    resultValid := true.B
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
