package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.Decoupled
import tools.bus.{BramNativePortFull, SimpleDataPortR}

class PixelGet(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val pos = UInt(config.posWidth.W)
    }))
    val read = new SimpleDataPortR(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val pb   = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out  = Decoupled(new PixelGet2ThreshCalc(config.pixelWidth, config.errorWidth, config.posWidth))
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
  io.pb.we    := false.B
  io.pb.din   := 0.U
  io.out.bits := DontCare

  /*
   * Read pixel from ddr
   */
  // begin to read when handshake finishes
  io.read.req.valid := io.in.fire
  io.read.req.addr  := io.in.bits.pos + config.ddrBaseAddr.U
  val offset = pos(1, 0)

  /*
   * Read error from bram
   */
  io.pb.en   := io.in.fire
  io.pb.addr := io.in.bits.pos

  when(busy) {
    when(io.read.resp.valid) {
      pix := io.read.resp.data >> (offset << 3.U)
    }
    err := io.pb.dout

    io.out.bits.pix := pix
    io.out.bits.err := err
    io.out.bits.pos := pos
    // Reading from ddr and bram finishes
    when(io.read.resp.valid) {
      resultValid := true.B
    }

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
