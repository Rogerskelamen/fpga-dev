package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.{is, switch, Counter, Decoupled, MuxLookup}
import tools.bus.BramNativePortFull

class ErrorOut(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ThreshCalc2ErrorOut(config.posWidth, config.errorWidth)))
    val pa  = Flipped(new BramNativePortFull)
    val out = Decoupled(new ErrorOut2WriteBinary(config.posWidth))
  })

  // Registers(for value storage and state presentation)
  val pos         = Reg(UInt(config.posWidth.W))
  val bval        = Reg(Bool())
  val errOut      = Reg(Vec(4, SInt(config.errorWidth.W)))
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy
  // useless signals
  io.pa       := DontCare
  io.pa.en    := false.B
  io.pa.we    := false.B

  /* TODO: check if position is out of boundary */
  val diffRight      = pos + 1.U
  val diffBelowRight = pos + config.imageCol.U + 1.U
  val diffBelow      = pos + config.imageCol.U
  val diffBelowLeft  = pos + config.imageCol.U - 1.U
  val (cnt, cntWrap) = Counter(busy && !resultValid, 7)

  // Emit outputs
  io.out.bits.pos := pos
  io.out.bits.bval := bval

  when(busy) {
    /*
     * Write to Error Cache
     */
    when(!resultValid) {
      // 0. when to enable bram data transfer
      io.pa.en := true.B
      io.pa.addr := MuxLookup(cnt, 0.U)(
        Seq(
          // 1. read from right, below, below left
          0.U -> diffRight,
          1.U -> diffBelow,
          2.U -> diffBelowLeft,
          // 2. write
          3.U -> diffRight,
          4.U -> diffBelowRight,
          5.U -> diffBelow,
          6.U -> diffBelowLeft
        )
      )
      switch(cnt) { // read
        is(1.U) { errOut(0) := errOut(0) + io.pa.dout.asSInt }
        is(2.U) { errOut(2) := errOut(2) + io.pa.dout.asSInt }
        is(3.U) { errOut(3) := errOut(3) + io.pa.dout.asSInt }
      }
      when(cnt > 2.U) { // write
        io.pa.we  := true.B
        io.pa.din := errOut((cnt - 3.U)(1, 0)).asUInt
      }
    }

    when(cntWrap) { resultValid := true.B }
    when(io.out.fire) {
      busy        := false.B
      resultValid := false.B
    }
  }.otherwise {
    when(io.in.valid) {
      val inBundle = io.in.deq()
      pos    := inBundle.pos
      bval   := inBundle.bval
      errOut := inBundle.errOut
      busy   := true.B
    }
  }
}
