package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.Decoupled

class ThreshCalc(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new PixelGet2ThreshCalc(config.pixelWidth, config.errorWidth, config.posWidth)))
    val out = Decoupled(new ThreshCalc2ErrorOut(config.posWidth, config.errorWidth))
  })

  // Registers(for value storage and state presentation)
  val pos = Reg(UInt(config.posWidth.W))
  val pix = Reg(UInt(config.pixelWidth.W))
  val err = Reg(UInt(config.errorWidth.W))
//  val bval = Reg(Bool()) // output binary value
//  val errOut = Reg(Vec(4, SInt(config.errorWidth.W)))
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy

  /*
   * Lookup Table
   */
  val elut = Module(new ELUT(config.errorWidth))

  /*
   * Compare with Threshold
   */
  val binOut = Mux(pix + err < config.threshold.U, 0.U, 1.U)

  /*
   * Calculate errors to be diffused
   */
  elut.io.err := err

  // Emit outputs
  io.out.bits.pos    := pos
  io.out.bits.bval   := binOut
  io.out.bits.errOut := elut.io.out

  when(busy) {
    resultValid := true.B
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      val inBundle = io.in.deq()
      pos  := inBundle.pos
      pix  := inBundle.pix
      err  := inBundle.err
      busy := true.B
    }
  }
}
