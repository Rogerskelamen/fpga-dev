package component

import chisel3._
import component.Uart._
import utils.{EdgeDetector, FPGAModule}

object Uart {
  val CLK_FREQ = 50000000
  val UART_BAUD = 115200
  final def BPS_CNT = CLK_FREQ/UART_BAUD
}

class Uart {

}

class UartRec extends FPGAModule(true) {
  // use customized clock and rest
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  // IO ports
  val io = FlatIO(new Bundle {
    val uart_rxd = Input(Bool())
    val uart_data = Output(UInt(8.W))
    val uart_done = Output(Bool())
  })

  // Detect falling of uart_rxd signal
  val uart_rxd_r = RegInit(VecInit(Seq.fill(2)(false.B)))
  uart_rxd_r(0) := io.uart_rxd
  uart_rxd_r(1) := uart_rxd_r(0)
  val start_flag = Wire(Bool())
  start_flag := !uart_rxd_r(0) & uart_rxd_r(1)

  val rx_flag = RegInit(false.B) // when to receive
  val rx_cnt = RegInit(0.U(4.W))
  val clk_cnt = RegInit(0.U(9.W))
  val rx_data = RegInit(0.U(8.W)) // cache receiving data
  // when to receive data
  when(start_flag) { rx_flag := true.B }
  .elsewhen(rx_cnt === 9.U && clk_cnt === (BPS_CNT/2).U) {
    rx_flag := false.B
  }

  // Record cycles to count to next bit
  when(rx_flag) {
    clk_cnt := Mux(clk_cnt < (BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise {
    clk_cnt := 0.U
  }

  // Count the number of RX data
  when(rx_flag) {
    when(clk_cnt === (BPS_CNT-1).U) { rx_cnt := rx_cnt + 1.U }
  }.otherwise { rx_cnt := 0.U }

  // Store RX data
  when(rx_flag) {
    when(clk_cnt === (BPS_CNT/2).U && rx_cnt >= 1.U && rx_cnt <= 8.U) {
      rx_data(rx_cnt-1.U) := uart_rxd_r(1)
    }
  }.otherwise( rx_data := 0.U )

  // Output signal and indicate done
  val uart_data_r = RegInit(0.U(8.W))
  val uart_done_r = RegInit(false.B)
  io.uart_data := uart_data_r
  io.uart_done := uart_done_r
  when(rx_cnt === 9.U) {
    uart_data_r := rx_data
    uart_done_r := true.B
  }.otherwise {
    uart_data_r := 0.U
    uart_done_r := false.B
  }
}
