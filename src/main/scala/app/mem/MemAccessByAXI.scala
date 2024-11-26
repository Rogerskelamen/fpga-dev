package app.mem

import chisel3._
import chisel3.util.{Counter, Enum, is, switch}
import tools.SimpleDataPort
import app.mem.MemAccessByAXI.{AWidth, BaseAddr, DATA1, DATA2, DATA3, DWidth}
import utils.FPGAModule

object MemAccessByAXI {
  val AWidth = 32
  val DWidth = 32
  val BaseAddr = "h3800_0000".U
  val DATA1: UInt = "h1234".U
  val DATA2: UInt = "habcd".U
  val DATA3: UInt = "h4321".U
}

class MemAccessByAXI extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read = new SimpleDataPort(AWidth, DWidth)
    val extn_ready = Input(Bool())
    // For debug
    val indicator = Output(Bool())
  })
  // define functions
  def test(): Bool = rd_r(0) === DATA1 && rd_r(1) === DATA2 && rd_r(2) === DATA3

  val triggered = RegInit(false.B)
  val rd_r = RegInit(VecInit(Seq.fill(3)(0.U(DWidth.W))))

  // FSM
  val sIdle :: sRead :: Nil = Enum(2)
  val curr_state = RegInit(sIdle)
  val next_state = WireDefault(sIdle)
  curr_state := next_state
  /** :NOTE:
   * Be careful when using two state signals to create FSM
   * next_state is wire type not reg,
   * so next_state won't stay the same when there is no condition
   */
  val (cnt, cnt_wrap) =
    Counter(curr_state === sRead && next_state === sIdle, 3)

  switch(curr_state) {
    is(sIdle) {
      when(!io.extn_ready && !triggered) { next_state := sRead }
    }
    is(sRead) {
      when(io.read.resp.valid) { next_state := sIdle }
      .otherwise { next_state := sRead }
    }
  }

  io.read.req.valid := curr_state === sIdle && next_state === sRead
  io.read.req.addr := BaseAddr + (cnt << 2)
  when(io.read.resp.valid) {
    rd_r(cnt) := io.read.resp.data
  }

  // triggered
  when(cnt_wrap) {
    triggered := true.B
  }

  io.indicator := test()
}
