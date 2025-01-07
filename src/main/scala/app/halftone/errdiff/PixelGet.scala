package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.Decoupled
import tools.bus.BramNativePortFull

class PixelGet(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val pos = UInt(config.posWidth.W)
    }))
    val img   = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val cache = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out   = Decoupled(new PixelGet2ThreshCalc(config.pixelWidth, config.errorWidth, config.posWidth))
  })

  // Registers(for value storage and state presentation)
  val pos         = Reg(UInt(config.posWidth.W))
  val pix         = Reg(UInt(config.pixelWidth.W))
  val err         = Reg(UInt(config.errorWidth.W))
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy
  // useless signals
  io.img.we    := false.B
  io.img.din   := 0.U
  io.cache.we  := false.B
  io.cache.din := 0.U
  io.out.bits  := DontCare

  /*
   * Read pixel from image storage
   */
  io.img.en   := io.in.fire
  io.img.addr := io.in.bits.pos

  /*
   * Read error from Error Cache
   */
  io.cache.en   := io.in.fire
  io.cache.addr := io.in.bits.pos

  when(busy) {
    pix := io.img.dout
    err := io.cache.dout

    io.out.bits.pix := pix
    io.out.bits.err := err
    io.out.bits.pos := pos

    // 'pix' and 'err' can be accessed once entering busy state
    // So it will be valid immediately
    resultValid := true.B
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      pos  := io.in.bits.pos
      busy := true.B
    }
  }
}
