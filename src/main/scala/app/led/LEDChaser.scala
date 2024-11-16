package app.led

import chisel3._
import chisel3.util.Counter
import utils.FPGAModule

object LEDChaser {
  val FREQ = 100000000
}

class LEDChaser extends FPGAModule {
  val io = FlatIO(new Bundle {
    val led_out = Output(Vec(4, Bool()))
  })

  import LEDChaser._
  val (cnt, cnt_wrap) = Counter(true.B, FREQ/2)
  val (led_cnt, _) = Counter(cnt_wrap, 4)

  for (i <- 0 to 3) {
    io.led_out(i) := led_cnt === i.U
  }
}
