package app.halftone

import app.halftone.errdiff.PixelGet
import chisel3._
import chisel3.util.Counter
import tools.bus.{BramNativePortFull, SimpleDataPortR}
import utils.FPGAModule

case class ErrDiffConfig(
  errorWidth: Int = 8 // maybe 7 is enough?
) extends HalftoneConfig

class ErrDiffCore(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read       = new SimpleDataPortR(32, dwidth = config.pixelWidth)
    val pb         = Flipped(new BramNativePortFull)
    val extn_ready = Input(Bool())
  })

  final def wrapModuleWithRst[T <: RawModule](subModule: T): T = {
    withClockAndReset(fpga_clk, (~fpga_rst).asBool) { subModule }
  }

  /** All Four Stages
    */
  // get pixel(ddr) and err(bram)
  val pixelGet = wrapModuleWithRst(Module(new PixelGet(config)))

  // calculate, store binary value and get four error values(LUT)

  // output to err cache

  // write binary value(ddr)

  /** Logics
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
