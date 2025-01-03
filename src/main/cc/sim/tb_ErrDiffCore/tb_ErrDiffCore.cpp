#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VErrDiffCore.h"
#include "VErrDiffCore___024root.h"
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>

#define IMG_H 16
#define IMG_W 16
#define THRES 128

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
    top->clk ^= 1;
    top->eval();
    contextp->timeInc(1);
    m_trace->dump(contextp->time());

    top->clk ^= 1;
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

  unsigned int seed = 42;
  srand(seed);

  // generate gray-image using random generator
  for (int pos = 0; pos < IMG_H*IMG_W; pos++)
    img[pos/IMG_H][pos%IMG_W] = random_gen(0x100);

  // init reset
  top->rstn = 0; top->extn_ready = 1;
  step_and_dump_wave(5);
  top->rstn = 1; step_and_dump_wave();

  // trigger hardware
  top->extn_ready = 0; step_and_dump_wave();
  top->extn_ready = 1; step_and_dump_wave();

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

      // execute simulation
      top->pb_dout = errCache[i][j]; step_and_dump_wave(2);
      top->read_resp_valid = 1; top->read_resp_data = pix << ((j%4) * 8);
      step_and_dump_wave();
      top->read_resp_valid = 0; top->read_resp_data = 0; step_and_dump_wave(2);

      step_and_dump_wave(); // enter ErrorOut
      uint8_t bval = top->rootp->ErrDiffCore__DOT__errorOut__DOT__bval * 0xff;
      if (bval != img[i][j]) {
        fprintf(stderr, "bval[%#x] != result[%#x], at row[%d] column[%d]\n", bval, img[i][j], i, j);
        exit_sim(); assert(0);
      }

      int errOut[4];
      errOut[0] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_0;
      errOut[1] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_1;
      errOut[2] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_2;
      errOut[3] = (int8_t)top->rootp->ErrDiffCore__DOT__errorOut__DOT__errOut_3;
      for (int k = 0; k < 4; k++) {
        if (errOut[k] != errdiff[k]) {
          fprintf(stderr, "errOut%d[%d] != result[%d], at row[%d] column[%d], err = %d\n", k, errOut[k], errdiff[k], i, j, err);
          exit_sim(); assert(0);
        }
      }

      top->pa_dout = check_bound_then_acc(i, j+1); step_and_dump_wave(); // right
      top->pa_dout = check_bound_then_acc(i+1, j); step_and_dump_wave(); // below
      top->pa_dout = check_bound_then_acc(i+1, j-1); step_and_dump_wave(); // belowLeft
      check_bound_then_acc(i, j+1, errdiff[0]);
      check_bound_then_acc(i+1, j+1, errdiff[1]);
      check_bound_then_acc(i+1, j, errdiff[2]);
      check_bound_then_acc(i+1, j-1, errdiff[3]);
      step_and_dump_wave(4);

      // WriteBinary
      step_and_dump_wave();
      top->write_resp_valid = 1; step_and_dump_wave();
      top->write_resp_valid = 0; step_and_dump_wave();
    }
  }

  exit_sim();
}
