package devices

import chisel3._
import chisel3.util.log2Up
import devices.Uart._
import utils.{EdgeDetector, FPGAModule}

/**
 * Attributes:
 * - No-parity
 * - 1 bit stop
 */
object Uart {
  val UART_BITW = 8
}

case class UartConfig(
  clkFreq: Int = 100000000,
  baudRate: Int = 115200,
  oddParity: Boolean = false,
  evenParity: Boolean = false,
  stopWidth: Int = 1) {
  final def BPS_CNT: Int = clkFreq/baudRate
  final def CNT_WID: Int = log2Up(BPS_CNT)
  final def hasParity: Boolean = oddParity | evenParity
  final def BIT_LEN: Int =
    if (hasParity)
      9 + stopWidth
    else
      8 + stopWidth
}

/**
 * Receive 1 bit serial signal and
 * Assemble into an 8 bits data to output
 *
 * IO ports:
 *
 * input uart_rxd
 * output done
 * output [7:0] uart_data
 */
class UartRecv(val conf: UartConfig = UartConfig())
  extends FPGAModule {
  // check violation
  require(!(conf.oddParity & conf.evenParity), "Only one parity type is allowed")

  // use customized clock and rest
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  // IO ports
  val io = FlatIO(new Bundle {
    val uart_rxd = Input(Bool())
    val uart_data = Output(UInt(UART_BITW.W))
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
  val clk_cnt = RegInit(0.U(conf.CNT_WID.W))
  val rx_data = RegInit(0.U(UART_BITW.W)) // cache receiving data

  // rx_flag indicates the time of receiving data
  when(start_flag) { rx_flag := true.B }
  .elsewhen(rx_cnt === conf.BIT_LEN.U && clk_cnt === (conf.BPS_CNT/2).U) {
    rx_flag := false.B
  }

  // clk_cnt records cycles to count to next bit
  when(rx_flag) {
    clk_cnt := Mux(clk_cnt < (conf.BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise { clk_cnt := 0.U }

  // rx_cnt counts the bit number of RX data
  when(rx_flag) {
    when(clk_cnt === (conf.BPS_CNT-1).U) { rx_cnt := rx_cnt + 1.U }
  }.otherwise { rx_cnt := 0.U }

  // Store RX data
  when(rx_flag) {
    // Only store current bit from middle of count cycle
    when(clk_cnt === (conf.BPS_CNT/2).U) {
      when(rx_cnt >= 1.U && rx_cnt <= 8.U) {
        rx_data := rx_data | (uart_rxd_r(1) << (rx_cnt - 1.U)).asUInt
      }.elsewhen(conf.hasParity.B && rx_cnt === 9.U) {
        assert(conf.evenParity.B ^ rx_data.xorR ^ uart_rxd_r(1), "Parity failed!")
      }
    }
  }.otherwise{ rx_data := 0.U }

  // Output signal and indicate done
  val uart_done_r = RegInit(false.B)
  io.uart_data := rx_data
  io.uart_done := uart_done_r
  when(rx_cnt === conf.BIT_LEN.U) {
    uart_done_r := true.B
  }.otherwise {
    uart_done_r := false.B
  }
}

/**
 * Receive 8 bits data when uart_en gets a raising edge then
 * Transmit data through 1 bit serial signal
 * While stay busy state
 *
 * IO ports:
 *
 * input uart_en
 * input [7:0] uart_din
 * output uart_txd
 * output uart_tx_busy
 */
class UartTran(val conf: UartConfig = UartConfig())
  extends FPGAModule {
  // check violation
  require(!(conf.oddParity & conf.evenParity), "Only one parity type is allowed")

  // use customized clock and rest
  override protected def clkFPGAName: String = "sys_clk"
  override protected def rstFPGAName: String = "sys_rst_n"

  // IO ports
  val io = FlatIO(new Bundle {
    val uart_en = Input(Bool())
    val uart_din = Input(UInt(UART_BITW.W))
    val uart_txd = Output(Bool())
    val uart_tx_busy = Output(Bool())
  })

  // Detect raising of uart_rxd signal
  val en_flag = EdgeDetector(io.uart_en)
  val tx_flag = RegInit(false.B)
  val tx_cnt = RegInit(0.U(4.W)) // maximum to 9
  val clk_cnt = RegInit(0.U(conf.CNT_WID.W)) // depends on baud rate
  val tx_data = RegInit(0.U(UART_BITW.W)) // cache transmit data

  // rx_flag indicates the time of transmitting data
  when(en_flag) {
    tx_flag := true.B
    tx_data := io.uart_din
  }
  .elsewhen(tx_cnt === conf.BIT_LEN.U && clk_cnt === (conf.BPS_CNT/2).U) {
    tx_flag := false.B
    tx_data := 0.U
  }

  // Record cycles to count to next bit
  when(tx_flag) {
    clk_cnt := Mux(clk_cnt < (conf.BPS_CNT-1).U, clk_cnt + 1.U, 0.U)
  }.otherwise { clk_cnt := 0.U }

  // Count the bit number of TX data
  when(tx_flag) {
    when(clk_cnt === (conf.BPS_CNT-1).U) { tx_cnt := tx_cnt + 1.U }
  }.otherwise { tx_cnt := 0.U }

  // Store RX data
  when(tx_flag) {
    when(tx_cnt === 0.U) {
      io.uart_txd := 0.U // start bit
    }.elsewhen(tx_cnt <= 8.U) {
      io.uart_txd := (tx_data >> (tx_cnt - 1.U))(0) // data
    }.elsewhen(conf.hasParity.B && tx_cnt === 9.U) {
      io.uart_txd := tx_data.xorR ^ conf.oddParity.B // parity bit
    }.otherwise {
      io.uart_txd := 1.U // 1 or 2 stop bit
    }
  }.otherwise{ io.uart_txd := 1.U }

  io.uart_tx_busy := tx_flag
}