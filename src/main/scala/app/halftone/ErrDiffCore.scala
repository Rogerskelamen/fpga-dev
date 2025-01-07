package app.halftone

import app.halftone.errdiff.{ErrorOut, PixelGet, ThreshCalc, WriteBinary}
import chisel3._
import chisel3.util.{Counter, Decoupled}
import tools.bus.BramNativePortFull
import utils.EdgeDetector

case class ErrDiffConfig(
  override val pixelWidth: Int = 8,
  // override val ddrBaseAddr: Int = ???,
  override val ddrBaseAddr: Int = 0x3800_0000,
  override val ddrWidth:    Int = 32,
  override val imageRow:    Int = 512,
  override val imageCol:    Int = 512,
  errorWidth:               Int = 8,
  threshold:                Int = 128,
  bramDataBits:             Int = 8,
  bramAddrBits:             Int = 18)
extends HalftoneConfig(pixelWidth, ddrBaseAddr, ddrWidth, imageRow, imageCol)

/** Bram Port A: serve for writing
  * Bram Port B: Serve for reading
  */
class BramNativePorts(val bramDataBits: Int, val bramAddrBits: Int) extends Bundle {
  val pb = Flipped(new BramNativePortFull(bramDataBits, bramAddrBits))
  val pa = Flipped(new BramNativePortFull(bramDataBits, bramAddrBits))
}

class ErrDiffCore(config: ErrDiffConfig) extends Module {
  val io = FlatIO(new Bundle {
    val in    = Flipped(Decoupled())
    val img   = new BramNativePorts(config.bramDataBits, config.bramAddrBits)
    val cache = new BramNativePorts(config.bramDataBits, config.bramAddrBits)
    val out   = Decoupled()
  })

  /*
   * All Four Stages
   * multi-cycle no-pipeline first
   */
  // 1 cycle
  // get pixel(bram) and err(bram)
  val pixelGet = Module(new PixelGet(config))

  // 1 cycle
  // calculate, get binary value and four error values(LUT)
  val threshCalc = Module(new ThreshCalc(config))

  // 7 cycles
  // output to err cache
  val errorOut = Module(new ErrorOut(config))

  // 1 cycle
  // write binary value(bram)
  val writeBinary = Module(new WriteBinary(config))

  /*
   * Logics
   */
  // Stages connection
  threshCalc.io.in  <> pixelGet.io.out
  errorOut.io.in    <> threshCalc.io.out
  writeBinary.io.in <> errorOut.io.out

  // useless signals
  writeBinary.io.out.ready := pixelGet.io.in.ready

  // IO ports
  io.img.pb   <> pixelGet.io.img // read pixel
  io.img.pa   <> writeBinary.io.img // write pixel
  io.cache.pb <> pixelGet.io.cache // get error
  io.cache.pa <> errorOut.io.pa // write error

  // Registers
  val triggered = RegInit(false.B)
  when(io.in.fire) { triggered := true.B }
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy

  // pixel position counter
  val (pos, posWrap) = Counter(writeBinary.io.out.fire, config.imageSiz)
  val next_pix_flag  = EdgeDetector(writeBinary.io.out.fire)
  // Execution trigger
  val pipeExe = next_pix_flag || io.in.fire
  pixelGet.io.in.valid    := pipeExe && !resultValid
  pixelGet.io.in.bits.pos := pos

  when(busy) {
    when(posWrap && writeBinary.io.out.fire) {
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
