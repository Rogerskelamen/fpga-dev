package app.bram

import app.bram.BramAccess.{DATA1, DATA2, DATA3, DATA4, DWidth}
import chisel3._
import chisel3.util.{MuxLookup, unsignedBitLength}
import tools.bus.BramNativePortFull
import utils.FPGAModule

object BramAccess {
  val DWidth = 8
  val DATA1 = "haa".U(8.W)
  val DATA2 = "hbb".U(8.W)
  val DATA3 = "hcc".U(8.W)
  val DATA4 = "hdd".U(8.W)
}

class BramAccess extends FPGAModule {
  val io = FlatIO(new Bundle {
    val pa = Flipped(new BramNativePortFull) // to write
    val pb = Flipped(new BramNativePortFull) // to read
    // For debug
//    val indicator = Output(Bool())
  })
  // define functions
  def test(): Bool = rd_r(0) === DATA1 && rd_r(1) === DATA2 && rd_r(2) === DATA3 && rd_r(3) === DATA4

  val rd_r = RegInit(VecInit(Seq.fill(4)(0.U(DWidth.W))))

  // ensure en and we are invalid for common case
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
  val w_cnt = RegInit(0.U(unsignedBitLength(4).W))
  when(w_cnt < 4.U) {
    w_cnt := w_cnt + 1.U
    io.pa.en := true.B
    io.pa.we := true.B
    io.pa.addr := w_cnt
    io.pa.din := MuxLookup(w_cnt, 0.U)(
      Seq(
        0.U -> DATA1,
        1.U -> DATA2,
        2.U -> DATA3,
        3.U -> DATA4,
      )
    )
  }

  /*
   * Read Transaction
   */
  /*
                ____    ____    ____    ____    ____
     clock   __|   |___|   |___|   |___|   |___|   |___
                ________________________
     enb     __|                       |_______________
                ________________________
     addrb   __| 00000 | 00001 | 00002 |_______________
                        _______________________________
     doutb   XXXXXXXXXX|  aa   |  bb   |  cc
   */
  val r_cnt = RegInit(0.U(unsignedBitLength(5).W))
  when(w_cnt >= 2.U && r_cnt < 5.U) {
    r_cnt := r_cnt + 1.U
    io.pb.en := true.B && (r_cnt < 4.U)
    io.pb.addr := r_cnt
    when(r_cnt > 0.U) {
      rd_r((r_cnt-1.U)(1, 0)) := io.pb.dout
    }
  }

//  io.indicator := test()
}
