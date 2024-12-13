package app.halftone.errdiff

import chisel3._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chisel3.util.MuxLookup

/** Pure Combinatorial Logic Circuit
  * @param errorWidth
  */
class ELUT(val errorWidth: Int) extends RawModule {
  val io = IO(new Bundle {
    val err = Input(SInt(errorWidth.W))
    val out = Output(Vec(4, SInt(errorWidth.W)))
  })

  private def errorVecLit[T <: SInt](d0: T, d1: T, d2: T, d3: T): Vec[SInt] = {
    Vec(4, SInt(errorWidth.W)).Lit(
      0 -> d0,
      1 -> d1,
      2 -> d2,
      3 -> d3
    )
  }

  /* TODO
   * compute the errors(iterate) to build LUT Seq
   * rather than hardcoding
   */
  io.out := MuxLookup(io.err.asUInt, errorVecLit(0.S, 0.S, 0.S, 0.S))(
    Seq(
      -127.S.asUInt -> errorVecLit(-56.S, -8.S, -40.S, -24.S),
      -126.S.asUInt -> errorVecLit(-55.S, -8.S, -39.S, -24.S),
      -125.S.asUInt -> errorVecLit(-55.S, -8.S, -39.S, -23.S),
      -124.S.asUInt -> errorVecLit(-54.S, -8.S, -39.S, -23.S),
      -123.S.asUInt -> errorVecLit(-54.S, -8.S, -38.S, -23.S),
      -122.S.asUInt -> errorVecLit(-53.S, -8.S, -38.S, -23.S),
      -121.S.asUInt -> errorVecLit(-53.S, -8.S, -38.S, -23.S),
      -120.S.asUInt -> errorVecLit(-52.S, -8.S, -38.S, -22.S),
      -119.S.asUInt -> errorVecLit(-52.S, -7.S, -37.S, -22.S),
      -118.S.asUInt -> errorVecLit(-52.S, -7.S, -37.S, -22.S),
      -117.S.asUInt -> errorVecLit(-51.S, -7.S, -37.S, -22.S),
      -116.S.asUInt -> errorVecLit(-51.S, -7.S, -36.S, -22.S),
      -115.S.asUInt -> errorVecLit(-50.S, -7.S, -36.S, -22.S),
      -114.S.asUInt -> errorVecLit(-50.S, -7.S, -36.S, -21.S),
      -113.S.asUInt -> errorVecLit(-49.S, -7.S, -35.S, -21.S),
      -112.S.asUInt -> errorVecLit(-49.S, -7.S, -35.S, -21.S),
      -111.S.asUInt -> errorVecLit(-49.S, -7.S, -35.S, -21.S),
      -110.S.asUInt -> errorVecLit(-48.S, -7.S, -34.S, -21.S),
      -109.S.asUInt -> errorVecLit(-48.S, -7.S, -34.S, -20.S),
      -108.S.asUInt -> errorVecLit(-47.S, -7.S, -34.S, -20.S),
      -107.S.asUInt -> errorVecLit(-47.S, -7.S, -33.S, -20.S),
      -106.S.asUInt -> errorVecLit(-46.S, -7.S, -33.S, -20.S),
      -105.S.asUInt -> errorVecLit(-46.S, -7.S, -33.S, -20.S),
      -104.S.asUInt -> errorVecLit(-46.S, -6.S, -32.S, -20.S),
      -103.S.asUInt -> errorVecLit(-45.S, -6.S, -32.S, -19.S),
      -102.S.asUInt -> errorVecLit(-45.S, -6.S, -32.S, -19.S),
      -101.S.asUInt -> errorVecLit(-44.S, -6.S, -32.S, -19.S),
      -100.S.asUInt -> errorVecLit(-44.S, -6.S, -31.S, -19.S),
      -99.S.asUInt  -> errorVecLit(-43.S, -6.S, -31.S, -19.S),
      -98.S.asUInt  -> errorVecLit(-43.S, -6.S, -31.S, -18.S),
      -97.S.asUInt  -> errorVecLit(-42.S, -6.S, -30.S, -18.S),
      -96.S.asUInt  -> errorVecLit(-42.S, -6.S, -30.S, -18.S),
      -95.S.asUInt  -> errorVecLit(-42.S, -6.S, -30.S, -18.S),
      -94.S.asUInt  -> errorVecLit(-41.S, -6.S, -29.S, -18.S),
      -93.S.asUInt  -> errorVecLit(-41.S, -6.S, -29.S, -17.S),
      -92.S.asUInt  -> errorVecLit(-40.S, -6.S, -29.S, -17.S),
      -91.S.asUInt  -> errorVecLit(-40.S, -6.S, -28.S, -17.S),
      -90.S.asUInt  -> errorVecLit(-39.S, -6.S, -28.S, -17.S),
      -89.S.asUInt  -> errorVecLit(-39.S, -6.S, -28.S, -17.S),
      -88.S.asUInt  -> errorVecLit(-38.S, -6.S, -28.S, -16.S),
      -87.S.asUInt  -> errorVecLit(-38.S, -5.S, -27.S, -16.S),
      -86.S.asUInt  -> errorVecLit(-38.S, -5.S, -27.S, -16.S),
      -85.S.asUInt  -> errorVecLit(-37.S, -5.S, -27.S, -16.S),
      -84.S.asUInt  -> errorVecLit(-37.S, -5.S, -26.S, -16.S),
      -83.S.asUInt  -> errorVecLit(-36.S, -5.S, -26.S, -16.S),
      -82.S.asUInt  -> errorVecLit(-36.S, -5.S, -26.S, -15.S),
      -81.S.asUInt  -> errorVecLit(-35.S, -5.S, -25.S, -15.S),
      -80.S.asUInt  -> errorVecLit(-35.S, -5.S, -25.S, -15.S),
      -79.S.asUInt  -> errorVecLit(-35.S, -5.S, -25.S, -15.S),
      -78.S.asUInt  -> errorVecLit(-34.S, -5.S, -24.S, -15.S),
      -77.S.asUInt  -> errorVecLit(-34.S, -5.S, -24.S, -14.S),
      -76.S.asUInt  -> errorVecLit(-33.S, -5.S, -24.S, -14.S),
      -75.S.asUInt  -> errorVecLit(-33.S, -5.S, -23.S, -14.S),
      -74.S.asUInt  -> errorVecLit(-32.S, -5.S, -23.S, -14.S),
      -73.S.asUInt  -> errorVecLit(-32.S, -5.S, -23.S, -14.S),
      -72.S.asUInt  -> errorVecLit(-32.S, -4.S, -22.S, -14.S),
      -71.S.asUInt  -> errorVecLit(-31.S, -4.S, -22.S, -13.S),
      -70.S.asUInt  -> errorVecLit(-31.S, -4.S, -22.S, -13.S),
      -69.S.asUInt  -> errorVecLit(-30.S, -4.S, -22.S, -13.S),
      -68.S.asUInt  -> errorVecLit(-30.S, -4.S, -21.S, -13.S),
      -67.S.asUInt  -> errorVecLit(-29.S, -4.S, -21.S, -13.S),
      -66.S.asUInt  -> errorVecLit(-29.S, -4.S, -21.S, -12.S),
      -65.S.asUInt  -> errorVecLit(-28.S, -4.S, -20.S, -12.S),
      -64.S.asUInt  -> errorVecLit(-28.S, -4.S, -20.S, -12.S),
      -63.S.asUInt  -> errorVecLit(-28.S, -4.S, -20.S, -12.S),
      -62.S.asUInt  -> errorVecLit(-27.S, -4.S, -19.S, -12.S),
      -61.S.asUInt  -> errorVecLit(-27.S, -4.S, -19.S, -11.S),
      -60.S.asUInt  -> errorVecLit(-26.S, -4.S, -19.S, -11.S),
      -59.S.asUInt  -> errorVecLit(-26.S, -4.S, -18.S, -11.S),
      -58.S.asUInt  -> errorVecLit(-25.S, -4.S, -18.S, -11.S),
      -57.S.asUInt  -> errorVecLit(-25.S, -4.S, -18.S, -11.S),
      -56.S.asUInt  -> errorVecLit(-24.S, -4.S, -18.S, -10.S),
      -55.S.asUInt  -> errorVecLit(-24.S, -3.S, -17.S, -10.S),
      -54.S.asUInt  -> errorVecLit(-24.S, -3.S, -17.S, -10.S),
      -53.S.asUInt  -> errorVecLit(-23.S, -3.S, -17.S, -10.S),
      -52.S.asUInt  -> errorVecLit(-23.S, -3.S, -16.S, -10.S),
      -51.S.asUInt  -> errorVecLit(-22.S, -3.S, -16.S, -10.S),
      -50.S.asUInt  -> errorVecLit(-22.S, -3.S, -16.S, -9.S),
      -49.S.asUInt  -> errorVecLit(-21.S, -3.S, -15.S, -9.S),
      -48.S.asUInt  -> errorVecLit(-21.S, -3.S, -15.S, -9.S),
      -47.S.asUInt  -> errorVecLit(-21.S, -3.S, -15.S, -9.S),
      -46.S.asUInt  -> errorVecLit(-20.S, -3.S, -14.S, -9.S),
      -45.S.asUInt  -> errorVecLit(-20.S, -3.S, -14.S, -8.S),
      -44.S.asUInt  -> errorVecLit(-19.S, -3.S, -14.S, -8.S),
      -43.S.asUInt  -> errorVecLit(-19.S, -3.S, -13.S, -8.S),
      -42.S.asUInt  -> errorVecLit(-18.S, -3.S, -13.S, -8.S),
      -41.S.asUInt  -> errorVecLit(-18.S, -3.S, -13.S, -8.S),
      -40.S.asUInt  -> errorVecLit(-18.S, -2.S, -12.S, -8.S),
      -39.S.asUInt  -> errorVecLit(-17.S, -2.S, -12.S, -7.S),
      -38.S.asUInt  -> errorVecLit(-17.S, -2.S, -12.S, -7.S),
      -37.S.asUInt  -> errorVecLit(-16.S, -2.S, -12.S, -7.S),
      -36.S.asUInt  -> errorVecLit(-16.S, -2.S, -11.S, -7.S),
      -35.S.asUInt  -> errorVecLit(-15.S, -2.S, -11.S, -7.S),
      -34.S.asUInt  -> errorVecLit(-15.S, -2.S, -11.S, -6.S),
      -33.S.asUInt  -> errorVecLit(-14.S, -2.S, -10.S, -6.S),
      -32.S.asUInt  -> errorVecLit(-14.S, -2.S, -10.S, -6.S),
      -31.S.asUInt  -> errorVecLit(-14.S, -2.S, -10.S, -6.S),
      -30.S.asUInt  -> errorVecLit(-13.S, -2.S, -9.S, -6.S),
      -29.S.asUInt  -> errorVecLit(-13.S, -2.S, -9.S, -5.S),
      -28.S.asUInt  -> errorVecLit(-12.S, -2.S, -9.S, -5.S),
      -27.S.asUInt  -> errorVecLit(-12.S, -2.S, -8.S, -5.S),
      -26.S.asUInt  -> errorVecLit(-11.S, -2.S, -8.S, -5.S),
      -25.S.asUInt  -> errorVecLit(-11.S, -2.S, -8.S, -5.S),
      -24.S.asUInt  -> errorVecLit(-10.S, -2.S, -8.S, -4.S),
      -23.S.asUInt  -> errorVecLit(-10.S, -1.S, -7.S, -4.S),
      -22.S.asUInt  -> errorVecLit(-10.S, -1.S, -7.S, -4.S),
      -21.S.asUInt  -> errorVecLit(-9.S, -1.S, -7.S, -4.S),
      -20.S.asUInt  -> errorVecLit(-9.S, -1.S, -6.S, -4.S),
      -19.S.asUInt  -> errorVecLit(-8.S, -1.S, -6.S, -4.S),
      -18.S.asUInt  -> errorVecLit(-8.S, -1.S, -6.S, -3.S),
      -17.S.asUInt  -> errorVecLit(-7.S, -1.S, -5.S, -3.S),
      -16.S.asUInt  -> errorVecLit(-7.S, -1.S, -5.S, -3.S),
      -15.S.asUInt  -> errorVecLit(-7.S, -1.S, -5.S, -3.S),
      -14.S.asUInt  -> errorVecLit(-6.S, -1.S, -4.S, -3.S),
      -13.S.asUInt  -> errorVecLit(-6.S, -1.S, -4.S, -2.S),
      -12.S.asUInt  -> errorVecLit(-5.S, -1.S, -4.S, -2.S),
      -11.S.asUInt  -> errorVecLit(-5.S, -1.S, -3.S, -2.S),
      -10.S.asUInt  -> errorVecLit(-4.S, -1.S, -3.S, -2.S),
      -9.S.asUInt   -> errorVecLit(-4.S, -1.S, -3.S, -2.S),
      -8.S.asUInt   -> errorVecLit(-4.S, 0.S, -2.S, -2.S),
      -7.S.asUInt   -> errorVecLit(-3.S, 0.S, -2.S, -1.S),
      -6.S.asUInt   -> errorVecLit(-3.S, 0.S, -2.S, -1.S),
      -5.S.asUInt   -> errorVecLit(-2.S, 0.S, -2.S, -1.S),
      -4.S.asUInt   -> errorVecLit(-2.S, 0.S, -1.S, -1.S),
      -3.S.asUInt   -> errorVecLit(-1.S, 0.S, -1.S, -1.S),
      -2.S.asUInt   -> errorVecLit(-1.S, 0.S, -1.S, 0.S),
      -1.S.asUInt   -> errorVecLit(0.S, 0.S, 0.S, 0.S),
      0.S.asUInt    -> errorVecLit(0.S, 0.S, 0.S, 0.S),
      1.S.asUInt    -> errorVecLit(0.S, 0.S, 0.S, 0.S),
      2.S.asUInt    -> errorVecLit(1.S, 0.S, 1.S, 0.S),
      3.S.asUInt    -> errorVecLit(1.S, 0.S, 1.S, 1.S),
      4.S.asUInt    -> errorVecLit(2.S, 0.S, 1.S, 1.S),
      5.S.asUInt    -> errorVecLit(2.S, 0.S, 2.S, 1.S),
      6.S.asUInt    -> errorVecLit(3.S, 0.S, 2.S, 1.S),
      7.S.asUInt    -> errorVecLit(3.S, 0.S, 2.S, 1.S),
      8.S.asUInt    -> errorVecLit(4.S, 0.S, 2.S, 2.S),
      9.S.asUInt    -> errorVecLit(4.S, 1.S, 3.S, 2.S),
      10.S.asUInt   -> errorVecLit(4.S, 1.S, 3.S, 2.S),
      11.S.asUInt   -> errorVecLit(5.S, 1.S, 3.S, 2.S),
      12.S.asUInt   -> errorVecLit(5.S, 1.S, 4.S, 2.S),
      13.S.asUInt   -> errorVecLit(6.S, 1.S, 4.S, 2.S),
      14.S.asUInt   -> errorVecLit(6.S, 1.S, 4.S, 3.S),
      15.S.asUInt   -> errorVecLit(7.S, 1.S, 5.S, 3.S),
      16.S.asUInt   -> errorVecLit(7.S, 1.S, 5.S, 3.S),
      17.S.asUInt   -> errorVecLit(7.S, 1.S, 5.S, 3.S),
      18.S.asUInt   -> errorVecLit(8.S, 1.S, 6.S, 3.S),
      19.S.asUInt   -> errorVecLit(8.S, 1.S, 6.S, 4.S),
      20.S.asUInt   -> errorVecLit(9.S, 1.S, 6.S, 4.S),
      21.S.asUInt   -> errorVecLit(9.S, 1.S, 7.S, 4.S),
      22.S.asUInt   -> errorVecLit(10.S, 1.S, 7.S, 4.S),
      23.S.asUInt   -> errorVecLit(10.S, 1.S, 7.S, 4.S),
      24.S.asUInt   -> errorVecLit(10.S, 2.S, 8.S, 4.S),
      25.S.asUInt   -> errorVecLit(11.S, 2.S, 8.S, 5.S),
      26.S.asUInt   -> errorVecLit(11.S, 2.S, 8.S, 5.S),
      27.S.asUInt   -> errorVecLit(12.S, 2.S, 8.S, 5.S),
      28.S.asUInt   -> errorVecLit(12.S, 2.S, 9.S, 5.S),
      29.S.asUInt   -> errorVecLit(13.S, 2.S, 9.S, 5.S),
      30.S.asUInt   -> errorVecLit(13.S, 2.S, 9.S, 6.S),
      31.S.asUInt   -> errorVecLit(14.S, 2.S, 10.S, 6.S),
      32.S.asUInt   -> errorVecLit(14.S, 2.S, 10.S, 6.S),
      33.S.asUInt   -> errorVecLit(14.S, 2.S, 10.S, 6.S),
      34.S.asUInt   -> errorVecLit(15.S, 2.S, 11.S, 6.S),
      35.S.asUInt   -> errorVecLit(15.S, 2.S, 11.S, 7.S),
      36.S.asUInt   -> errorVecLit(16.S, 2.S, 11.S, 7.S),
      37.S.asUInt   -> errorVecLit(16.S, 2.S, 12.S, 7.S),
      38.S.asUInt   -> errorVecLit(17.S, 2.S, 12.S, 7.S),
      39.S.asUInt   -> errorVecLit(17.S, 2.S, 12.S, 7.S),
      40.S.asUInt   -> errorVecLit(18.S, 2.S, 12.S, 8.S),
      41.S.asUInt   -> errorVecLit(18.S, 3.S, 13.S, 8.S),
      42.S.asUInt   -> errorVecLit(18.S, 3.S, 13.S, 8.S),
      43.S.asUInt   -> errorVecLit(19.S, 3.S, 13.S, 8.S),
      44.S.asUInt   -> errorVecLit(19.S, 3.S, 14.S, 8.S),
      45.S.asUInt   -> errorVecLit(20.S, 3.S, 14.S, 8.S),
      46.S.asUInt   -> errorVecLit(20.S, 3.S, 14.S, 9.S),
      47.S.asUInt   -> errorVecLit(21.S, 3.S, 15.S, 9.S),
      48.S.asUInt   -> errorVecLit(21.S, 3.S, 15.S, 9.S),
      49.S.asUInt   -> errorVecLit(21.S, 3.S, 15.S, 9.S),
      50.S.asUInt   -> errorVecLit(22.S, 3.S, 16.S, 9.S),
      51.S.asUInt   -> errorVecLit(22.S, 3.S, 16.S, 10.S),
      52.S.asUInt   -> errorVecLit(23.S, 3.S, 16.S, 10.S),
      53.S.asUInt   -> errorVecLit(23.S, 3.S, 17.S, 10.S),
      54.S.asUInt   -> errorVecLit(24.S, 3.S, 17.S, 10.S),
      55.S.asUInt   -> errorVecLit(24.S, 3.S, 17.S, 10.S),
      56.S.asUInt   -> errorVecLit(24.S, 4.S, 18.S, 10.S),
      57.S.asUInt   -> errorVecLit(25.S, 4.S, 18.S, 11.S),
      58.S.asUInt   -> errorVecLit(25.S, 4.S, 18.S, 11.S),
      59.S.asUInt   -> errorVecLit(26.S, 4.S, 18.S, 11.S),
      60.S.asUInt   -> errorVecLit(26.S, 4.S, 19.S, 11.S),
      61.S.asUInt   -> errorVecLit(27.S, 4.S, 19.S, 11.S),
      62.S.asUInt   -> errorVecLit(27.S, 4.S, 19.S, 12.S),
      63.S.asUInt   -> errorVecLit(28.S, 4.S, 20.S, 12.S),
      64.S.asUInt   -> errorVecLit(28.S, 4.S, 20.S, 12.S),
      65.S.asUInt   -> errorVecLit(28.S, 4.S, 20.S, 12.S),
      66.S.asUInt   -> errorVecLit(29.S, 4.S, 21.S, 12.S),
      67.S.asUInt   -> errorVecLit(29.S, 4.S, 21.S, 13.S),
      68.S.asUInt   -> errorVecLit(30.S, 4.S, 21.S, 13.S),
      69.S.asUInt   -> errorVecLit(30.S, 4.S, 22.S, 13.S),
      70.S.asUInt   -> errorVecLit(31.S, 4.S, 22.S, 13.S),
      71.S.asUInt   -> errorVecLit(31.S, 4.S, 22.S, 13.S),
      72.S.asUInt   -> errorVecLit(32.S, 4.S, 22.S, 14.S),
      73.S.asUInt   -> errorVecLit(32.S, 5.S, 23.S, 14.S),
      74.S.asUInt   -> errorVecLit(32.S, 5.S, 23.S, 14.S),
      75.S.asUInt   -> errorVecLit(33.S, 5.S, 23.S, 14.S),
      76.S.asUInt   -> errorVecLit(33.S, 5.S, 24.S, 14.S),
      77.S.asUInt   -> errorVecLit(34.S, 5.S, 24.S, 14.S),
      78.S.asUInt   -> errorVecLit(34.S, 5.S, 24.S, 15.S),
      79.S.asUInt   -> errorVecLit(35.S, 5.S, 25.S, 15.S),
      80.S.asUInt   -> errorVecLit(35.S, 5.S, 25.S, 15.S),
      81.S.asUInt   -> errorVecLit(35.S, 5.S, 25.S, 15.S),
      82.S.asUInt   -> errorVecLit(36.S, 5.S, 26.S, 15.S),
      83.S.asUInt   -> errorVecLit(36.S, 5.S, 26.S, 16.S),
      84.S.asUInt   -> errorVecLit(37.S, 5.S, 26.S, 16.S),
      85.S.asUInt   -> errorVecLit(37.S, 5.S, 27.S, 16.S),
      86.S.asUInt   -> errorVecLit(38.S, 5.S, 27.S, 16.S),
      87.S.asUInt   -> errorVecLit(38.S, 5.S, 27.S, 16.S),
      88.S.asUInt   -> errorVecLit(38.S, 6.S, 28.S, 16.S),
      89.S.asUInt   -> errorVecLit(39.S, 6.S, 28.S, 17.S),
      90.S.asUInt   -> errorVecLit(39.S, 6.S, 28.S, 17.S),
      91.S.asUInt   -> errorVecLit(40.S, 6.S, 28.S, 17.S),
      92.S.asUInt   -> errorVecLit(40.S, 6.S, 29.S, 17.S),
      93.S.asUInt   -> errorVecLit(41.S, 6.S, 29.S, 17.S),
      94.S.asUInt   -> errorVecLit(41.S, 6.S, 29.S, 18.S),
      95.S.asUInt   -> errorVecLit(42.S, 6.S, 30.S, 18.S),
      96.S.asUInt   -> errorVecLit(42.S, 6.S, 30.S, 18.S),
      97.S.asUInt   -> errorVecLit(42.S, 6.S, 30.S, 18.S),
      98.S.asUInt   -> errorVecLit(43.S, 6.S, 31.S, 18.S),
      99.S.asUInt   -> errorVecLit(43.S, 6.S, 31.S, 19.S),
      100.S.asUInt  -> errorVecLit(44.S, 6.S, 31.S, 19.S),
      101.S.asUInt  -> errorVecLit(44.S, 6.S, 32.S, 19.S),
      102.S.asUInt  -> errorVecLit(45.S, 6.S, 32.S, 19.S),
      103.S.asUInt  -> errorVecLit(45.S, 6.S, 32.S, 19.S),
      104.S.asUInt  -> errorVecLit(46.S, 6.S, 32.S, 20.S),
      105.S.asUInt  -> errorVecLit(46.S, 7.S, 33.S, 20.S),
      106.S.asUInt  -> errorVecLit(46.S, 7.S, 33.S, 20.S),
      107.S.asUInt  -> errorVecLit(47.S, 7.S, 33.S, 20.S),
      108.S.asUInt  -> errorVecLit(47.S, 7.S, 34.S, 20.S),
      109.S.asUInt  -> errorVecLit(48.S, 7.S, 34.S, 20.S),
      110.S.asUInt  -> errorVecLit(48.S, 7.S, 34.S, 21.S),
      111.S.asUInt  -> errorVecLit(49.S, 7.S, 35.S, 21.S),
      112.S.asUInt  -> errorVecLit(49.S, 7.S, 35.S, 21.S),
      113.S.asUInt  -> errorVecLit(49.S, 7.S, 35.S, 21.S),
      114.S.asUInt  -> errorVecLit(50.S, 7.S, 36.S, 21.S),
      115.S.asUInt  -> errorVecLit(50.S, 7.S, 36.S, 22.S),
      116.S.asUInt  -> errorVecLit(51.S, 7.S, 36.S, 22.S),
      117.S.asUInt  -> errorVecLit(51.S, 7.S, 37.S, 22.S),
      118.S.asUInt  -> errorVecLit(52.S, 7.S, 37.S, 22.S),
      119.S.asUInt  -> errorVecLit(52.S, 7.S, 37.S, 22.S),
      120.S.asUInt  -> errorVecLit(52.S, 8.S, 38.S, 22.S),
      121.S.asUInt  -> errorVecLit(53.S, 8.S, 38.S, 23.S),
      122.S.asUInt  -> errorVecLit(53.S, 8.S, 38.S, 23.S),
      123.S.asUInt  -> errorVecLit(54.S, 8.S, 38.S, 23.S),
      124.S.asUInt  -> errorVecLit(54.S, 8.S, 39.S, 23.S),
      125.S.asUInt  -> errorVecLit(55.S, 8.S, 39.S, 23.S),
      126.S.asUInt  -> errorVecLit(55.S, 8.S, 39.S, 24.S),
      127.S.asUInt  -> errorVecLit(56.S, 8.S, 40.S, 24.S)
    )
  )
}
