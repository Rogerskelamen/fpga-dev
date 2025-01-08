#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VErrDiffCore.h"
#include "VErrDiffCore___024root.h"
#include "debug.h"
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <math.h>

#define IMG_H 512
#define IMG_W 512
#define THRES 128

#define AssertExit(cond, format, ...) \
  do { \
    if (!(cond)) { \
      fprintf(stderr, ANSI_FMT(format, ANSI_FG_RED) "\n", ## __VA_ARGS__); \
      exit_sim(); \
      assert(cond); \
    } \
  } while (0)

// global variables
static int errCache[IMG_H][IMG_W] = {0};
static uint8_t img[IMG_H][IMG_W];

VerilatedContext *contextp = NULL;
VerilatedVcdC *m_trace = NULL;
VErrDiffCore *top = NULL;

static uint32_t
random_gen(uint32_t max) {
  srand(rand());
  return rand() % max;
}

/*
 * the default `round` function in <math.h> uses
 * "round half away from zero",
 * but here needs "round half up"(substitution)
 */
static double
round_half_up(double x) {
  return floor(x+0.5);
}

static int
check_bound_then_acc(int i, int j, int val = 0) {
  if (0 <= i && i < IMG_H && 0 <= j && j < IMG_W) {
    errCache[i][j] += val;
    return errCache[i][j];
  }
  return 0;
}

void init_sim() {
  contextp = new VerilatedContext;
  m_trace = new VerilatedVcdC;
  top = new VErrDiffCore;
  Verilated::traceEverOn(true);
  top->trace(m_trace, 0);
  m_trace->open("waveform.vcd");
}

void step_and_dump_wave(int n = 1) {
  for (int i = 0; i < n; i++) {
    top->clock ^= 1;
    top->eval();
    contextp->timeInc(1);
    m_trace->dump(contextp->time());

    top->clock ^= 1;
    top->eval();
    contextp->timeInc(1);
    m_trace->dump(contextp->time());
  }
}

void exit_sim() {
  m_trace->close();
  delete top;
}

int main() {
  init_sim();

  // generate random seed
  unsigned int seed = time(0);
  srand(seed);
  // generate gray-image using random generator
  for (int pos = 0; pos < IMG_H*IMG_W; pos++)
    img[pos/IMG_H][pos%IMG_W] = random_gen(0x100);

  // init reset
  top->reset = 1; top->in_valid = 0;
  step_and_dump_wave(5);
  top->reset = 0;

  // trigger hardware
  top->in_valid = 1; step_and_dump_wave();
  top->in_valid = 0;

  for (int i = 0; i < IMG_H; i++) {
    for (int j = 0; j < IMG_W; j++) {
      uint8_t pix = img[i][j];
      int err = 0;

      // 1. compare with threshold
      img[i][j] = pix + errCache[i][j] < THRES ? 0 : 0xff;
      // 2. calculate error
      err = pix + errCache[i][j] - img[i][j];
      int errdiff[4];
      errdiff[0] = (int)round_half_up(err * 7.0/16);
      errdiff[1] = (int)round_half_up(err * 1.0/16);
      errdiff[2] = (int)round_half_up(err * 5.0/16);
      errdiff[3] = (int)round_half_up(err * 3.0/16);

      // Execute simulation
      top->cache_pb_dout = errCache[i][j];
      top->img_pb_dout = pix;

      while(!top->rootp->ErrDiffCore__DOT__threshCalc__DOT__resultValid)
        step_and_dump_wave(); // enter ErrorOut
      step_and_dump_wave(); // get bval
      uint8_t bval = top->rootp->ErrDiffCore__DOT__errorOut__DOT__bval * 0xff;
      AssertExit(bval == img[i][j], "bval[%#x] != result[%#x], at row[%d] column[%d]", bval, img[i][j], i, j);

      int errOut[4];
      errOut[0] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_0;
      errOut[1] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_1;
      errOut[2] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_2;
      errOut[3] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_3;
      for (int k = 0; k < 4; k++)
        AssertExit(errOut[k] == errdiff[k], "errOut%d[%d] != result[%d], at row[%d] column[%d], err = %d", k, errOut[k], errdiff[k], i, j, err);

      top->cache_pa_dout = check_bound_then_acc(i, j+1); step_and_dump_wave(); // right
      top->cache_pa_dout = check_bound_then_acc(i+1, j); step_and_dump_wave(); // below
      top->cache_pa_dout = check_bound_then_acc(i+1, j-1); step_and_dump_wave(); // belowLeft
      check_bound_then_acc(i, j+1, errdiff[0]);
      check_bound_then_acc(i+1, j+1, errdiff[1]);
      check_bound_then_acc(i+1, j, errdiff[2]);
      check_bound_then_acc(i+1, j-1, errdiff[3]);
      step_and_dump_wave(4);

      // WriteBinary to PixelGet
      step_and_dump_wave(3);
    }
  }

  OK("Differential tests all passed, safe exit");
  exit_sim();
}
