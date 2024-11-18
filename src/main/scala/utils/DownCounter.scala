package utils

import chisel3._
import chisel3.util.signedBitLength

/**
 * A better Counter(with less Area)
 */
object DownCounter {
  def apply(cond: Bool, n: Int): Bool = {
    val MAX = (n - 2).S
//    val MAX = (n - 2).S(signedBitLength(n-2).W)
    val gen = RegInit(MAX)
    when(gen.head(1).asBool) {
      gen := MAX
    }.otherwise { gen := gen - 1.S }
    gen.head(1).asBool
  }
}
