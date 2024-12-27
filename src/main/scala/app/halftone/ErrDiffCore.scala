package app.halftone

import app.halftone.errdiff.PixelGet
import chisel3._
import chisel3.util.Counter
import tools.bus.{BramNativePortFull, SimpleDataPortR}
import utils.FPGAModule

case class ErrDiffConfig(
  override val pixelWidth: Int = 8,
  // override val ddrBaseAddr: Int = ???,
  override val ddrBaseAddr: Int = 0,
  override val imageRow:    Int = 512,
  override val imageCol:    Int = 512,
  errorWidth:               Int = 8,
  threshold: Int = 128
) extends HalftoneConfig(pixelWidth, ddrBaseAddr, imageRow, imageCol)

class ErrDiffCore(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read       = new SimpleDataPortR(32, dwidth = config.pixelWidth)
    val pb         = Flipped(new BramNativePortFull)
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

  // 7 cycles
  // output to err cache

  // 3 cycles at least
  // write binary value(ddr)

  /*
   * Logics
   */
  // Registers
  val triggered = RegInit(false.B)
  when(!io.extn_ready) { triggered := true.B }

  val (pos, posWrap) = Counter(pixelGet.io.out.fire, config.imageSiz - 1)

  val pipExe = RegInit(false.B)
  pipExe := (pixelGet.io.out.fire || (!io.extn_ready && !triggered)) && !posWrap

  pixelGet.io.in.valid    := pipExe
  pixelGet.io.in.bits.pos := pos

  pixelGet.io.pb   <> io.pb
  pixelGet.io.read <> io.read
  pixelGet.io.out  <> DontCare
}
