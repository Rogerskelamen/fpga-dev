package app.halftone.errdiff

import chisel3._
import app.halftone.ErrDiffConfig
import chisel3.util.Decoupled

class ThreshCalc(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new PixelGet2ThreshCalc(config.pixelWidth, config.errorWidth, config.posWidth)))
    val out = Decoupled()
  })

  // Registers(for value storage and state present)
  val pos = Reg(UInt(config.posWidth.W))
  val pix = Reg(UInt(config.pixelWidth.W))
  val err = Reg(UInt(config.errorWidth.W))
  val bval = Reg(Bool()) // output binary value
  val errOut = Reg(Vec(4, SInt(config.errorWidth.W)))
  val busy = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready := !busy
  io.out.bits := DontCare

  val elut = Module(new ELUT(config.errorWidth))

  /*
   * Compare with Threshold
   */
  val binOut = Mux(io.in.bits.pix + io.in.bits.err < 128.U, 0.U, 1.U)

  /*
   * Calculate errors to be diffused
   */
}
