package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.{is, switch, Counter, Decoupled, MuxLookup}
import tools.bus.BramNativePortFull

class ErrorOut(config: ErrDiffConfig) extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ThreshCalc2ErrorOut(config.posWidth, config.errorWidth)))
    val pa  = Flipped(new BramNativePortFull(config.bramDataBits, config.bramAddrBits))
    val out = Decoupled(new ErrorOut2WriteBinary(config.posWidth))
  })

  // Check Boundary
  /** @note
    * the '%' operator may take a lot to synthesis
    * But for now, config.imageCol is a power of 2,
    * so it will be synthesised as a simple bitwise operation
    */
  def isFirstColumn(pos: UInt): Bool =
    pos % config.imageCol.U === 0.U
  def isLastColumn(pos: UInt): Bool =
    pos % config.imageCol.U === (config.imageCol - 1).U
  def isLastRow(pos: UInt): Bool =
    pos >= (config.imageSiz - config.imageCol).U

  // Registers(for value storage and state presentation)
  val pos         = Reg(UInt(config.posWidth.W))
  val bval        = Reg(Bool())
  val errOut      = Reg(Vec(4, SInt(config.errorWidth.W)))
  val busy        = RegInit(false.B)
  val resultValid = RegInit(false.B)

  io.out.valid := resultValid
  io.in.ready  := !busy
  // useless signals
  io.pa    := DontCare
  io.pa.en := false.B
  io.pa.we := false.B
  io.pa.din := 0.U

  val diffRight      = pos + 1.U
  val diffBelowRight = pos + (config.imageCol + 1).U
  val diffBelow      = pos + config.imageCol.U
  val diffBelowLeft  = pos + (config.imageCol - 1).U
  val (cnt, cntWrap) = Counter(busy && !resultValid, 7)

  // Emit outputs
  io.out.bits.pos  := pos
  io.out.bits.bval := bval

  when(busy) {
    /*
     * Write to Error Cache
     */
    when(!resultValid) {
      // 0. when to enable bram data transfer(must within the boundary)
      io.pa.en := MuxLookup(cnt, false.B)(
        Seq(
          0.U -> !isLastColumn(pos),
          1.U -> !isLastRow(pos),
          2.U -> !(isFirstColumn(pos) || isLastRow(pos)),
          3.U -> !isLastColumn(pos),
          4.U -> !(isLastColumn(pos) || isLastRow(pos)),
          5.U -> !isLastRow(pos),
          6.U -> !(isFirstColumn(pos) || isLastRow(pos))
        )
      )

      io.pa.addr := MuxLookup(cnt, 0.U)(
        Seq(
          // 1. read from right, below, below left
          0.U -> diffRight,
          1.U -> diffBelow,
          2.U -> diffBelowLeft,
          // 2. write to 4 diff area
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
        io.pa.we  := io.pa.en
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
