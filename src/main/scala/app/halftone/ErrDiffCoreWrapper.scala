package app.halftone

import app.halftone.errdiff.{ImageDumpRead, ImageDumpWrite}
import chisel3._
import chisel3.util.{is, switch, Enum}
import tools.bus.{SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

/** Bram Port A: serve for writing <br>
  * Bram Port B: Serve for reading
  */
class ErrDiffCoreWrapper(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read       = new SimpleDataPortR(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val write      = new SimpleDataPortW(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val img        = new BramNativePorts(config.bramDataBits, config.bramAddrBits)
    val cache      = new BramNativePorts(config.bramDataBits, config.bramAddrBits)
    val extn_ready = Input(Bool())
    val indicator  = Output(Bool())
  })

  /*
   * Three Stages
   */
  // 1. transfer image from ddr to bram
  val imageDumpR = wrapModuleWithRst(Module(new ImageDumpRead(config)))

  // 2. process image using a logical core
  val core = wrapModuleWithRst(Module(new ErrDiffCore(config)))

  // 3. dump image from bram to ddr
  val imageDumpW = wrapModuleWithRst(Module(new ImageDumpWrite(config)))

  // Stages connection
  imageDumpR.io.out       <> core.io.in
  core.io.out             <> imageDumpW.io.in
  imageDumpW.io.out.ready := imageDumpR.io.in.ready
  // unset ports
  core.io.img <> DontCare

  // Registers
  val triggered   = RegInit(false.B)
  val indicator_r = RegInit(false.B)
  when(!io.extn_ready) { triggered := true.B }
  // Trigger the system
  imageDumpR.io.in.valid := !io.extn_ready && !triggered

  /*
   * External Ports
   */
  // ddr through AXI4Lite
  io.read  <> imageDumpR.io.read
  io.write <> imageDumpW.io.write
  // two brams(image storage and error cache)
  io.img.pa <> imageDumpR.io.pa
  io.img.pb <> imageDumpW.io.pb
  io.cache  <> core.io.cache

  /*
   * FSM
   */
  val sRead :: sWork :: sWrite :: Nil = Enum(3)
  val state = RegInit(sRead)

  switch(state) {
    is(sRead) {
      when(imageDumpR.io.out.fire) { state := sWork }
    }
    is(sWork) {
      io.img <> core.io.img
      when(core.io.out.fire) { state := sWrite }
    }
    // No need to turn around to `sRead`
  }

  when(imageDumpW.io.out.fire) {
    indicator_r := true.B
  }
  io.indicator := indicator_r
}
