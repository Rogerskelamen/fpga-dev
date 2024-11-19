package utils

import chisel3._

/**
 * A better Counter(with less Area)
 * Activate a signal for every n cycles
 */
object DownCounter {
  def apply(cond: Bool, n: Int): Bool = {
    require(n >= 2, "n needs to be greater than 2")
    val MAX = (n - 2).S
    val gen = RegInit(MAX)
    when(gen.head(1).asBool) {
      gen := MAX
    }.otherwise { gen := gen - 1.S }
    gen.head(1).asBool
  }
}
