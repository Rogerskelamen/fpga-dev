package tools

import chisel3._
import chisel3.util.Decoupled

/**
 * Direction is from Master to Slave
 */
object AXI4Parameters {
  // -------- Fixed parameters of signals in AXI4 specifications ---------

  /*
   real Burst length = AxLEN[7:0] + 1
   AxLEN[7:0]     burst length
      0               1
      1               2
      ...             ...
      15              16(length limit but for INCR)
      ...             ...
      255             256(INCR can reach here)
   */

  val lenBits = 8 // burst length, how many times of transfer data for one burst

  /*
   AxSIZE[2:0]     Bytes in transfer
      0b000              1
      0b001              2
      0b010              4
      0b011              8
      0b100              16
      0b101              32
      0b110              64
      0b111              128
   */
  val sizeBits = 3 // burst size, the width of data for one transfer

  /*
   00 -> FIXED: transfer all data to the same address
   01 -> INCR: transfer each data to next increment address
   10 -> WRAP: similar to INCR, but wraps around to lower address
               if upper address is limited
   11 -> Reserved
   */
  val burstBits = 2 // burst type

  /*
   arcache  awcache            memory type
    0000     0000            Device Non-bufferable
    0001     0001            Device Bufferable
    0010     0010            Normal Non-cacheable Non-bufferable
    0011     0011            Normal Non-cacheable Bufferable
    1010     0110            Write-through No-allocate
    1110     (0110) 0110     Write-through Read-allocate
    1010     1110 (1010)     Write-through Write-allocate
    1110     1110            Write-through Read and Write-allocate
    1011     0111            Write-back No-allocate
    1111     (0111) 0111     Write-back Read-allocate
    1011     1111 (1011)     Write-back Write-allocate
    1111     1111            Write-back Read and Write-allocate
   */
  val cacheBits = 4 // memory type

  /*
   [0] 0 Unprivileged access
       1 Privileged access
   [1] 0 Secure access
       1 Non-secure access
   [2] 0 Data access
       1 Instruction access
   */
  val protBits = 3 // protection type

  val qosBits = 4 // quality of service

  /*
   00 -> OKAY: access success
   01 -> EXOKAY: Exclusive access success
   10 -> SLVERR: slave return an error condition
   11 -> DECERR: decode error, indicate slave not found
   */
  val respBits = 2 // response type

  // -------- Configurable parameters of signals in AXI4 specifications ---------
  val idBits   = 1  // transaction ID, set to 1 (don't care)
  val addrBits = 32 // address bits
  val dataBits = 32 // data bits
  val userBits = 1  // user signal (user defined)

  def CACHE_RALLOCATE  = 8.U(cacheBits.W)
  def CACHE_WALLOCATE  = 4.U(cacheBits.W)
  def CACHE_MODIFIABLE = 2.U(cacheBits.W)
  def CACHE_BUFFERABLE = 1.U(cacheBits.W)

  def PROT_PRIVILEGED  = 1.U(protBits.W)
  def PROT_INSECURE    = 2.U(protBits.W)
  def PROT_INSTRUCTION = 4.U(protBits.W)

  def BURST_FIXED = 0.U(burstBits.W)
  def BURST_INCR  = 1.U(burstBits.W)
  def BURST_WRAP  = 2.U(burstBits.W)

  def RESP_OKAY   = 0.U(respBits.W)
  def RESP_EXOKAY = 1.U(respBits.W)
  def RESP_SLVERR = 2.U(respBits.W)
  def RESP_DECERR = 3.U(respBits.W)
}

trait AXI4HasUser {
  val user  = Output(UInt(AXI4Parameters.userBits.W))
}

trait AXI4HasData {
  def dataBits: Int = AXI4Parameters.dataBits
  val data  = Output(UInt(dataBits.W))
}

trait AXI4HasId {
  def idBits: Int = AXI4Parameters.idBits
  val id = Output(UInt(idBits.W))
}

trait AXI4HasLast {
  val last = Output(Bool())
}

/**
 * AXI4-lite
 */
class AXI4LiteBundleA extends Bundle {
  val addr = Output(UInt(AXI4Parameters.addrBits.W))
}

class AXI4LiteBundleW(override val dataBits: Int = AXI4Parameters.dataBits)
  extends Bundle with AXI4HasData {
  val strb = Output(UInt((dataBits/8).W))
}

class AXI4LiteBundleB extends Bundle {
  val resp = Output(UInt(AXI4Parameters.respBits.W))
}

class AXI4LiteBundleR(override val dataBits: Int = AXI4Parameters.dataBits)
  extends AXI4LiteBundleB with AXI4HasData

class AXI4Lite extends Bundle {
  val aw = Decoupled(new AXI4LiteBundleA)
  val w  = Decoupled(new AXI4LiteBundleW)
  val b  = Flipped(Decoupled(new AXI4LiteBundleB))
  val ar = Decoupled(new AXI4LiteBundleA)
  val r  = Flipped(Decoupled(new AXI4LiteBundleR))
}

/**
 * AXI4-full
 */
class AXI4BundleA(override val idBits: Int)
  extends AXI4LiteBundleA with AXI4HasId with AXI4HasUser {
  val len   = Output(UInt(AXI4Parameters.lenBits.W))  // number of beats - 1
  val size  = Output(UInt(AXI4Parameters.sizeBits.W)) // bytes in beat = 2^size
  val burst = Output(UInt(AXI4Parameters.burstBits.W))
  val lock  = Output(Bool())
  val cache = Output(UInt(AXI4Parameters.cacheBits.W))
  val qos   = Output(UInt(AXI4Parameters.qosBits.W))  // 0=no QoS, bigger = higher priority
  // val region = UInt(width = 4) // optional

  override def toPrintable: Printable = p"addr = 0x${Hexadecimal(addr)}, id = ${id}, len = ${len}, size = ${size}"
}

// id ... removed in AXI4
class AXI4BundleW(override val dataBits: Int)
  extends AXI4LiteBundleW(dataBits) with AXI4HasLast with AXI4HasUser {
  override def toPrintable: Printable = p"data = ${Hexadecimal(data)}, wmask = 0x${strb}, last = ${last}"
}
class AXI4BundleB(override val idBits: Int)
  extends AXI4LiteBundleB with AXI4HasId with AXI4HasUser {
  override def toPrintable: Printable = p"resp = ${resp}, id = ${id}"
}
class AXI4BundleR(override val dataBits: Int, override val idBits: Int)
  extends AXI4LiteBundleR(dataBits) with AXI4HasLast with AXI4HasId with AXI4HasUser {
  override def toPrintable: Printable = p"resp = ${resp}, id = ${id}, data = ${Hexadecimal(data)}, last = ${last}"
}

class AXI4(val dataBits: Int = AXI4Parameters.dataBits, val idBits: Int = AXI4Parameters.idBits) extends AXI4Lite {
  override val aw = Decoupled(new AXI4BundleA(idBits))
  override val w  = Decoupled(new AXI4BundleW(dataBits))
  override val b  = Flipped(Decoupled(new AXI4BundleB(idBits)))
  override val ar = Decoupled(new AXI4BundleA(idBits))
  override val r  = Flipped(Decoupled(new AXI4BundleR(dataBits, idBits)))
}