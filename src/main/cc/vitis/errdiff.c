/******************************************************************************
*
* Copyright (C) 2009 - 2014 Xilinx, Inc.  All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* Use of the Software is limited solely to applications:
* (a) running on a Xilinx device, or
* (b) that interact with a Xilinx device through a bus or interconnect.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* XILINX  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
* OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*
* Except as contained in this notice, the name of the Xilinx shall not be used
* in advertising or otherwise to promote the sale, use or other dealings in
* this Software without prior written authorization from Xilinx.
*
******************************************************************************/

/*
 * helloworld.c: simple test application
 *
 * This application configures UART 16550 to baud rate 9600.
 * PS7 UART (Zynq) is not initialized by this application, since
 * bootrom/bsp configures it to baud rate 115200
 *
 * ------------------------------------------------
 * | UART TYPE   BAUD RATE                        |
 * ------------------------------------------------
 *   uartns550   9600
 *   uartlite    Configurable only in HW design
 *   ps7_uart    115200 (configured by bootrom/bsp)
 */

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "xil_cache.h"
#include "xil_io.h"

#define DDRBASE 0x38000000
#define IMG_H 512
#define IMG_W 512
#define THRES 128

static int errCache[IMG_H][IMG_W] = {0};
static u8 img[IMG_H][IMG_W];

static u32
random_gen(u32 max) {
    srand(rand());
    return rand() % max;
}

static double
round_half_up(double x) {
    return floor(x+0.5);
}

static void
valid_then_acc(int i, int j, int val) {
    if (0 <= i && i < IMG_H && 0 <= j && j < IMG_W) {
        errCache[i][j] += val;
    }
}

int main()
{
    // set random seed
    unsigned int seed = 42;
    srand(seed);

    Xil_DCacheDisable(); // Disable DCache

    char c, a;
    int data;

    print("AXI4 PL DDR TEST!\n\r");

    print("Writing to DDR...\n\r");

    // generate random numbers to write ddr
    for (int pos = 0; pos < IMG_H*IMG_W; pos++) {
        u8 tmp = random_gen(0x100);
        img[pos/IMG_H][pos%IMG_W] = tmp;
        Xil_Out8(DDRBASE+pos, tmp);
    }

    // calculate by ARM Core
    for (int i = 0; i < IMG_H; i++) {
        for (int j = 0; j < IMG_W; j++) {
            u8 pix = img[i][j];
            int err = 0;
            // 1. compare with threshold
            img[i][j] = pix + errCache[i][j] < THRES ? 0 : 0xff;
            // 2. calculate error
            err = pix + errCache[i][j] - img[i][j];

            // 3. diffuse the error
            valid_then_acc(i, j+1, (int)round_half_up(err * 7.0/16));
            valid_then_acc(i+1, j-1, (int)round_half_up(err * 3.0/16));
            valid_then_acc(i+1, j, (int)round_half_up(err * 5.0/16));
            valid_then_acc(i+1, j+1, (int)round_half_up(err * 1.0/16));
        }
    }
    print("Calculation finished, please trigger hardware\n\r");

    // wait to trigger hardware
    getchar();

    // validate result with software
    for (int i = 0; i < IMG_H; i++) {
        for (int j = 0; j < IMG_W; j++) {
            u8 res = Xil_In8(DDRBASE + i*IMG_W + j);
            if (res != img[i][j]) {
                printf("error: the result[%#x] != %#x at row[%d] column[%d]\n\r", res, img[i][j], i, j);
                assert(0);
            }
        }
    }
    print("All test passed\n\r");

    while(1) {
        scanf("%c", &c);
        scanf("%c", &a);
        if(c == 'r') {
            // int data = Xil_In32(0x38000000);
            switch(a) {
                case '1':
                    data = Xil_In32(0x38000000);
                    break;
                case '2':
                    data = Xil_In32(0x38000004);
                    break;
                case '3':
                    data = Xil_In32(0x38000008);
                    break;
                default:
                    data = Xil_In32(0x38000000);
                    break;
            }
            printf("%#x\n\r", data);
        }
    }

    return 0;
}
