package app.halftone.errdiff

import chisel3._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chisel3.util.MuxLookup

/** Pure Combinatorial Logic Circuit
  * @param errorWidth Error bit width
  */
class ELUT(val errorWidth: Int) extends RawModule {
  val io = IO(new Bundle {
    val err = Input(SInt(errorWidth.W))
    val out = Output(Vec(4, SInt(errorWidth.W)))
  })

  /** @note
    * The 'round' function in scala is "Round Half Up"(similar to python)
    * It's not like the C99 'round' function which obeys the rule
    * "Round Half Away from Zero"
    */
  private def errorVecLit(err: Int): Vec[SInt] = {
    val errF = err.toFloat
    Vec(4, SInt(errorWidth.W)).Lit(
      0 -> scala.math.round(errF * 7/16).S,
      1 -> scala.math.round(errF * 1/16).S,
      2 -> scala.math.round(errF * 5/16).S,
      3 -> scala.math.round(errF * 3/16).S
    )
  }

  /** Chisel only fills bits width with
    * the minimal satisfaction for presentation of literal variables
    * It won't expand bits width anyway, e.g.
    * -64 will be presented as `b100_0000` rather than `b1100_0000`
    */
  val errLookupSeq = for (err <- -127 to 127) yield {
    err.S(errorWidth.W).asUInt -> errorVecLit(err)
  }

  io.out := MuxLookup(io.err.asUInt, errorVecLit(0))(errLookupSeq)
}
