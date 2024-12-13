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
  val pix = Reg(UInt((config.pixelWidth+2).W)) // expand 2 bit
  val err = Reg(SInt((config.errorWidth+2).W)) // expand 2 bit

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
  /** Explain these variable range
    * pix = [0, 255], err = [-127, 127], pix + err = [-127, 382]
    * Because err can be negative,
    * So make vars as SInt when calculating
    * make vars as UInt when storing
    */
  val realPix = pix.asSInt + err
  val binOut  = Mux(realPix < config.threshold.S, 0.U, 1.U)

  /*
   * Calculate errors to be diffused
   */
  elut.io.err := realPix - Mux(binOut.asBool, 255.S(config.pixelWidth.W), 0.S)

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
      err  := inBundle.err.asSInt
      busy := true.B
    }
  }
}
