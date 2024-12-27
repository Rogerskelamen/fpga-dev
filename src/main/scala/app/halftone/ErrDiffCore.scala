package app.halftone

import app.halftone.errdiff.{ErrorOut, PixelGet, ThreshCalc, WriteBinary}
import chisel3._
import chisel3.util.Counter
import tools.bus.{BramNativePortFull, SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

case class ErrDiffConfig(
  override val pixelWidth: Int = 8,
  // override val ddrBaseAddr: Int = ???,
  override val ddrBaseAddr: Int = 0,
  override val imageRow:    Int = 512,
  override val imageCol:    Int = 512,
  errorWidth:               Int = 8,
  threshold:                Int = 128)
extends HalftoneConfig(pixelWidth, ddrBaseAddr, imageRow, imageCol)

class ErrDiffCore(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read       = new SimpleDataPortR(32, dwidth = config.pixelWidth)
    val write      = new SimpleDataPortW(32, dwidth = config.pixelWidth)
    val pb         = Flipped(new BramNativePortFull)
    val pa         = Flipped(new BramNativePortFull)
    val extn_ready = Input(Bool())
  })

  final def wrapModuleWithRst[T <: RawModule](subModule: T): T = {
    withClockAndReset(fpga_clk, (~fpga_rst).asBool) { subModule }
  }

  /*
   * All Four Stages
   * multi-cycle none pipeline first
   */
  // 3 cycles at least
  // get pixel(ddr) and err(bram)
  val pixelGet = wrapModuleWithRst(Module(new PixelGet(config)))

  // 1 cycle
  // calculate, get binary value and four error values(LUT)
  val threshCalc = wrapModuleWithRst(Module(new ThreshCalc(config)))

  // 7 cycles
  // output to err cache
  val errorOut = wrapModuleWithRst(Module(new ErrorOut(config)))

  // 3 cycles at least
  // write binary value(ddr)
  val writeBinary = wrapModuleWithRst(Module(new WriteBinary(config)))

  /*
   * Logics
   */
  // Stages connection
  threshCalc.io.in  <> pixelGet.io.out
  errorOut.io.in    <> threshCalc.io.out
  writeBinary.io.in <> errorOut.io.out

  // useless signals
  writeBinary.io.out.ready := pixelGet.io.in.ready

  // Registers
  val triggered = RegInit(false.B)
  when(!io.extn_ready) { triggered := true.B }

  // pixel position counter
  val (pos, posWrap) = Counter(writeBinary.io.out.fire, config.imageSiz - 1)

  // Execution trigger
  val pipeExe = (writeBinary.io.out.fire || (!io.extn_ready && !triggered)) && !posWrap
  pixelGet.io.in.valid    := pipeExe
  pixelGet.io.in.bits.pos := pos

  io.pb    <> pixelGet.io.pb
  io.read  <> pixelGet.io.read
  io.pa    <> errorOut.io.pa
  io.write <> writeBinary.io.write
}
