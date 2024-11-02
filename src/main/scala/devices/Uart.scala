package devices

import chisel3._
import chisel3.util.MuxLookup
import devices.Uart._
import utils.FPGAModule

object Uart {
  val CLK_FREQ = 50000000
  val UART_BAUD = 115200
  final def BPS_CNT = CLK_FREQ/UART_BAUD
}

class Uart {

}

class UartRecv extends FPGAModule(true) {
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
  // rx_flag indicates the time of receiving data
  when(start_flag) { rx_flag := true.B }
  .elsewhen(rx_cnt === 9.U && clk_cnt === (BPS_CNT/2).U) {
    rx_flag := false.B
  }

  // clk_cnt records cycles to count to next bit
  when(rx_flag) {
    clk_cnt := Mux(clk_cnt < (BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise {
    clk_cnt := 0.U
  }

  // rx_cnt counts the bit number of RX data
  when(rx_flag) {
    when(clk_cnt === (BPS_CNT-1).U) { rx_cnt := rx_cnt + 1.U }
  }.otherwise { rx_cnt := 0.U }

  // Store RX data
  when(rx_flag) {
    // Only store current bit from middle of count cycle
    when(clk_cnt === (BPS_CNT/2).U && rx_cnt >= 1.U && rx_cnt <= 8.U) {
      rx_data := rx_data | (uart_rxd_r(1) << (rx_cnt-1.U)).asUInt
    }
  }.otherwise{ rx_data := 0.U }

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

class UartTran extends FPGAModule(true) {
  // use customized clock and rest
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  val io = FlatIO(new Bundle {
    val uart_en = Input(Bool())
    val uart_din = Input(UInt(8.W))
    val uart_txd = Output(Bool())
    val uart_tx_busy = Output(Bool())
  })

  // Detect raising of uart_rxd signal
  val uart_txd_r = RegInit(VecInit(Seq.fill(2)(false.B)))
  uart_txd_r(0) := io.uart_en
  uart_txd_r(1) := uart_txd_r(0)
  val en_flag = Wire(Bool())
  en_flag := uart_txd_r(0) & !uart_txd_r(1)

  val tx_flag = RegInit(false.B)
  val tx_cnt = RegInit(0.U(4.W)) // maximum to 9
  val clk_cnt = RegInit(0.U(9.W)) // depend on baud rate
  val tx_data = RegInit(UInt(8.W)) // cache transmit data

  // rx_flag indicates the time of transmitting data
  when(en_flag) {
    tx_flag := true.B
    tx_data := io.uart_din
  }
  .elsewhen(tx_cnt === 9.U && clk_cnt === (BPS_CNT/2).U) {
    tx_flag := false.B
    tx_data := 0.U
  }

  // Record cycles to count to next bit
  when(tx_flag) {
    clk_cnt := Mux(clk_cnt < (BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise {
    clk_cnt := 0.U
  }

  // Count the bit number of TX data
  when(tx_flag) {
    when(clk_cnt === (BPS_CNT-1).U) { tx_cnt := tx_cnt + 1.U }
  }.otherwise { tx_cnt := 0.U }

  // Store RX data
  when(tx_flag) {
    // Only store current bit from middle of count cycle
    when(clk_cnt === (BPS_CNT/2).U) {
      when(tx_cnt === 0.U) {
        io.uart_txd := 0.U // start bit
      }.elsewhen(tx_cnt <= 8.U) {
        io.uart_txd := tx_data(tx_cnt-1.U) // data
      }.otherwise {
        io.uart_txd := 1.U // 1 stop bit
      }
    }
  }.otherwise{ io.uart_txd := 1.U }

  io.uart_tx_busy := tx_flag
}