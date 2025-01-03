#include "utils.h"

#define Log(format, ...) \
    printf(ANSI_FMT("[%s %d] " format, ANSI_FG_BLUE) "\n", \
        __func__, __LINE__, ## __VA_ARGS__)

#define OK(format, ...) \
    printf(ANSI_FMT(format, ANSI_FG_GREEN) "\n", ## __VA_ARGS__)

#define Assert(cond, format, ...) \
  do { \
    if (!(cond)) { \
      fprintf(stderr, ANSI_FMT(format, ANSI_FG_RED) "\n", ## __VA_ARGS__); \
      assert(cond); \
    } \
  } while (0)

#define panic(format, ...) Assert(0, format, ## __VA_ARGS__)

#define TODO() panic("please implement me")
