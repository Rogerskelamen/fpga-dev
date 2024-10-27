import chisel3._
import MemAccess._
import chisel3.util.{Enum, is, switch}
import utils.FPGAModule

object MemAccess {
  val DWIDTH: Int = 32
  val AWIDTH: Int = 32

  val DATA1: UInt = "h1234".U
  val DATA2: UInt = "habcd".U
  val DATA3: UInt = "h4321".U
}

// MemAccess common Bundle
class MemAccessB(val dwidth: Int, val awidth: Int, val isR: Boolean)
  extends Bundle {
  val addr = Output(UInt(awidth.W))
  val data = if (isR)
    Input(UInt(dwidth.W)) else
    Output(UInt(dwidth.W))
  val en   = Output(Bool())
  val done = Input(Bool())
}

trait MemAcessHasValid { val data_valid = Input(Bool()) }
// external input indicating read ready, connect to IO device
trait MemAcessHasEReady { val extn_ready = Input(Bool()) }

class MemAccessRB(override val dwidth: Int, override val awidth: Int)
  extends MemAccessB(dwidth, awidth, true) with MemAcessHasValid with MemAcessHasEReady

class MemAccessWB(override val dwidth: Int, override val awidth: Int)
  extends MemAccessB(dwidth, awidth, false) with MemAcessHasEReady

// Read/Write data from DDR
class MemAccess extends FPGAModule(rstN = true) {
  // Use customized clock and reset
  override protected def clkFPGAName: String = "axi_clk"
  override protected def rstFPGAName: String = "axi_rstn"

  // IO ports
  val io = FlatIO(new Bundle {
    val read = new MemAccessRB(DWIDTH, AWIDTH)
    val write = new MemAccessWB(DWIDTH, AWIDTH)
    val axi_txn = Output(Bool()) // AXI bus reset
//    val out_data = Output(UInt(DWIDTH.W)) // output to another Module(debug purpose)

    // For debug
    val indicator = Output(Bool())
  })

  // Deal with read_done signal
  val read_done_r = RegInit(VecInit(Seq.fill(2)(false.B)))
  read_done_r(0) := io.read.done
  read_done_r(1) := read_done_r(0)
  val read_done_raise = Wire(Bool())
  read_done_raise := read_done_r(0) && !read_done_r(1)

  // Define internal registers to store data
  val rd_r = RegInit(VecInit(Seq.fill(3)(0.U(DWIDTH.W))))
  val counter = RegInit(0.U(2.W))
  val triggered = RegInit(false.B)

  // define functions
  def test(): Bool = rd_r(0) === DATA1 && rd_r(1) === DATA2 && rd_r(2) === DATA3

  /**
   * FSM
   */
  // Define states
  val sAXI_IDLE :: sAXI_READ :: sAXI_WRITE :: Nil = Enum(3)
  val sAXI_FREE :: sAXI_TXN :: sAXI_EN :: Nil = Enum(3)

  // Define current state and next state
  val axi_curr_state = RegInit(sAXI_IDLE)
  val axi_next_state = WireDefault(sAXI_IDLE)
  val axi_te_curr_state = RegInit(sAXI_FREE)
  val axi_te_next_state = WireDefault(sAXI_FREE)

  // Three segments programming for FSM
  // 1. state transfer
  axi_curr_state := axi_next_state
  axi_te_curr_state := axi_te_next_state
  // 2. next state change in combinatorial logic circuit
  switch(axi_curr_state) {
    is(sAXI_IDLE) {
      when(!io.read.extn_ready && !triggered) { axi_next_state := sAXI_READ }
      .elsewhen(io.write.extn_ready) { axi_next_state := sAXI_WRITE }
    }
    is(sAXI_READ) {
      when(counter >= 2.U && read_done_raise) { axi_next_state := sAXI_IDLE }
      // Line below is necessary cause axi_next_state is sAXI_IDLE by default
      .otherwise { axi_next_state := sAXI_READ }
    }
    // is(sAXI_WRITE) {
    //   when(/* jump to read */) { axi_next_state := sAXI_READ }
    //   .elsewhen(/* finish write */) { axi_next_state := sAXI_IDLE }
    // }
  }
  switch(axi_te_curr_state) {
    is(sAXI_FREE) {
      when(axi_curr_state === sAXI_IDLE && axi_next_state === sAXI_READ) {
        axi_te_next_state := sAXI_TXN
      }
      // Another read continues
      .elsewhen(axi_curr_state === sAXI_READ && axi_next_state === sAXI_READ && read_done_raise) {
        axi_te_next_state := sAXI_TXN
      }
    }
    is(sAXI_TXN) { axi_te_next_state := sAXI_EN }
    is(sAXI_EN) { axi_te_next_state := sAXI_FREE }
  }
  // 3. core logic in FSM(Sequential logic circuit)

  /**
   * Signal Handle
   */
  // io.axi_txn
  val axi_txn_r = RegInit(false.B)
  when(axi_te_curr_state === sAXI_TXN) { axi_txn_r := true.B }
  .otherwise { axi_txn_r := false.B }
  io.axi_txn := axi_txn_r

  // io.read.en
  val read_en_r = RegInit(false.B)
  when(axi_curr_state === sAXI_READ && axi_te_curr_state === sAXI_EN) { read_en_r := true.B }
  .otherwise { read_en_r := false.B }
  io.read.en := read_en_r

  // io.read.addr
  // read_addr just an offset address of ddr(the base is 0x38000000)
  val read_addr_r = RegInit(0.U(AWIDTH.W))
  when(axi_curr_state === sAXI_IDLE) { read_addr_r := 0.U }
  .elsewhen(axi_curr_state === sAXI_READ && read_done_raise) { read_addr_r := read_addr_r + 4.U } // continue to read
  io.read.addr := read_addr_r

  // io.out_data
//  val out_data_r = RegInit(0.U(DWIDTH.W))
//  when(io.read.data_valid) { out_data_r := io.read.data }
//  io.out_data := out_data_r

  // rd_r
  when(io.read.data_valid) {
    rd_r(counter) := io.read.data
  }

  // counter
  when(axi_curr_state === sAXI_READ && read_done_raise) {
    counter := counter + 1.U
  }
  when(axi_curr_state === sAXI_READ && axi_next_state === sAXI_IDLE) {
    counter := 0.U
  }

  // triggered
  when(axi_curr_state === sAXI_READ) {
    triggered := true.B
  }

  // indicator
  io.indicator := test()

  io.write := DontCare // Don't care about write signals
}
