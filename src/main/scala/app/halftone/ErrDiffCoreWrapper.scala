package app.halftone

import app.halftone.errdiff.{ErrDiffReg, ImageDumpRead, ImageDumpWrite}
import chisel3._
import chisel3.util.{Enum, is, switch}
import tools.bus.{BramNativeDualPorts, SimpleDataPortR, SimpleDataPortW}
import utils.{EdgeDetector, FPGAModule}

/** Bram Port A: serve for writing <br>
  * Bram Port B: Serve for reading
  */
class ErrDiffCoreWrapper(config: ErrDiffConfig) extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read    = new SimpleDataPortR(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val write   = new SimpleDataPortW(awidth = config.ddrWidth, dwidth = config.ddrWidth)
    val img     = new BramNativeDualPorts(config.bramDataBits, config.bramAddrBits)
    val cache   = new BramNativeDualPorts(config.bramDataBits, config.bramAddrBits)
    val read_r  = Flipped(new SimpleDataPortR(config.ddrWidth, config.ddrWidth))
    val write_r = Flipped(new SimpleDataPortW(config.ddrWidth, config.ddrWidth))
//    val extn_ready = Input(Bool())
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

  // Other components
  val ctrlReg = wrapModuleWithRst(Module(new ErrDiffReg(config.ddrWidth)))

  // Stages connection
  imageDumpR.io.out       <> core.io.in
  core.io.out             <> imageDumpW.io.in
  imageDumpW.io.out.ready := imageDumpR.io.in.ready
  // Other connection
  io.read_r  <> ctrlReg.io.read
  io.write_r <> ctrlReg.io.write
  // unset ports
  core.io.img <> DontCare

  // Registers
  val indicator_r = RegInit(false.B)
  val start_r     = RegInit(false.B)
  val end_r       = RegInit(false.B)
  // Trigger the system
  imageDumpR.io.in.valid := EdgeDetector(ctrlReg.io.reg.active)

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
  val sIdle :: sRead :: sWork :: sWrite :: Nil = Enum(4)
  val state = RegInit(sRead)

  switch(state) {
    is(sIdle) {
      start_r := false.B
      end_r := false.B
      when(imageDumpR.io.in.fire) { state := sRead }
    }
    is(sRead) {
      when(imageDumpR.io.out.fire) { state := sWork }
    }
    is(sWork) {
      io.img <> core.io.img
      when(core.io.out.fire) { state := sWrite }
    }
    is(sWrite) {
      when(imageDumpW.io.out.fire) { state := sRead }
    }
  }

  when(imageDumpR.io.out.fire) { start_r := true.B }
  when(core.io.out.fire) { end_r := true.B }
  when(imageDumpW.io.out.fire) { indicator_r := true.B }
  // State Registers
  ctrlReg.io.reg.start := start_r
  ctrlReg.io.reg.end   := end_r
  ctrlReg.io.reg.done  := indicator_r

  // indicate the finish
  io.indicator := indicator_r
}
