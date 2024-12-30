package app.mem

import chisel3._
import chisel3.util.{Counter, Enum, MuxLookup, is, switch}
import app.mem.MemAccessByAXI.{AWidth, BaseAddr, DATA1, DATA2, DATA3, DWidth}
import tools.bus.{SimpleDataPortR, SimpleDataPortW}
import utils.FPGAModule

object MemAccessByAXI {
  val AWidth = 32
  val DWidth = 32
  val BaseAddr = "h3800_0000".U
  val DATA1: UInt = "had".U
  val DATA2: UInt = "hde".U
  val DATA3: UInt = "haa".U
}

/** Address accessed from ddr is aligned with 4 bytes = 1 word
 * This is for axi bus connects to ZYNQ PS
 */
class MemAccessByAXI extends FPGAModule {
  val io = FlatIO(new Bundle {
    val read = new SimpleDataPortR(AWidth, DWidth)
    val write = new SimpleDataPortW(AWidth, DWidth)
    val extn_ready = Input(Bool())
    // For debug
    val indicator = Output(Bool())
  })
  // define functions
  def test(): Bool = rd_r(0) === DATA1 && rd_r(1) === DATA2 && rd_r(2) === DATA3

  val triggered_w = RegInit(false.B)
  val triggered_r = RegInit(false.B)
  val rd_r = RegInit(VecInit(Seq.fill(3)(0.U(DWidth.W))))

  // FSM
  val sIdle :: sRead :: sWrite :: Nil = Enum(3)
  val curr_state_w = RegInit(sIdle)
  val next_state_w = WireDefault(sIdle)
  curr_state_w := next_state_w
  val (w_cnt, w_cnt_wrap) =
    Counter(curr_state_w === sWrite && next_state_w === sIdle, 3)

  /** :NOTE:
   * Be careful when using two state signals to create FSM
   * notice that next_state is wire type not reg,
   * so next_state won't stay the same when there is no condition
   */
  switch(curr_state_w) {
    is(sIdle) {
      when(!io.extn_ready && !triggered_w) { next_state_w := sWrite }
    }
    is(sWrite) {
      when(io.write.resp.valid) { next_state_w := sIdle }
      .otherwise { next_state_w := sWrite }
    }
  }

  io.write.req.valid := curr_state_w === sIdle && next_state_w === sWrite
//  io.write.req.strb := ~0.U((DWidth/8).W)
  io.write.req.strb := 1.U << w_cnt
  io.write.req.addr := BaseAddr + w_cnt
  io.write.req.data := MuxLookup(w_cnt, 0.U)(
    Seq(
      0.U -> DATA1,
      1.U -> (DATA2 << 8.U).asUInt,
      2.U -> (DATA3 << 16.U).asUInt,
    )
  )

  // read
  val curr_state_r = RegInit(sIdle)
  val next_state_r = WireDefault(sIdle)
  curr_state_r := next_state_r
  val (r_cnt, r_cnt_wrap) =
    Counter(curr_state_r === sRead && next_state_r === sIdle, 3)

  switch(curr_state_r) {
    is(sIdle) {
      when(triggered_r) { next_state_r := sRead }
    }
    is(sRead) {
      when(io.read.resp.valid) { next_state_r := sIdle }
      .otherwise { next_state_r := sRead }
    }
  }

  io.read.req.valid := curr_state_r === sIdle && next_state_r === sRead
  io.read.req.addr := BaseAddr + r_cnt
  when(io.read.resp.valid) {
    rd_r(r_cnt) := io.read.resp.data
  }

  // triggered
  when(w_cnt_wrap) {
    triggered_w := true.B
    triggered_r := true.B
  }
  when(r_cnt_wrap) {
    triggered_r := false.B
  }

  io.indicator := rd_r(2)(23, 0) === "haadead".U
}
