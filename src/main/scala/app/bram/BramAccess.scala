package app.bram

import app.bram.BramAccess.{DATA1, DATA2, DATA3, DWidth}
import chisel3._
import chisel3.util.{Counter, Enum, MuxLookup, is, switch, unsignedBitLength}
import tools.bus.BramNativePortFull
import utils.FPGAModule

object BramAccess {
  val DWidth = 8
  val DATA1 = "haa".U(8.W)
  val DATA2 = "hbb".U(8.W)
  val DATA3 = "hff".U(8.W)
}

class BramAccess extends FPGAModule {
  val io = FlatIO(new Bundle {
    val pa = Flipped(new BramNativePortFull) // to write
    val pb = Flipped(new BramNativePortFull) // to read
    // For debug
    val indicator = Output(Bool())
  })
  // define functions
  def test(): Bool = rd_r(0) === DATA1 && rd_r(1) === DATA2 && rd_r(2) === DATA3

  val w_cnt = RegInit(0.U(unsignedBitLength(2).W))
  val w_cnt_wrap = w_cnt === 3.U
  val rd_r = RegInit(VecInit(Seq.fill(3)(0.U(DWidth.W))))

  // ensure en and we are invalid for common condition
  io.pa <> DontCare
  io.pb <> DontCare
  io.pa.en := false.B
  io.pa.we := false.B
  io.pb.en := false.B
  io.pb.we := false.B

  /*
   * Write Transaction
   */
  // Straightly enable bram native ports when write request comes
  // No need to make FSM
  // This system builds on the condition that
  // All write correctness is guaranteed
  when(!w_cnt_wrap) {
    w_cnt := w_cnt + 1.U
    io.pa.en := true.B
    io.pa.we := true.B
    io.pa.addr := w_cnt
    io.pa.din := MuxLookup(w_cnt, 0.U)(
      Seq(
        0.U -> DATA1,
        1.U -> DATA2,
        2.U -> DATA3,
      )
    )
  }

  /*
   * Read Transaction
   */
  val sIdle :: sRead :: Nil = Enum(2)
  val curr_state_r = RegInit(sIdle)
  val next_state_r = WireDefault(sIdle)
  curr_state_r := next_state_r

  // counter
  val r_cnt = RegInit(0.U(unsignedBitLength(2).W))
  when(curr_state_r === sRead && next_state_r === sIdle && r_cnt < 3.U) {
    r_cnt := r_cnt + 1.U
  }

  switch(curr_state_r) {
    is(sIdle) {
      when(w_cnt >= 2.U && r_cnt < 3.U) { next_state_r := sRead }
    }
    is(sRead) {
      next_state_r := sIdle
    }
  }
  when(curr_state_r === sIdle && next_state_r === sRead) {
    io.pb.en := true.B
    io.pb.addr := r_cnt
  }
  when(curr_state_r === sRead) {
    rd_r(r_cnt) := io.pb.dout
  }

  val indicator_r = RegInit(false.B)
  when(w_cnt > 1.U) {
    indicator_r := true.B
  }

  io.indicator := test()
}
