package app.halftone.errdiff

import app.halftone.ErrDiffConfig
import chisel3._
import chisel3.util.{Counter, Decoupled, MuxLookup}
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
  io.out.bits := DontCare
  io.pa       := DontCare
  io.pa.en    := false.B
  io.pa.we    := false.B

  val diffRight      = pos + 1.U
  val diffBelowRight = pos + config.imageCol.U + 1.U
  val diffBelow      = pos + config.imageCol.U
  val diffBelowLeft  = pos + config.imageCol.U - 1.U
  val (cnt, cntWrap) = Counter(busy && !resultValid, 4)

  // Emit outputs
  io.out.bits.pos := pos

  when(busy) {
    /*
     * Write to Error Cache
     */
    // when to write bram
    when(!resultValid) {
      io.pa.en := true.B
      io.pa.we := true.B
    }
    io.pa.addr := MuxLookup(cnt, 0.U)(
      Seq(
        0.U -> diffRight,
        1.U -> diffBelowRight,
        2.U -> diffBelow,
        3.U -> diffBelowLeft
      )
    )
    io.pa.din := errOut(cnt).asUInt

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
